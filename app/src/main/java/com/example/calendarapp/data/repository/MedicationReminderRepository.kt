package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.*

class MedicationReminderRepository(
    private val medicationReminderDao: MedicationReminderDao,
    private val medicationIntakeDao: MedicationIntakeDao
) {
    val allReminders: Flow<List<MedicationReminder>> = medicationReminderDao.getAllReminders()

    suspend fun insertOrUpdateReminder(reminder: MedicationReminder): Int {
        return if (reminder.id == 0) {
            // Insert new reminder
            val id = medicationReminderDao.insertReminder(reminder).toInt()
            generateIntakesForReminder(reminder.copy(id = id))
            id
        } else {
            // Update existing reminder
            medicationReminderDao.updateReminder(reminder)
            // Instead of deleting, we'll update existing intakes and add new ones if necessary
            updateIntakesForReminder(reminder)
            reminder.id
        }
    }

    fun getIntakesWithRemindersForDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Pair<MedicationIntake, MedicationReminder>>> = flow {
        combine(
            medicationIntakeDao.getIntakesForDateRange(start, end),
            medicationReminderDao.getAllReminders()
        ) { intakes, reminders ->
            intakes.mapNotNull { intake ->
                val reminder = reminders.find { it.id == intake.reminderId }
                if (reminder != null) intake to reminder else null
            }
        }.collect { pairedIntakes ->
            emit(pairedIntakes)
        }
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
                        intakeDateTime = intakeDateTime
                    )
                    medicationIntakeDao.insert(intake)
                }
            }
            date = date.plusDays(1)
        }
    }

    private suspend fun updateIntakesForReminder(reminder: MedicationReminder) {
        // Get existing intakes for this reminder
        val existingIntakes = medicationIntakeDao.getIntakesForReminder(reminder.id).first()

        // Generate new intakes based on updated reminder
        val newIntakes = generateNewIntakes(reminder)

        // Delete intakes that are no longer needed
        existingIntakes.forEach { intake ->
            if (!newIntakes.any { it.intakeDateTime == intake.intakeDateTime }) {
                medicationIntakeDao.delete(intake)
            }
        }

        // Insert new intakes
        newIntakes.forEach { intake ->
            if (!existingIntakes.any { it.intakeDateTime == intake.intakeDateTime }) {
                medicationIntakeDao.insert(intake)
            }
        }
    }

    private fun generateNewIntakes(reminder: MedicationReminder): List<MedicationIntake> {
        val intakes = mutableListOf<MedicationIntake>()
        val currentDate = LocalDate.now()
        val endDate = reminder.endDate ?: currentDate.plusYears(1)

        var date = reminder.startDate
        while (date <= endDate) {
            if (reminder.reminderDays.contains(date.dayOfWeek.value)) {
                for (time in reminder.reminderTimes) {
                    val intakeDateTime = LocalDateTime.of(date, time)
                    intakes.add(
                        MedicationIntake(
                            reminderId = reminder.id,
                            intakeDateTime = intakeDateTime
                        )
                    )
                }
            }
            date = date.plusDays(1)
        }
        return intakes
    }

    suspend fun insert(reminder: MedicationReminder): Long {
        val id = medicationReminderDao.insertReminder(reminder)
        generateIntakesForReminder(reminder.copy(id = id.toInt()))
        return id
    }

    suspend fun update(reminder: MedicationReminder) {
        medicationReminderDao.updateReminder(reminder)
        // You might want to regenerate intakes here, depending on your use case
    }

    suspend fun delete(reminder: MedicationReminder) {
        medicationReminderDao.deleteReminder(reminder)
        // Intakes will be automatically deleted due to the foreign key constraint
    }

//    private suspend fun generateIntakesForReminder(reminder: MedicationReminder) {
//        val currentDate = LocalDate.now()
//        val endDate = reminder.endDate ?: currentDate.plusYears(1)
//
//        var date = reminder.startDate
//        while (date <= endDate) {
//            if (reminder.reminderDays.contains(date.dayOfWeek.value)) {
//                for (time in reminder.reminderTimes) {
//                    val intakeDateTime = LocalDateTime.of(date, time)
//                    val intake = MedicationIntake(
//                        reminderId = reminder.id,
//                        intakeDateTime = intakeDateTime
//                    )
//                    medicationIntakeDao.insert(intake)
//                }
//            }
//            date = date.plusDays(1)
//        }
//    }

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

    fun getUpcomingIntakes(start: LocalDateTime, end: LocalDateTime): List<MedicationIntake> {
        return medicationIntakeDao.getUpcomingIntakes(start, end)
    }

    fun getActiveReminders(date: LocalDate): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getActiveReminders(date)
    }

    fun getReminderById(id: Int): Flow<MedicationReminder> {
        return medicationReminderDao.getReminderById(id)
    }

    fun getRefillReminders(date: LocalDate): Flow<List<MedicationReminder>> {
        return medicationReminderDao.getRefillReminders(date)
    }
}