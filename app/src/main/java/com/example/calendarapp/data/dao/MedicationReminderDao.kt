package com.example.calendarapp.data.dao

import androidx.room.*
import com.example.calendarapp.data.model.MedicationReminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MedicationReminderDao {
    @Query("SELECT * FROM medication_reminders")
    fun getAllReminders(): Flow<List<MedicationReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long

    @Update
    suspend fun updateReminder(reminder: MedicationReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminder)

    @Query("SELECT * FROM medication_reminders WHERE startDate <= :date AND (endDate IS NULL OR endDate >= :date)")
    fun getActiveReminders(date: LocalDate): Flow<List<MedicationReminder>>

    @Query("SELECT * FROM medication_reminders WHERE id = :id")
    fun getReminderById(id: Int): Flow<MedicationReminder>
}