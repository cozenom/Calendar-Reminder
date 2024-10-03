package com.example.calendarapp

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationReminderDao {
    @Query("SELECT * FROM medication_reminders")
    fun getAllReminders(): Flow<List<MedicationReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminder)
}