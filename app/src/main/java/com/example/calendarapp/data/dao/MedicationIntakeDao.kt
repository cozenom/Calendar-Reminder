package com.example.calendarapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.calendarapp.data.model.ReminderLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ReminderLogDao {
    @Insert
    suspend fun insert(log: ReminderLog)

    @Update
    suspend fun update(log: ReminderLog)

    @Delete
    suspend fun delete(log: ReminderLog)

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId")
    fun getLogsForReminder(reminderId: Int): Flow<List<ReminderLog>>

    @Query("SELECT * FROM reminder_logs WHERE logDateTime BETWEEN :start AND :end")
    fun getLogsForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<ReminderLog>>

    @Query("SELECT * FROM reminder_logs WHERE logDateTime <= :dateTime AND completed = 0")
    fun getMissedLogs(dateTime: LocalDateTime): Flow<List<ReminderLog>>

    @Query("SELECT * FROM reminder_logs WHERE logDateTime BETWEEN :start AND :end")
    fun getUpcomingLogs(start: LocalDateTime, end: LocalDateTime): List<ReminderLog>

    @Query("UPDATE reminder_logs SET completed = :completed WHERE id = :logId")
    suspend fun updateCompletedStatus(logId: Int, completed: Boolean)

    @Query("SELECT * FROM reminder_logs WHERE id = :logId")
    suspend fun getLogById(logId: Int): ReminderLog?

    @Query("SELECT * FROM reminder_logs WHERE logDateTime > :now ORDER BY logDateTime ASC")
    suspend fun getFutureLogs(now: LocalDateTime): List<ReminderLog>

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime > :after ORDER BY logDateTime ASC LIMIT 1")
    suspend fun getNextLogForReminder(reminderId: Int, after: LocalDateTime): ReminderLog?

    @Query("SELECT * FROM reminder_logs WHERE logDateTime > :since AND logDateTime < :now AND completed = 0")
    suspend fun getMissedLogsList(since: LocalDateTime, now: LocalDateTime): List<ReminderLog>

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime > :fromDateTime")
    suspend fun deleteFutureLogsForReminder(reminderId: Int, fromDateTime: LocalDateTime)
}
