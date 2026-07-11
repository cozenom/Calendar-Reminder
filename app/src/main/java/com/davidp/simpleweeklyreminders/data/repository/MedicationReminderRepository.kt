package com.davidp.simpleweeklyreminders.data.repository

import com.davidp.simpleweeklyreminders.data.dao.ReminderDao
import com.davidp.simpleweeklyreminders.data.dao.ReminderLogDao
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val reminderLogDao: ReminderLogDao
) {
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    suspend fun insert(reminder: Reminder): Long {
        val id = reminderDao.insertReminder(reminder)
        generateLogsForReminder(reminder.copy(id = id.toInt()))
        return id
    }

    suspend fun update(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
        // Keep historical log snapshots in sync with the new title
        reminderLogDao.updateTitleForReminder(reminder.id, reminder.title)

        val now = LocalDateTime.now()
        if (!reminder.isActive) {
            // Paused: nothing gets regenerated, so keep completed-early logs —
            // the carry-over below handles them when the reminder is reactivated
            reminderLogDao.deleteFutureIncompleteLogsForReminder(reminder.id, now)
            return
        }

        // Remember completed-early future logs so their completion survives regeneration
        val completedCountByDate = reminderLogDao.getFutureLogsForReminder(reminder.id, now)
            .filter { it.completed }
            .groupingBy { it.logDateTime.toLocalDate() }
            .eachCount()

        // Delete all future logs to avoid duplicates when regenerating
        reminderLogDao.deleteFutureLogsForReminder(reminder.id, now)
        // Regenerate logs with updated schedule
        generateLogsForReminder(reminder)

        // Re-apply completions to the regenerated logs on the same dates
        if (completedCountByDate.isNotEmpty()) {
            reminderLogDao.getFutureLogsForReminder(reminder.id, now)
                .groupBy { it.logDateTime.toLocalDate() }
                .forEach { (date, logs) ->
                    val count = completedCountByDate[date] ?: return@forEach
                    logs.sortedBy { it.logDateTime }.take(count).forEach { log ->
                        reminderLogDao.updateCompletedStatus(log.id, true)
                    }
                }
        }
    }

    suspend fun delete(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    fun getActiveReminders(date: LocalDate): Flow<List<Reminder>> {
        return reminderDao.getActiveReminders(date)
    }

    private suspend fun generateLogsForReminder(reminder: Reminder) {
        if (!reminder.isActive) return

        val now = LocalDateTime.now()
        val currentDate = LocalDate.now()
        val endDate = reminder.endDate ?: currentDate.plusYears(1)
        val loopStart = if (reminder.startDate > currentDate) reminder.startDate else currentDate
        // Dedupe from the start of the first generated day so today's already-passed
        // times (which survive deletion) aren't re-inserted
        val existingDateTimes = reminderLogDao
            .getExistingLogDateTimesForReminder(reminder.id, loopStart.atStartOfDay())
            .toHashSet()

        if (reminder.dayInterval != null) {
            val daysSinceStart = ChronoUnit.DAYS.between(reminder.startDate, loopStart)
            val offset = daysSinceStart % reminder.dayInterval
            var date = if (offset == 0L) loopStart else loopStart.plusDays(reminder.dayInterval - offset)
            while (date <= endDate) {
                for (time in reminder.reminderTimes) {
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
                date = date.plusDays(reminder.dayInterval.toLong())
            }
        } else {
            var date = loopStart
            while (date <= endDate) {
                if (reminder.reminderDays.contains(date.dayOfWeek.value)) {
                    for (time in reminder.reminderTimes) {
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

    suspend fun updateRemindersOrder(reminders: List<Reminder>) {
        reminders.forEachIndexed { index, reminder ->
            reminderDao.updateSortOrder(reminder.id, index)
        }
    }
}
