package com.davidp.simpleweeklyreminders.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.davidp.simpleweeklyreminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE startDate <= :date AND (endDate IS NULL OR endDate >= :date)")
    fun getActiveReminders(date: LocalDate): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderById(id: Int): Flow<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderByIdOnce(id: Int): Reminder?

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<Reminder>
}
