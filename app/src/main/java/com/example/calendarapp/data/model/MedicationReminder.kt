package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationName: String,
    val reminderTime: LocalTime,
    val isMorning: Boolean,
    val dosage: Float? = null,
    val unit: String? = null,
    val frequency: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val reminderDays: String? = null  // Stored as "1,2,3,4,5,6,7" for all days of the week
)