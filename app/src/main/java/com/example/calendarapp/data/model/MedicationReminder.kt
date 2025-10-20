package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationName: String,
    val reminderTimes: List<LocalTime>, // Changed from single reminderTime to list of reminderTimes
    val frequency: Int, // Number of times to take the medication daily
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday, 7 = Sunday

    // Prescription tracking fields
    val dosagePerIntake: Int = 1,                    // Pills per dose (e.g., 1 or 2)
    val currentInventory: Int = 0,                   // Current pill count
    val inventoryTrackingEnabled: Boolean = false,   // Toggle prescription tracking
    val refillPeriodDays: Int = 30,                  // Days between refills (default 30)
    val prescriptionPillsPerRefill: Int = 60,        // Pills in each refill bottle
    val prescriptionTotalRefills: Int = 5            // Total refills authorized on prescription
)