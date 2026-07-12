package com.davidp.simpleweeklyreminders.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ReminderLogDao {
    @Insert
    suspend fun insert(log: ReminderLog)

    @Query("SELECT * FROM reminder_logs WHERE logDateTime BETWEEN :start AND :end")
    fun getLogsForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<ReminderLog>>

    @Query("UPDATE reminder_logs SET completed = :completed WHERE id = :logId")
    suspend fun updateCompletedStatus(logId: Int, completed: Boolean)

    @Query("SELECT * FROM reminder_logs WHERE id = :logId")
    suspend fun getLogById(logId: Int): ReminderLog?

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime > :after ORDER BY logDateTime ASC LIMIT 1")
    suspend fun getNextLogForReminder(reminderId: Int, after: LocalDateTime): ReminderLog?

    @Query("SELECT * FROM reminder_logs WHERE logDateTime > :since AND logDateTime < :now AND completed = 0")
    suspend fun getMissedLogsList(since: LocalDateTime, now: LocalDateTime): List<ReminderLog>

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime > :fromDateTime")
    suspend fun deleteFutureLogsForReminder(reminderId: Int, fromDateTime: LocalDateTime)

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime > :fromDateTime AND completed = 0")
    suspend fun deleteFutureIncompleteLogsForReminder(reminderId: Int, fromDateTime: LocalDateTime)

    @Query("SELECT logDateTime FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime >= :from")
    suspend fun getExistingLogDateTimesForReminder(reminderId: Int, from: LocalDateTime): List<LocalDateTime>

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId AND logDateTime > :after")
    suspend fun getFutureLogsForReminder(reminderId: Int, after: LocalDateTime): List<ReminderLog>

    @Query("UPDATE reminder_logs SET title = :title WHERE reminderId = :reminderId")
    suspend fun updateTitleForReminder(reminderId: Int, title: String)
}
