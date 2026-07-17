package com.davidp.simpleweeklyreminders.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class ReminderType { SPECIFIC_DAYS, EVERY_N_DAYS, ONE_TIME }

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val reminderTimes: List<LocalTime>,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday, 7 = Sunday
    val notes: String? = null,
    val color: String? = null,
    val completedColor: String? = null,
    val icon: String? = null,
    val dayInterval: Int? = null,        // interval count, only meaningful when reminderType == EVERY_N_DAYS
    val reminderType: ReminderType = ReminderType.SPECIFIC_DAYS,
    val isActive: Boolean = true,        // false = paused, skipped in scheduling
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val sortOrder: Int = 0               // user-defined drag order, fallback = createdAt
)

/** Whether this reminder's schedule includes the given date (ignores isActive). */
fun Reminder.isScheduledOn(date: LocalDate): Boolean {
    if (date < startDate) return false
    endDate?.let { if (date > it) return false }
    return when (reminderType) {
        ReminderType.EVERY_N_DAYS -> ChronoUnit.DAYS.between(startDate, date) % (dayInterval ?: 1) == 0L
        ReminderType.SPECIFIC_DAYS, ReminderType.ONE_TIME -> reminderDays.contains(date.dayOfWeek.value)
    }
}

/** True once this reminder's schedule has fully elapsed — the day after endDate, calendar-date based. */
val Reminder.isArchived: Boolean get() = endDate != null && endDate < LocalDate.now()
