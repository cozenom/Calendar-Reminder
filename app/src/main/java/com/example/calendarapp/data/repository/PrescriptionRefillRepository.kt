package com.example.calendarapp.data.repository

import com.example.calendarapp.data.dao.MedicationReminderDao
import com.example.calendarapp.data.dao.PrescriptionRefillDao
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.model.PrescriptionRefill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PrescriptionRefillRepository(
    private val refillDao: PrescriptionRefillDao,
    private val reminderDao: MedicationReminderDao
) {
    fun getRefillsForReminder(reminderId: Int): Flow<List<PrescriptionRefill>> {
        return refillDao.getRefillsForReminder(reminderId)
    }

    fun getRefillsForDateRange(start: LocalDate, end: LocalDate): Flow<List<PrescriptionRefill>> {
        return refillDao.getRefillsForDateRange(start, end)
    }

    suspend fun getLatestRefill(reminderId: Int): PrescriptionRefill? {
        return refillDao.getLatestRefill(reminderId)
    }

    /**
     * Record a new prescription (initial or renewed) for a medication.
     * Creates the first refill entry and adds pills to inventory.
     */
    suspend fun recordNewPrescription(
        reminderId: Int,
        pillsPerRefill: Int,
        totalRefills: Int,
        pickupDate: LocalDate = LocalDate.now()
    ) {
        val refill = PrescriptionRefill(
            reminderId = reminderId,
            pickupDate = pickupDate,
            pillsPerRefill = pillsPerRefill,
            totalRefillsAuthorized = totalRefills,
            refillsRemaining = totalRefills
        )
        refillDao.insert(refill)

        // Update medication inventory by adding pills from the new prescription pickup
        val currentInventory = reminderDao.getCurrentInventory(reminderId) ?: 0
        val newInventory = currentInventory + pillsPerRefill
        reminderDao.updateInventory(reminderId, newInventory)
    }

    /**
     * Record picking up a refill.
     * Creates a new entry and updates the medication's inventory.
     */
    suspend fun recordRefillPickup(
        reminderId: Int,
        currentReminder: MedicationReminder,
        latestRefill: PrescriptionRefill,
        pickupDate: LocalDate = LocalDate.now()
    ) {
        // Create new refill entry with decremented count
        val newRefill = PrescriptionRefill(
            reminderId = reminderId,
            pickupDate = pickupDate,
            pillsPerRefill = latestRefill.pillsPerRefill,
            totalRefillsAuthorized = latestRefill.totalRefillsAuthorized,
            refillsRemaining = maxOf(0, latestRefill.refillsRemaining - 1)
        )
        refillDao.insert(newRefill)

        // Update medication inventory by adding pills
        val newInventory = currentReminder.currentInventory + latestRefill.pillsPerRefill
        reminderDao.updateInventory(reminderId, newInventory)
    }
}
