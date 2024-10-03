package com.example.calendarapp.data.dao

import androidx.room.*
import com.example.calendarapp.data.model.MedicationIntake
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MedicationIntakeDao {
    @Query("SELECT * FROM medication_intake WHERE reminderId = :reminderId")
    fun getIntakesForReminder(reminderId: Int): Flow<List<MedicationIntake>>

    @Insert
    suspend fun insert(intake: MedicationIntake)

    @Update
    suspend fun update(intake: MedicationIntake)

    @Query("SELECT * FROM medication_intake WHERE intakeDateTime BETWEEN :start AND :end")
    fun getIntakesForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<MedicationIntake>>
}