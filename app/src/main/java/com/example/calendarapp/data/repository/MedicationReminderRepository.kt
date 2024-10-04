package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationReminderRepository(
    private val medicationReminderDao: MedicationReminderDao,
    private val medicationIntakeDao: MedicationIntakeDao
) {
    val allReminders: Flow<List<MedicationReminder>> = medicationReminderDao.getAllReminders()

    suspend fun insert(reminder: MedicationReminder) {
        val id = medicationReminderDao.insertReminder(reminder)
        generateIntakesForReminder(reminder.copy(id = id.toInt()))
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

    fun getReminderById(id: Int): Flow<MedicationReminder> {
        return medicationReminderDao.getReminderById(id)
    }

    private suspend fun generateIntakesForReminder(reminder: MedicationReminder) {
        val currentDate = LocalDate.now()
        val endDate = reminder.endDate ?: currentDate.plusYears(1)

        var date = reminder.startDate
        while (date <= endDate) {
            if (reminder.reminderDays.contains(date.dayOfWeek.value)) {
                for (time in reminder.reminderTimes) {
                    val intakeDateTime = LocalDateTime.of(date, time)
                    val intake = MedicationIntake(
                        reminderId = reminder.id,
                        medicationName = reminder.medicationName,
                        intakeDateTime = intakeDateTime,
                        status = "Scheduled"
                    )
                    medicationIntakeDao.insert(intake)
                }
            }
            date = date.plusDays(1)
        }
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

    suspend fun updateIntakeTakenStatus(intakeId: Int, taken: Boolean) {
        medicationIntakeDao.updateTakenStatus(intakeId, taken)
    }

    fun getMissedIntakes(dateTime: LocalDateTime): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getMissedIntakes(dateTime)
    }

    fun getUpcomingIntakes(start: LocalDateTime, end: LocalDateTime): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getUpcomingIntakes(start, end)
    }
}