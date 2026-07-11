package com.davidp.simpleweeklyreminders.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
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
    val color: String? = null,
    val completedColor: String? = null,
    val icon: String? = null,
    val dayInterval: Int? = null,        // null = specific weekdays; set to repeat every N days
    val isActive: Boolean = true,        // false = paused, skipped in scheduling
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val sortOrder: Int = 0               // user-defined drag order, fallback = createdAt
)
