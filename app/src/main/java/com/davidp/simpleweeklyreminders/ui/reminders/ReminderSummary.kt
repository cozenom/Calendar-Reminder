package com.davidp.simpleweeklyreminders.ui.reminders

import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.ui.components.weekdayShortName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Human-readable recurrence line, e.g. "Mon, Wed, Fri", "Every 2 days", "Weekdays".
 * [today] is passed in rather than read here, so the caller owns the clock (and tests can
 * pin it).
 */
fun scheduleSummary(
    reminder: Reminder,
    datePattern: String,
    dateNoYearPattern: String,
    today: LocalDate
): String {
    val base = when (reminder.reminderType) {
        ReminderType.EVERY_N_DAYS ->
            if (reminder.dayInterval == 1) "Every day" else "Every ${reminder.dayInterval} days"
        ReminderType.ONE_TIME -> "One-time"
        ReminderType.SPECIFIC_DAYS -> when {
            reminder.reminderDays.size == 7 -> "Every day"
            reminder.reminderDays == setOf(1, 2, 3, 4, 5) -> "Weekdays"
            reminder.reminderDays == setOf(6, 7) -> "Weekends"
            else -> reminder.reminderDays.sorted().joinToString(", ") { weekdayShortName(it) }
        }
    }

    val qualifiers = buildList {
        if (reminder.startDate > today) {
            add("starts ${reminder.startDate.format(DateTimeFormatter.ofPattern(dateNoYearPattern))}")
        }
        reminder.endDate?.let { add("until ${it.format(DateTimeFormatter.ofPattern(datePattern))}") }
    }
    return (listOf(base) + qualifiers).joinToString(" · ")
}

/**
 * The reminder row's one-line subtitle: cadence, then its times —
 * "Every day · 08:00, 22:00", "Mon, Wed, Fri · 09:00".
 *
 * Describes the schedule as set up, not today's progress: that lives on the Calendar tab.
 * Paused isn't spelled out either — the row's switch and dimming already say it.
 */
fun rowSubtitle(
    reminder: Reminder,
    timePattern: String,
    datePattern: String,
    dateNoYearPattern: String,
    today: LocalDate
): String {
    val cadence = scheduleSummary(reminder, datePattern, dateNoYearPattern, today)
    val formatter = DateTimeFormatter.ofPattern(timePattern)
    val times = reminder.reminderTimes.distinct().sorted().joinToString(", ") { it.format(formatter) }
    return if (times.isEmpty()) cadence else "$cadence · $times"
}
