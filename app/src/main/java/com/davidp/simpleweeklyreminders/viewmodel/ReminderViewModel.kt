package com.davidp.simpleweeklyreminders.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.isArchived
import com.davidp.simpleweeklyreminders.data.notification.ReminderWorker
import com.davidp.simpleweeklyreminders.data.repository.ReminderLogRepository
import com.davidp.simpleweeklyreminders.data.repository.ReminderRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Owns the app's three database observers. Room flows are cold, so every extra collector
 * is another query on the same rows — two observers of the same data re-query independently
 * and can hand the UI different snapshots (this is what made the calendar pip disagree with
 * the day-list checkbox). Everything the UI needs is derived from the flows below rather
 * than opening a new one; add a query here only for rows none of them already cover.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ReminderRepository(database.reminderDao(), database.reminderLogDao())
    private val logRepository = ReminderLogRepository(database.reminderLogDao())

    /**
     * Watcher 1 of 3: every row in the reminders table. null until the first emission
     * lands, so callers can show nothing rather than flashing an empty state.
     */
    val reminders: StateFlow<List<Reminder>?> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    /** Active (non-archived) reminders — a filter of [reminders], not a second query. */
    val allReminders: StateFlow<List<Reminder>?> = reminders
        .map { list -> list?.filterNot { it.isArchived() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    /** Archived reminders, most recently archived first — also a filter of [reminders]. */
    val archivedReminders: StateFlow<List<Reminder>?> = reminders
        .map { list -> list?.filter { it.isArchived() }?.sortedByDescending { it.endDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

    private val calendarWindow = MutableStateFlow(CalendarWindow(YearMonth.now(), LocalDate.now()))

    /**
     * Watcher 2 of 3: logs for the calendar's visible range. The month grid's pips and the
     * selected-day list both slice this single emission, so they cannot disagree about a
     * day's completed state. Set the range with [setCalendarWindow].
     */
    val calendarLogs: StateFlow<List<ReminderLog>> = calendarWindow
        .map { calendarLogRange(it.month, it.selectedDate) }
        .distinctUntilChanged()
        .flatMapLatest { range -> logRepository.getLogsForDateRange(range.start, range.end) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    private val today = MutableStateFlow(LocalDate.now())

    /**
     * Watcher 3 of 3: today's logs, for the reminder cards' per-time chips. Separate from
     * [calendarLogs] because the Reminders tab needs today no matter which month the
     * calendar is parked on; it's one day of rows, so the extra query is negligible.
     */
    val todayLogs: StateFlow<List<ReminderLog>> = today
        .flatMapLatest { date -> logRepository.getLogsForDateRange(date.atStartOfDay(), date.endOfDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

    /** Re-points [calendarLogs] as the pager moves or a different day is tapped. */
    fun setCalendarWindow(month: YearMonth, selectedDate: LocalDate) {
        calendarWindow.value = CalendarWindow(month, selectedDate)
    }

    /** Re-points [todayLogs] when the calendar date rolls over while the app is open. */
    fun setToday(date: LocalDate) {
        today.value = date
    }

    fun insert(reminder: Reminder) = viewModelScope.launch {
        repository.insert(reminder)
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder)
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        repository.delete(reminder)
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun archive(reminder: Reminder) = viewModelScope.launch {
        repository.update(
            reminder.copy(isActive = false, endDate = LocalDate.now().minusDays(1), archivedAt = LocalDateTime.now())
        )
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun restore(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder.copy(isActive = true, endDate = null, archivedAt = null))
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun updateLogCompletedStatus(logId: Int, completed: Boolean) = viewModelScope.launch {
        repository.updateLogCompletedStatus(logId, completed)
    }

    fun logAdHocCompletion(reminder: Reminder, dateTime: LocalDateTime) = viewModelScope.launch {
        repository.insertCompletedLog(reminder, dateTime)
    }

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
