package com.example.calendarapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    // Prescription tracking queries
    @Query("UPDATE medication_reminders SET currentInventory = :inventory WHERE id = :id")
    suspend fun updateInventory(id: Int, inventory: Int)

    @Query("SELECT currentInventory FROM medication_reminders WHERE id = :id")
    suspend fun getCurrentInventory(id: Int): Int?
}