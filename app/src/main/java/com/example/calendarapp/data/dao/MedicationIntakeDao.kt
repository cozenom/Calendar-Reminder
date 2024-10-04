package com.example.calendarapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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
    fun getIntakesForDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<MedicationIntake>>

    @Query("UPDATE medication_intake SET taken = :taken WHERE id = :intakeId")
    suspend fun updateTakenStatus(intakeId: Int, taken: Boolean)

    @Query("SELECT * FROM medication_intake WHERE intakeDateTime <= :dateTime AND taken = 0")
    fun getMissedIntakes(dateTime: LocalDateTime): Flow<List<MedicationIntake>>

    @Query("SELECT * FROM medication_intake WHERE intakeDateTime BETWEEN :start AND :end")
    fun getUpcomingIntakes(start: LocalDateTime, end: LocalDateTime): Flow<List<MedicationIntake>>
}