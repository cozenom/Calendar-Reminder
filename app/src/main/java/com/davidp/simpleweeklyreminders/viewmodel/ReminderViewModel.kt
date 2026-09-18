package com.davidp.simpleweeklyreminders.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.OccurrenceCounts
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.archivedSince
import com.davidp.simpleweeklyreminders.data.model.isArchived
import com.davidp.simpleweeklyreminders.data.notification.NotificationActionReceiver
import com.davidp.simpleweeklyreminders.data.notification.ReminderWorker
import com.davidp.simpleweeklyreminders.data.repository.ReminderLogRepository
import com.davidp.simpleweeklyreminders.data.repository.ReminderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Owns the app's two database observers. Room flows are cold, so every extra collector
 * re-queries independently and can hand the UI a different snapshot. Derive from the flows
 * below; add a query only for rows none of them already cover.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReminderRepository(database.reminderDao(), database.reminderLogDao())
    private val logRepository = ReminderLogRepository(database.reminderLogDao())

    /**
     * Watcher 1 of 2: every row in the reminders table. null until the first emission
     * lands, so callers can show nothing rather than flashing an empty state.
     */
    val reminders: StateFlow<List<Reminder>?> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    /**
     * The app's clock, to the minute. Screens read this instead of calling `.now()` at
     * composition: a plain `.now()` isn't state, so Compose never redraws when the minute,
     * the day or the time zone changes — a screen left open over midnight kept yesterday.
     *
     * Ticks on the minute boundary while anyone collects. Collect it (and the flows derived
     * from it) with `collectAsStateWithLifecycle`, so collection stops when the app is in
     * the background and the flow restarts on resume — which re-reads the clock for free.
     * Truncated so equal minutes compare equal and downstream `remember` keys stay stable.
     */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            // One clock read per loop: the sleep is measured from the same instant that was
            // emitted, so an early wake just re-emits the same minute (deduped) and waits again
            val exact = LocalDateTime.now()
            emit(exact.truncatedTo(ChronoUnit.MINUTES))
            delay(millisUntilNextMinute(exact))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
    )

    /**
     * Active (non-archived) reminders — a filter of [reminders], not a second query.
     * Combined with [now] so a reminder that lapses at midnight moves to the Archive on
     * the next tick, not on the next unrelated DB write.
     */
    val allReminders: StateFlow<List<Reminder>?> = combine(reminders, now) { list, now ->
        list?.filterNot { it.isArchived(now.toLocalDate()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    /** Archived reminders, most recently archived first — also a filter of [reminders]. */
    val archivedReminders: StateFlow<List<Reminder>?> = combine(reminders, now) { list, now ->
        val today = now.toLocalDate()
        // archivedSince, not endDate: a manual archive keeps the user's end date (or none)
        list?.filter { it.isArchived(today) }?.sortedByDescending { it.archivedSince(today) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    private val calendarWindow = MutableStateFlow(CalendarWindow(YearMonth.now(), LocalDate.now()))

    /**
     * Watcher 2 of 2: logs for the calendar's visible range. The month grid's pips and the
     * selected-day list both slice this single emission, so they cannot disagree about a
     * day's completed state. Set the range with [setCalendarWindow].
     */
    val calendarLogs: StateFlow<List<ReminderLog>> = calendarWindow
        .map { calendarLogRange(it.month, it.selectedDate) }
        .distinctUntilChanged()
        .flatMapLatest { range -> logRepository.getLogsForDateRange(range.start, range.end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    /** Re-points [calendarLogs] as the pager moves or a different day is tapped. */
    fun setCalendarWindow(month: YearMonth, selectedDate: LocalDate) {
        calendarWindow.value = CalendarWindow(month, selectedDate)
    }

    fun insert(reminder: Reminder) = viewModelScope.launch {
        repository.insert(reminder)
        ReminderWorker.schedule(getApplication())
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder)
        ReminderWorker.schedule(getApplication())
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        val context = getApplication<Application>()
        // Cancel what's armed and shown before the rows go: the worker's re-arm pass only
        // sees reminders that still exist, so nothing else would ever clean these up. The
        // stray chain alarm would fire and no-op, but a notification already in the tray
        // would sit there with dead actions.
        val elapsed = repository.elapsedLogs(reminder)
        ReminderWorker.cancelAlarm(context, reminder.id)
        elapsed.filter { it.snoozedUntil != null }.forEach { ReminderWorker.cancelSnoozeAlarm(context, it.id) }
        NotificationActionReceiver.cancelNotifications(context, elapsed.map { it.id })

        repository.delete(reminder)
        ReminderWorker.schedule(context)
    }

    /** Manual archive: stamp archivedAt and stop scheduling. endDate is left untouched. */
    fun archive(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder.copy(isActive = false, archivedAt = LocalDateTime.now()))
        ReminderWorker.schedule(getApplication())
    }

    /**
     * Undo of [archive] and the Archive screen's Restore. Keeps the reminder's end date by
     * default; a lapsed reminder (see hasLapsed) needs [endDate] passed as null or a date from
     * today on — restoring it with its old past date would drop it straight back in the Archive.
     */
    fun restore(reminder: Reminder, endDate: LocalDate? = reminder.endDate) = viewModelScope.launch {
        repository.update(reminder.copy(isActive = true, endDate = endDate, archivedAt = null))
        ReminderWorker.schedule(getApplication())
    }

    fun updateLogCompletedStatus(logId: Int, completed: Boolean) = viewModelScope.launch {
        repository.updateLogCompletedStatus(logId, completed)
    }

    fun logAdHocCompletion(reminder: Reminder, dateTime: LocalDateTime) = viewModelScope.launch {
        repository.insertCompletedLog(reminder, dateTime)
    }

    /** One-shot done/missed tally for an archive row; loaded per row, not observed. */
    suspend fun loadArchiveStats(reminder: Reminder): OccurrenceCounts = repository.archiveStats(reminder)

    fun updateRemindersOrder(reminders: List<Reminder>) = viewModelScope.launch {
        repository.updateRemindersOrder(reminders)
    }

    private data class CalendarWindow(val month: YearMonth, val selectedDate: LocalDate)

    private companion object {
        /**
         * Keeps the upstream queries alive briefly after the last collector leaves, so the
         * tab switch (which composes both tabs at once) and configuration changes re-read
         * the cached value instead of re-querying.
         */
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
