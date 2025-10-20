package com.example.calendarapp.data.repository

import android.content.Context
import com.example.calendarapp.data.dao.MedicationIntakeDao
import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.dao.PrescriptionRefillDao
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.notification.InventoryNotificationHelper
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationReminderRepository(
    private val medicationReminderDao: MedicationReminderDao,
    private val medicationIntakeDao: MedicationIntakeDao,
    private val refillDao: PrescriptionRefillDao,
    private val context: Context
) {
    val allReminders: Flow<List<MedicationReminder>> = medicationReminderDao.getAllReminders()

    suspend fun insert(reminder: MedicationReminder): Long {
        val id = medicationReminderDao.insertReminder(reminder)
        generateIntakesForReminder(reminder.copy(id = id.toInt()))
        return id
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
                        intakeDateTime = intakeDateTime
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

    fun getMissedIntakes(dateTime: LocalDateTime): Flow<List<MedicationIntake>> {
        return medicationIntakeDao.getMissedIntakes(dateTime)
    }

    fun getUpcomingIntakes(start: LocalDateTime, end: LocalDateTime): List<MedicationIntake> {
        return medicationIntakeDao.getUpcomingIntakes(start, end)
    }

    /**
     * Update intake taken status and adjust inventory if prescription tracking is enabled.
     * Also triggers inventory alerts when needed.
     *
     * Note: Inventory only decrements when marking as taken.
     * Marking as not taken does NOT increment inventory back.
     */
    suspend fun updateIntakeTakenStatus(
        intakeId: Int,
        taken: Boolean,
        reminder: MedicationReminder
    ) {
        val intake = medicationIntakeDao.getIntakeById(intakeId)

        if (intake != null && reminder.inventoryTrackingEnabled) {
            // Only decrement inventory when marking as taken
            if (taken && !intake.taken) {
                // Decrement inventory (don't go negative)
                val newInventory = maxOf(0, reminder.currentInventory - reminder.dosagePerIntake)
                medicationReminderDao.updateInventory(reminder.id, newInventory)

                // Check and send inventory alerts after decrementing
                val latestRefill = refillDao.getLatestRefill(reminder.id)
                InventoryNotificationHelper.checkAndSendInventoryAlerts(
                    context,
                    reminder.copy(currentInventory = newInventory),
                    latestRefill
                )
            }
            // When marking as not taken, we don't change inventory
        }

        // Update the intake status
        medicationIntakeDao.updateTakenStatus(intakeId, taken)
    }
}