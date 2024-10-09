// MedicationReminder.kt
package com.example.calendarapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationName: String,
    val reminderTimes: List<LocalTime>,
    val frequency: Int,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val dosage: String = "",
    val notes: String = "",
    val refillDate: LocalDate? = null,
    val refillReminder: Boolean = false,
    val refillQuantity: Int? = null,
    val refillsRemaining: Int? = null,
    @ColumnInfo(name = "refill_notification_days")
    val refillNotificationDays: List<Int> = emptyList()
)

data class RefillInfo(
    val medicationName: String,
    val quantity: Int,
    val refillsRemaining: Int,
    val refillDate: LocalDate,
    val notificationDays: List<Int>,
    val refillHistory: List<RefillEvent> = emptyList()
)

data class RefillEvent(
    val date: LocalDate,
    val quantity: Int
)