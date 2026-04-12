package com.davidp.simpleweeklyreminders.data.repository

import com.davidp.simpleweeklyreminders.data.dao.ReminderLogDao
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class ReminderLogRepository(private val reminderLogDao: ReminderLogDao) {
    suspend fun insert(log: ReminderLog) = reminderLogDao.insert(log)
    suspend fun delete(log: ReminderLog) = reminderLogDao.delete(log)

    fun getLogsForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<ReminderLog>> =
        reminderLogDao.getLogsForDateRange(start, end)

    fun getMissedLogs(dateTime: LocalDateTime): Flow<List<ReminderLog>> =
        reminderLogDao.getMissedLogs(dateTime)

}
