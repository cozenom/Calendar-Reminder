package com.example.calendarapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val reminderTimes: List<LocalTime>,
    val frequency: Int,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday, 7 = Sunday
    val notes: String? = null,
    val color: String? = null,           // Hex color for pending/active state
    val completedColor: String? = null,  // Hex color for completed state
    val icon: String? = null             // Icon identifier (e.g. "pill", "plant", "water")
)
