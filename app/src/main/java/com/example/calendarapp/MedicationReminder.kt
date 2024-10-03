package com.example.calendarapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationName: String,
    val reminderTime: Long,
    val isMorning: Boolean
)