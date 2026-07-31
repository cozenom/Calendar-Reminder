package com.davidp.simpleweeklyreminders.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.davidp.simpleweeklyreminders.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderByIdOnce(id: Int): Reminder?

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<Reminder>

    @Query("UPDATE reminders SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Int, sortOrder: Int)
}
