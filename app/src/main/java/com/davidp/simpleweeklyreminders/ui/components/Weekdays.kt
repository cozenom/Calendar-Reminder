package com.davidp.simpleweeklyreminders.ui.components

import com.davidp.simpleweeklyreminders.data.settings.WeekStart
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * The one source of weekday names. Four hardcoded English lists used to live in
 * ReminderSummary, WeekdaySelector and CalendarView; every label now comes from java.time,
 * so they follow the device locale instead.
 *
 * ISO numbering throughout: Mon = 1 .. Sun = 7, matching `Reminder.reminderDays`.
 */
private fun dayOf(isoDay: Int) = DayOfWeek.of(isoDay)

/** "Monday" — accessibility labels, where the short forms repeat (T/T, S/S). */
fun weekdayFullName(isoDay: Int): String =
    dayOf(isoDay).getDisplayName(TextStyle.FULL, Locale.getDefault())

/** "Mon" — the recurrence summary line. */
fun weekdayShortName(isoDay: Int): String =
    dayOf(isoDay).getDisplayName(TextStyle.SHORT, Locale.getDefault())

/** "M" — the form's day circles, where the column is only one character wide. */
fun weekdayNarrowName(isoDay: Int): String =
    dayOf(isoDay).getDisplayName(TextStyle.NARROW, Locale.getDefault())

/**
 * "Mo" — the calendar grid header. java.time has no two-character style, so this trims the
 * short form: narrow would repeat (T/T, S/S) across seven adjacent columns with no
 * accessibility label to tell them apart, which the day circles can afford and this can't.
 */
fun weekdayMiniName(isoDay: Int): String = weekdayShortName(isoDay).take(2)

/** The seven ISO day numbers, ordered from the user's chosen first column. */
fun weekdayOrder(weekStart: WeekStart): List<Int> = when (weekStart) {
    WeekStart.MONDAY -> listOf(1, 2, 3, 4, 5, 6, 7)
    WeekStart.SUNDAY -> listOf(7, 1, 2, 3, 4, 5, 6)
}
