package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.model.MedicationIntake
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class MedicationIntakeRepository(private val medicationIntakeDao: MedicationIntakeDao) {
    fun getIntakesForReminder(reminderId: Int): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getIntakesForReminder(reminderId)
    }

    suspend fun insert(intake: MedicationIntake) {
        medicationIntakeDao.insert(intake)
    }

    suspend fun update(intake: MedicationIntake) {
        medicationIntakeDao.update(intake)
    }

    fun getIntakesForDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getIntakesForDateRange(start, end)
    }
}