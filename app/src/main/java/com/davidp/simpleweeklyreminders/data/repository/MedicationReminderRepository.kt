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
        // Delete all logs from today onwards to avoid duplicates when regenerating
        reminderLogDao.deleteFutureLogsForReminder(reminder.id, LocalDateTime.now())
        // Regenerate logs with updated schedule
        generateLogsForReminder(reminder)
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
        val existingDateTimes = reminderLogDao
            .getExistingLogDateTimesForReminder(reminder.id, now)
            .toHashSet()

        if (reminder.dayInterval != null) {
            val daysSinceStart = ChronoUnit.DAYS.between(reminder.startDate, loopStart)
            val offset = daysSinceStart % reminder.dayInterval
            var date = if (offset == 0L) loopStart else loopStart.plusDays(reminder.dayInterval - offset)
            while (date <= endDate) {
                for (time in reminder.reminderTimes) {
                    val logDateTime = LocalDateTime.of(date, time)
                    if (logDateTime !in existingDateTimes) {
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
                        if (logDateTime !in existingDateTimes) {
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

    suspend fun updateRemindersOrder(reminders: List<Reminder>) {
        reminders.forEachIndexed { index, reminder ->
            reminderDao.updateSortOrder(reminder.id, index)
        }
    }
}
