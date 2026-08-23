package com.davidp.simpleweeklyreminders.ui.reminders

import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.nextOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val WEEKDAY_ABBREVIATIONS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Human-readable recurrence line, e.g. "Mon, Wed, Fri", "Every 2 days", "Weekdays". */
fun scheduleSummary(reminder: Reminder, datePattern: String, dateNoYearPattern: String): String {
    val base = when (reminder.reminderType) {
        ReminderType.EVERY_N_DAYS ->
            if (reminder.dayInterval == 1) "Every day" else "Every ${reminder.dayInterval} days"
        ReminderType.ONE_TIME -> "One-time"
        ReminderType.SPECIFIC_DAYS -> when {
            reminder.reminderDays.size == 7 -> "Every day"
            reminder.reminderDays == setOf(1, 2, 3, 4, 5) -> "Weekdays"
            reminder.reminderDays == setOf(6, 7) -> "Weekends"
            else -> reminder.reminderDays.sorted().joinToString(", ") { WEEKDAY_ABBREVIATIONS[it - 1] }
        }
    }

    val today = LocalDate.now()
    val qualifiers = buildList {
        if (reminder.startDate > today) {
            add("starts ${reminder.startDate.format(DateTimeFormatter.ofPattern(dateNoYearPattern))}")
        }
        reminder.endDate?.let { add("until ${it.format(DateTimeFormatter.ofPattern(datePattern))}") }
    }
    return (listOf(base) + qualifiers).joinToString(" · ")
}

/**
 * The next fire, compact enough to sit inside a row subtitle: "today 22:00", "tomorrow 08:30",
 * "Fri 09:00", "4 Sep 10:00".
 */
internal fun formatNextCompact(
    dateTime: LocalDateTime,
    now: LocalDateTime,
    timePattern: String,
    dateNoYearPattern: String
): String {
    val time = dateTime.format(DateTimeFormatter.ofPattern(timePattern))
    val daysAway = ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate())
    val day = when {
        daysAway == 0L -> "today"
        daysAway == 1L -> "tomorrow"
        daysAway < 7L -> dateTime.format(DateTimeFormatter.ofPattern("EEE"))
        else -> dateTime.format(DateTimeFormatter.ofPattern(dateNoYearPattern))
    }
    return "$day $time"
}

/**
 * The reminder row's one-line subtitle: cadence, then when it next fires —
 * "Every day · today 22:00", "Mon, Wed, Fri · Fri 09:00", "Every 3 days · paused".
 *
 * Replaces the old separate "Next: Tomorrow at 9:00 AM" line.
 */
fun rowSubtitle(
    reminder: Reminder,
    now: LocalDateTime,
    timePattern: String,
    datePattern: String,
    dateNoYearPattern: String
): String {
    val cadence = scheduleSummary(reminder, datePattern, dateNoYearPattern)
    if (!reminder.isActive) return "$cadence · paused"
    val next = nextOccurrence(reminder, now) ?: return cadence
    return "$cadence · ${formatNextCompact(next, now, timePattern, dateNoYearPattern)}"
}
