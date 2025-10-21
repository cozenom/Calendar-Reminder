package com.example.calendarapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.model.PrescriptionRefill
import com.example.calendarapp.data.notification.MedicationReminderWorker
import com.example.calendarapp.data.repository.MedicationIntakeRepository
import com.example.calendarapp.data.repository.MedicationReminderRepository
import com.example.calendarapp.data.repository.PrescriptionRefillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class MedicationReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicationReminderRepository
    private val intakeRepository: MedicationIntakeRepository
    private val refillRepository: PrescriptionRefillRepository
    val allReminders: Flow<List<MedicationReminder>>

    init {
        val database = AppDatabase.getDatabase(application)
        val reminderDao = database.medicationReminderDao()
        val intakeDao = database.medicationIntakeDao()
        val refillDao = database.prescriptionRefillDao()
        repository = MedicationReminderRepository(reminderDao, intakeDao, refillDao, application)
        allReminders = repository.allReminders
        intakeRepository = MedicationIntakeRepository(intakeDao)
        refillRepository = PrescriptionRefillRepository(refillDao, reminderDao)
    }

    fun insert(reminder: MedicationReminder) = viewModelScope.launch {
        repository.insert(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun insertWithPrescription(
        reminder: MedicationReminder,
        pillsPerRefill: Int,
        totalRefills: Int
    ) = viewModelScope.launch {
        val reminderId = repository.insert(reminder)
        // Automatically initialize prescription tracking if enabled
        if (reminder.inventoryTrackingEnabled) {
            refillRepository.initializePrescriptionTracking(reminderId.toInt(), pillsPerRefill, totalRefills)
        }
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun update(reminder: MedicationReminder) = viewModelScope.launch {
        repository.update(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun updateWithPrescription(
        reminder: MedicationReminder,
        pillsPerRefill: Int,
        totalRefills: Int
    ) = viewModelScope.launch {
        repository.update(reminder)
        // If tracking is enabled, sync prescription record
        if (reminder.inventoryTrackingEnabled) {
            val existingRefill = refillRepository.getLatestRefill(reminder.id)
            if (existingRefill == null) {
                // No prescription record exists, initialize it
                refillRepository.initializePrescriptionTracking(reminder.id, pillsPerRefill, totalRefills)
            } else {
                // Prescription record exists, update the counts to match edited values
                refillRepository.updateLatestRefillCounts(reminder.id, totalRefills, pillsPerRefill)
            }
        }
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun delete(reminder: MedicationReminder) = viewModelScope.launch {
        repository.delete(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun getActiveReminders(date: LocalDate): Flow<List<MedicationReminder>> {
        return repository.getActiveReminders(date)
    }

    fun updateIntakeTakenStatus(intakeId: Int, taken: Boolean, reminder: MedicationReminder) =
        viewModelScope.launch {
            repository.updateIntakeTakenStatus(intakeId, taken, reminder)
        }

    fun getMissedIntakes(dateTime: LocalDateTime): Flow<List<MedicationIntake>> {
        return intakeRepository.getMissedIntakes(dateTime)
    }

    fun getIntakesForMonth(yearMonth: YearMonth): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            yearMonth.atDay(1).atStartOfDay(),
            yearMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1)
        )
    }

    fun getIntakesForDate(date: LocalDate): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusNanos(1)
        )
    }

    // Prescription refill methods
    fun getRefillsForReminder(reminderId: Int): Flow<List<PrescriptionRefill>> {
        return refillRepository.getRefillsForReminder(reminderId)
    }

    fun getRefillsForMonth(yearMonth: YearMonth): Flow<List<PrescriptionRefill>> {
        return refillRepository.getRefillsForDateRange(
            yearMonth.atDay(1),
            yearMonth.atEndOfMonth()
        )
    }

    fun getAllRefills(): Flow<List<PrescriptionRefill>> {
        return refillRepository.getAllRefills()
    }

    suspend fun getLatestRefill(reminderId: Int): PrescriptionRefill? {
        return refillRepository.getLatestRefill(reminderId)
    }

    fun getLatestRefillFlow(reminderId: Int): Flow<PrescriptionRefill?> {
        return refillRepository.getLatestRefillFlow(reminderId)
    }

    fun recordRefillPickup(
        reminderId: Int,
        currentReminder: MedicationReminder,
        latestRefill: PrescriptionRefill,
        pickupDate: LocalDate = LocalDate.now()
    ) = viewModelScope.launch {
        refillRepository.recordRefillPickup(reminderId, currentReminder, latestRefill, pickupDate)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun recordNewPrescription(
        reminderId: Int,
        pillsPerRefill: Int,
        totalRefills: Int
    ) = viewModelScope.launch {
        refillRepository.recordNewPrescription(reminderId, pillsPerRefill, totalRefills)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }
}