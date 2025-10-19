package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Represents a prescription refill event.
 * Each row represents either:
 * - Initial prescription from doctor
 * - A refill pickup
 *
 * Tracks the refill authorization details and creates a history of pickups.
 */
@Entity(
    tableName = "prescription_refills",
    foreignKeys = [ForeignKey(
        entity = MedicationReminder::class,
        parentColumns = ["id"],
        childColumns = ["reminderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("reminderId")]
)
data class PrescriptionRefill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val pickupDate: LocalDate,
    val pillsPerRefill: Int,           // Pills in each refill (e.g., 60)
    val totalRefillsAuthorized: Int,   // Total refills on prescription (e.g., 11)
    val refillsRemaining: Int          // Refills left after this pickup
)
