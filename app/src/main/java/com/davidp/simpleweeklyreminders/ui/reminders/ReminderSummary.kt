package com.davidp.simpleweeklyreminders.ui.reminders

import com.davidp.simpleweeklyreminders.data.model.IntervalUnit
import com.davidp.simpleweeklyreminders.data.model.MonthlyMode
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.nthWeekdayOfMonth
import com.davidp.simpleweeklyreminders.ui.components.weekdayShortName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Human-readable recurrence line, e.g. "Mon, Wed, Fri", "Every 2 days", "Every month on the 15th".
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
        ReminderType.INTERVAL -> {
            val n = reminder.interval ?: 1
            when (reminder.intervalUnit) {
                IntervalUnit.DAYS -> every(n, "day")
                IntervalUnit.WEEKS -> every(n, "week") + " on " +
                    reminder.reminderDays.sorted().joinToString(", ") { weekdayShortName(it) }
                IntervalUnit.MONTHS -> every(n, "month") + " on " +
                    monthlyDayLabel(reminder.startDate, reminder.monthlyMode)
                IntervalUnit.YEARS -> every(n, "year") + " on " +
                    reminder.startDate.format(DateTimeFormatter.ofPattern(dateNoYearPattern))
            }
        }
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

/** "Every day" / "Every 3 days" */
private fun every(n: Int, unit: String): String = if (n == 1) "Every $unit" else "Every $n ${unit}s"

/** Which day a monthly reminder lands on, from its start date: "the 15th", "the 3rd Mon", "the last Fri". */
fun monthlyDayLabel(startDate: LocalDate, mode: MonthlyMode): String = when (mode) {
    MonthlyMode.DAY_OF_MONTH -> "the ${ordinal(startDate.dayOfMonth)}"
    MonthlyMode.NTH_WEEKDAY -> {
        val nth = startDate.nthWeekdayOfMonth()
        // Matches isScheduledOn: a 5th weekday means "last"
        "the ${if (nth == 5) "last" else ordinal(nth)} ${weekdayShortName(startDate.dayOfWeek.value)}"
    }
}

private fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
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
