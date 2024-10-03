package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.model.MedicationReminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MedicationReminderRepository(private val medicationReminderDao: MedicationReminderDao) {
    val allReminders: Flow<List<MedicationReminder>> = medicationReminderDao.getAllReminders()

    suspend fun insert(reminder: MedicationReminder) {
        medicationReminderDao.insertReminder(reminder)
    }

    suspend fun update(reminder: MedicationReminder) {
        medicationReminderDao.updateReminder(reminder)
    }

    suspend fun delete(reminder: MedicationReminder) {
        medicationReminderDao.deleteReminder(reminder)
    }

    fun getActiveReminders(date: LocalDate): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getActiveReminders(date)
    }
}