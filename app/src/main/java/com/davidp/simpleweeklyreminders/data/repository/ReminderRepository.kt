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
        generateLogsForReminder(reminder.copy(id = id.toInt()), now)
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

    private suspend fun generateLogsForReminder(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()) {
        if (!reminder.isActive) return

        val currentDate = now.toLocalDate()
        val endDate = reminder.endDate ?: currentDate.plusYears(1)
        val loopStart = if (reminder.startDate > currentDate) reminder.startDate else currentDate
        // distinct(): reminders saved before the form deduped times may still hold
        // duplicates, which would insert two logs for the same occurrence
        val times = reminder.reminderTimes.distinct()
        // Dedupe from the start of the first generated day so today's already-passed
        // times (which survive deletion) aren't re-inserted
        val existingDateTimes = reminderLogDao
            .getExistingLogDateTimesForReminder(reminder.id, loopStart.atStartOfDay())
            .toHashSet()

        if (reminder.reminderType == ReminderType.EVERY_N_DAYS) {
            val interval = reminder.dayInterval ?: 1
            val daysSinceStart = ChronoUnit.DAYS.between(reminder.startDate, loopStart)
            val offset = daysSinceStart % interval
            var date = if (offset == 0L) loopStart else loopStart.plusDays(interval - offset)
            while (date <= endDate) {
                for (time in times) {
                    val logDateTime = LocalDateTime.of(date, time)
                    // Never create already-passed occurrences: a reminder that was
                    // paused or didn't exist at that time can't have missed it
                    if (logDateTime > now && logDateTime !in existingDateTimes) {
                        reminderLogDao.insert(ReminderLog(
                            reminderId = reminder.id,
                            title = reminder.title,
                            logDateTime = logDateTime
                        ))
                    }
                }
                date = date.plusDays(interval.toLong())
            }
        } else {
            var date = loopStart
            while (date <= endDate) {
                if (reminder.reminderDays.contains(date.dayOfWeek.value)) {
                    for (time in times) {
                        val logDateTime = LocalDateTime.of(date, time)
                        if (logDateTime > now && logDateTime !in existingDateTimes) {
                            reminderLogDao.insert(ReminderLog(
                                reminderId = reminder.id,
                                title = reminder.title,
                                logDateTime = logDateTime
                            ))
                        }
                    }
                }
                date = date.plusDays(1)
            }
        }
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
     * startDate..endDate; falls back to today's end if endDate is somehow null.
     */
    suspend fun archiveStats(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): OccurrenceCounts {
        val end = (reminder.endDate ?: now.toLocalDate()).atTime(LocalTime.MAX)
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
}
