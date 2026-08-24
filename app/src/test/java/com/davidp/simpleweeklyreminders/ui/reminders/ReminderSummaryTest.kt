package com.davidp.simpleweeklyreminders.ui.reminders

import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.ui.components.weekdayShortName
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The reminder row's subtitle copy. Pure string building, but it is the densest text in the
 * app — cadence, start, end, pause state and next fire all in one line.
 *
 * Patterns are passed in explicitly (the app derives them from the user's settings), so these
 * fix them rather than depending on a locale. Day *names* still come from the locale via
 * [weekdayShortName], so the one test that needs them builds its expectation the same way
 * instead of hardcoding English.
 *
 * Note [scheduleSummary] reads `LocalDate.now()` internally for the "starts" qualifier, so
 * the cases that exercise it use dates relative to today rather than fixed ones.
 */
class ReminderSummaryTest {

    private val timePattern = "HH:mm"
    private val datePattern = "MMM d, yyyy"
    private val dateNoYearPattern = "MMM d"

    // 2026-01-05 is a Monday
    private val monday: LocalDate = LocalDate.of(2026, 1, 5)

    private fun reminder(
        reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
        reminderType: ReminderType = ReminderType.SPECIFIC_DAYS,
        dayInterval: Int? = null,
        startDate: LocalDate = monday,
        endDate: LocalDate? = null,
        times: List<LocalTime> = listOf(LocalTime.of(9, 0)),
        isActive: Boolean = true
    ) = Reminder(
        title = "Test",
        reminderTimes = times,
        startDate = startDate,
        endDate = endDate,
        reminderDays = reminderDays,
        dayInterval = dayInterval,
        reminderType = reminderType,
        isActive = isActive,
        importance = Importance.MEDIUM
    )

    private fun summaryOf(r: Reminder) = scheduleSummary(r, datePattern, dateNoYearPattern)

    // --- scheduleSummary: cadence ---

    @Test
    fun `every n days reads as every day when the interval is one`() {
        assertEquals("Every day", summaryOf(reminder(reminderType = ReminderType.EVERY_N_DAYS, dayInterval = 1)))
    }

    @Test
    fun `every n days names the interval`() {
        assertEquals("Every 3 days", summaryOf(reminder(reminderType = ReminderType.EVERY_N_DAYS, dayInterval = 3)))
    }

    @Test
    fun `a one-time reminder says so`() {
        assertEquals("One-time", summaryOf(reminder(reminderType = ReminderType.ONE_TIME)))
    }

    @Test
    fun `all seven weekdays collapse to every day`() {
        assertEquals("Every day", summaryOf(reminder(reminderDays = setOf(1, 2, 3, 4, 5, 6, 7))))
    }

    @Test
    fun `monday to friday collapses to weekdays`() {
        assertEquals("Weekdays", summaryOf(reminder(reminderDays = setOf(1, 2, 3, 4, 5))))
    }

    @Test
    fun `saturday and sunday collapse to weekends`() {
        assertEquals("Weekends", summaryOf(reminder(reminderDays = setOf(6, 7))))
    }

    @Test
    fun `an arbitrary day set is listed in weekday order`() {
        // Set iteration order must not leak through — Fri/Mon/Wed has to come out Mon, Wed, Fri
        val expected = listOf(1, 3, 5).joinToString(", ") { weekdayShortName(it) }
        assertEquals(expected, summaryOf(reminder(reminderDays = setOf(5, 1, 3))))
    }

    // --- scheduleSummary: qualifiers ---

    @Test
    fun `a future start date is called out`() {
        val start = LocalDate.now().plusDays(10)
        val expected = "Every day · starts ${start.format(DateTimeFormatter.ofPattern(dateNoYearPattern))}"
        assertEquals(expected, summaryOf(reminder(startDate = start)))
    }

    @Test
    fun `a start date already past is not mentioned`() {
        assertEquals("Every day", summaryOf(reminder(startDate = LocalDate.now().minusDays(1))))
    }

    @Test
    fun `an end date is called out with its year`() {
        val end = LocalDate.now().plusDays(30)
        val expected = "Every day · until ${end.format(DateTimeFormatter.ofPattern(datePattern))}"
        assertEquals(expected, summaryOf(reminder(startDate = LocalDate.now(), endDate = end)))
    }

    @Test
    fun `start comes before end when both apply`() {
        val start = LocalDate.now().plusDays(5)
        val end = LocalDate.now().plusDays(40)
        val expected = "Every day" +
            " · starts ${start.format(DateTimeFormatter.ofPattern(dateNoYearPattern))}" +
            " · until ${end.format(DateTimeFormatter.ofPattern(datePattern))}"
        assertEquals(expected, summaryOf(reminder(startDate = start, endDate = end)))
    }

    // --- formatNextCompact ---

    private val now: LocalDateTime = LocalDateTime.of(monday, LocalTime.of(8, 0))

    private fun compact(target: LocalDateTime) =
        formatNextCompact(target, now, timePattern, dateNoYearPattern)

    @Test
    fun `later the same day reads as today`() {
        assertEquals("today 22:00", compact(LocalDateTime.of(monday, LocalTime.of(22, 0))))
    }

    @Test
    fun `the next day reads as tomorrow`() {
        assertEquals("tomorrow 08:30", compact(LocalDateTime.of(monday.plusDays(1), LocalTime.of(8, 30))))
    }

    @Test
    fun `within the week reads as a weekday name`() {
        val friday = monday.plusDays(4)
        val expected = "${friday.format(DateTimeFormatter.ofPattern("EEE"))} 09:00"
        assertEquals(expected, compact(LocalDateTime.of(friday, LocalTime.of(9, 0))))
    }

    @Test
    fun `a week out or more falls back to the date`() {
        // Exactly 7 days away is already "not this week" — the weekday name would be ambiguous
        val target = monday.plusDays(7)
        val expected = "${target.format(DateTimeFormatter.ofPattern(dateNoYearPattern))} 09:00"
        assertEquals(expected, compact(LocalDateTime.of(target, LocalTime.of(9, 0))))
    }

    @Test
    fun `earlier today still counts as today`() {
        // daysAway is date-based, so a time already passed today is not treated as the past
        assertEquals("today 07:00", compact(LocalDateTime.of(monday, LocalTime.of(7, 0))))
    }

    // --- rowSubtitle ---

    @Test
    fun `a paused reminder says paused instead of a next fire`() {
        val r = reminder(startDate = LocalDate.now().minusDays(1), isActive = false)
        assertEquals("Every day · paused", rowSubtitle(r, now, timePattern, datePattern, dateNoYearPattern))
    }

    @Test
    fun `an active reminder appends its next occurrence`() {
        val r = reminder(startDate = monday, times = listOf(LocalTime.of(22, 0)))
        assertEquals(
            "Every day · today 22:00",
            rowSubtitle(r, now, timePattern, datePattern, dateNoYearPattern)
        )
    }

    @Test
    fun `a reminder with no upcoming occurrence shows the cadence alone`() {
        // Ended yesterday: nextOccurrence returns null, so there is nothing to append
        val ended = reminder(
            startDate = LocalDate.now().minusDays(30),
            endDate = LocalDate.now().minusDays(1)
        )
        val expected = summaryOf(ended)
        assertEquals(expected, rowSubtitle(ended, LocalDateTime.now(), timePattern, datePattern, dateNoYearPattern))
    }
}
