package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.model.MedicationIntake
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class MedicationIntakeRepository(private val medicationIntakeDao: MedicationIntakeDao) {
    suspend fun insert(intake: MedicationIntake) {
        medicationIntakeDao.insert(intake)
    }

    suspend fun update(intake: MedicationIntake) {
        medicationIntakeDao.update(intake)
    }

    suspend fun delete(intake: MedicationIntake) {
        medicationIntakeDao.delete(intake)
    }

    fun getIntakesForReminder(reminderId: Int): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getIntakesForReminder(reminderId)
    }

    fun getIntakesForDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getIntakesForDateRange(start, end)
    }

    suspend fun updateTakenStatus(intakeId: Int, taken: Boolean) {
        medicationIntakeDao.updateTakenStatus(intakeId, taken)
    }

    fun getMissedIntakes(dateTime: LocalDateTime): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getMissedIntakes(dateTime)
    }

    suspend fun getNextIntake(now: LocalDateTime): MedicationIntake? {
        return medicationIntakeDao.getNextIntake(now)
    }

    suspend fun getFutureIntakes(now: LocalDateTime): List<MedicationIntake> {
        return medicationIntakeDao.getFutureIntakes(now)
    }
}