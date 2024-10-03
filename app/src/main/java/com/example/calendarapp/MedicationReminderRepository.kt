package com.example.calendarapp

import kotlinx.coroutines.flow.Flow

class MedicationReminderRepository(private val medicationReminderDao: MedicationReminderDao) {
    val allReminders: Flow<List<MedicationReminder>> = medicationReminderDao.getAllReminders()

    suspend fun insert(reminder: MedicationReminder) {
        medicationReminderDao.insertReminder(reminder)
    }

    suspend fun delete(reminder: MedicationReminder) {
        medicationReminderDao.deleteReminder(reminder)
    }
}