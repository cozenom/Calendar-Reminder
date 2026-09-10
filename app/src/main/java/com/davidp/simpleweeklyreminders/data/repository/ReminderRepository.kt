package com.davidp.simpleweeklyreminders.data.repository

import com.davidp.simpleweeklyreminders.data.dao.ReminderDao
import com.davidp.simpleweeklyreminders.data.dao.ReminderLogDao
import com.davidp.simpleweeklyreminders.data.model.OccurrenceCounts
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.countOutcomes
import com.davidp.simpleweeklyreminders.data.model.isScheduledOn
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderLogDao: ReminderLogDao
) {
    /**
     * Every row, in manual sort order. The only query on this table — active/archived/
     * covers-a-date are filters of it in ReminderViewModel.
     */
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    suspend fun insert(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): Long {
        val id = reminderDao.insertReminder(reminder)
        // Backfill the whole run from startDate: a backdated start fills past occurrences
        // (shown missed, still tappable) so the calendar and chips have real rows to draw.
        // Safe because the start date is locked once created (see ReminderForm), so update()
        // never has to re-backfill.
        generateLogsForReminder(reminder.copy(id = id.toInt()), now, includePast = true)
        return id
    }

    suspend fun update(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()) {
        reminderDao.updateReminder(reminder)
        // Keep historical log snapshots in sync with the new title
        reminderLogDao.updateTitleForReminder(reminder.id, reminder.title)

        if (!reminder.isActive) {
            // Paused: nothing gets regenerated, so keep completed-early logs —
            // the carry-over below handles them when the reminder is reactivated
            reminderLogDao.deleteFutureIncompleteLogsForReminder(reminder.id, now)
            return
        }

        // Snapshot the outgoing future schedule so completions survive regeneration
        val oldFutureLogs = reminderLogDao.getFutureLogsForReminder(reminder.id, now)
        val completedOldLogs = oldFutureLogs.filter { it.completed }

        // Delete all future logs to avoid duplicates when regenerating
        reminderLogDao.deleteFutureLogsForReminder(reminder.id, now)

        // The delete above only touches future logs, so a slot whose time passed
        // between edits would keep its card chip actionable after the schedule
        // dropped it. Clear today's incomplete leftovers the new schedule no longer
        // contains; completed ones record something the user actually did, so they stay.
        val today = now.toLocalDate()
        val staleTodayLogIds = reminderLogDao
            .getLogsForReminderInRange(reminder.id, today.atStartOfDay(), now)
            .filter {
                !it.completed &&
                    (!reminder.isScheduledOn(today) || it.logDateTime.toLocalTime() !in reminder.reminderTimes)
            }
            .map { it.id }
        if (staleTodayLogIds.isNotEmpty()) reminderLogDao.deleteLogsByIds(staleTodayLogIds)

        generateLogsForReminder(reminder, now)

        if (completedOldLogs.isEmpty()) return

        val newLogs = reminderLogDao.getFutureLogsForReminder(reminder.id, now)
        val newByDateTime = newLogs.associateBy { it.logDateTime }

        // 1) A slot that still exists at the same date+time keeps its completion
        val unmatchedCountByDate = mutableMapOf<LocalDate, Int>()
        for (old in completedOldLogs) {
            val match = newByDateTime[old.logDateTime]
            if (match != null) {
                reminderLogDao.updateCompletedStatus(match.id, true)
            } else {
                unmatchedCountByDate.merge(old.logDateTime.toLocalDate(), 1, Int::plus)
            }
        }
        if (unmatchedCountByDate.isEmpty()) return

        // 2) A completion whose time was changed carries over to a slot that is
        //    new on that date; a slot removed outright takes its completion with
        //    it rather than ticking some other unrelated time
        val oldDateTimes = oldFutureLogs.mapTo(HashSet()) { it.logDateTime }
        newLogs.filter { it.logDateTime !in oldDateTimes }
            .groupBy { it.logDateTime.toLocalDate() }
            .forEach { (date, logs) ->
                val count = unmatchedCountByDate[date] ?: return@forEach
                logs.sortedBy { it.logDateTime }.take(count).forEach { log ->
                    reminderLogDao.updateCompletedStatus(log.id, true)
                }
            }
    }

    suspend fun delete(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    /**
     * @param includePast generate occurrences that have already passed too (from startDate).
     * Only true on insert; update() keeps this false so it touches the future only and never
     * resurrects or re-flags completed past logs.
     */
    private suspend fun generateLogsForReminder(
        reminder: Reminder,
        now: LocalDateTime = LocalDateTime.now(),
        includePast: Boolean = false
    ) {
        if (!reminder.isActive) return

        val currentDate = now.toLocalDate()
        val loopStart = when {
            includePast -> reminder.startDate
            reminder.startDate > currentDate -> reminder.startDate
            else -> currentDate
        }
        // Materialize only a short forward window. The calendar computes further-out
        // occurrences from the schedule (see syntheticFutureLogs) and the alarm chain
        // self-sustains via the fire-hook top-up, so a year of rows isn't needed (todo #13).
        // Anchored on max(today, loopStart) so a future-dated reminder still gets its first
        // occurrence; widened past a long interval so the next occurrence always lands inside
        // the window (otherwise the chain would have nothing to arm). endDate still caps it.
        val windowDays = maxOf(FORWARD_WINDOW_DAYS, (reminder.dayInterval?.toLong() ?: 0L) + 1L)
        val horizon = maxOf(currentDate, loopStart).plusDays(windowDays)
        val endDate = reminder.endDate?.let { if (it < horizon) it else horizon } ?: horizon
        // distinct(): reminders saved before the form deduped times may still hold
        // duplicates, which would insert two logs for the same occurrence
        val times = reminder.reminderTimes.distinct()
        // Dedupe from the start of the first generated day so today's already-passed
        // times (which survive deletion) aren't re-inserted
        val existingDateTimes = reminderLogDao
            .getExistingLogDateTimesForReminder(reminder.id, loopStart.atStartOfDay())
            .toHashSet()

        // Collect the whole run and insert in one transaction. A recurring reminder can be
        // hundreds-to-thousands of rows (esp. with backfill); one-at-a-time inserts are each
        // their own transaction and would jank the save.
        val newLogs = mutableListOf<ReminderLog>()
        // Past occurrences only on a backfilling insert; otherwise a paused or freshly-edited
        // reminder can't have missed a slot it wasn't around for.
        fun addIfNew(date: LocalDate, time: LocalTime) {
            val logDateTime = LocalDateTime.of(date, time)
            if ((includePast || logDateTime > now) && logDateTime !in existingDateTimes) {
                newLogs += ReminderLog(reminderId = reminder.id, title = reminder.title, logDateTime = logDateTime)
            }
        }

        if (reminder.reminderType == ReminderType.EVERY_N_DAYS) {
            val interval = reminder.dayInterval ?: 1
            val daysSinceStart = ChronoUnit.DAYS.between(reminder.startDate, loopStart)
            val offset = daysSinceStart % interval
            var date = if (offset == 0L) loopStart else loopStart.plusDays(interval - offset)
            while (date <= endDate) {
                for (time in times) addIfNew(date, time)
                date = date.plusDays(interval.toLong())
            }
        } else {
            var date = loopStart
            while (date <= endDate) {
                if (reminder.reminderDays.contains(date.dayOfWeek.value)) {
                    for (time in times) addIfNew(date, time)
                }
                date = date.plusDays(1)
            }
        }

        if (newLogs.isNotEmpty()) reminderLogDao.insertAll(newLogs)
    }

    /**
     * Advance a reminder's materialized future logs to the current horizon. Purely additive
     * (dedup skips what already exists, nothing is deleted), so it's safe to call repeatedly:
     * from the alarm chain when it reaches its last row, and from ReminderWorker on app open /
     * boot. This is what stops a no-end-date reminder from running out of logs (todo #13).
     */
    suspend fun topUpLogs(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()) {
        generateLogsForReminder(reminder, now)
    }

    suspend fun updateLogCompletedStatus(logId: Int, completed: Boolean) {
        reminderLogDao.updateCompletedStatus(logId, completed)
    }

    /**
     * Records a completion for a slot that has no log — a time that had already
     * passed when the reminder was created or edited (logs are never generated
     * in the past), checked off after the fact from the reminder card.
     */
    suspend fun insertCompletedLog(reminder: Reminder, dateTime: LocalDateTime) {
        reminderLogDao.insert(
            ReminderLog(
                reminderId = reminder.id,
                title = reminder.title,
                logDateTime = dateTime,
                completed = true
            )
        )
    }

    /**
     * Done/missed tally over an archived reminder's whole run, for its archive-row subtitle.
     * A one-shot suspend aggregate — not a fourth Room observer (see CLAUDE.md). Spans
     * startDate up to whichever came first: the manual archive moment or the end of endDate.
     * archivedAt matters since a manual archive keeps a future endDate, and slots ticked off
     * early past the archive point would otherwise count. Falls back to today's end.
     */
    suspend fun archiveStats(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): OccurrenceCounts {
        val end = listOfNotNull(reminder.archivedAt, reminder.endDate?.atTime(LocalTime.MAX)).minOrNull()
            ?: now.toLocalDate().atTime(LocalTime.MAX)
        val logs = reminderLogDao.getLogsForReminderInRange(
            reminder.id, reminder.startDate.atStartOfDay(), end
        )
        return countOutcomes(logs, now)
    }

    suspend fun updateRemindersOrder(reminders: List<Reminder>) {
        reminders.forEachIndexed { index, reminder ->
            reminderDao.updateSortOrder(reminder.id, index)
        }
    }

    private companion object {
        /**
         * How far ahead to materialize logs. The calendar shows occurrences beyond this by
         * computing them from the schedule, and the alarm chain is kept alive by the fire-hook
         * top-up, so this is just a buffer — not the reminder's real reach (todo #13).
         */
        const val FORWARD_WINDOW_DAYS = 45L
    }
}
