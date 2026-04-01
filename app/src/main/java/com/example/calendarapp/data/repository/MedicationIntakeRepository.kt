package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.ReminderLogDao
import com.example.calendarapp.data.model.ReminderLog
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
