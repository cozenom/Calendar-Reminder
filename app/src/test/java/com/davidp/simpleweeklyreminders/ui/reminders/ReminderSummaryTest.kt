package com.davidp.simpleweeklyreminders.ui.reminders

import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.ui.components.weekdayShortName
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The reminder row's subtitle copy. Pure string building, but it is the densest text in the
 * app — cadence, start, end and times all in one line.
 *
 * Patterns are passed in explicitly (the app derives them from the user's settings), so these
 * fix them rather than depending on a locale. Day *names* still come from the locale via
 * [weekdayShortName], so the one test that needs them builds its expectation the same way
 * instead of hardcoding English. `today` is passed in too, so every date here is fixed.
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

    private fun summaryOf(r: Reminder, today: LocalDate = monday) =
        scheduleSummary(r, datePattern, dateNoYearPattern, today)

    private fun subtitleOf(r: Reminder, today: LocalDate = monday) =
        rowSubtitle(r, timePattern, datePattern, dateNoYearPattern, today)

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
        val start = monday.plusDays(10)
        val expected = "Every day · starts ${start.format(DateTimeFormatter.ofPattern(dateNoYearPattern))}"
        assertEquals(expected, summaryOf(reminder(startDate = start)))
    }

    @Test
    fun `a start date already past is not mentioned`() {
        assertEquals("Every day", summaryOf(reminder(startDate = monday.minusDays(1))))
    }

    @Test
    fun `a start date of today is not mentioned`() {
        assertEquals("Every day", summaryOf(reminder(startDate = monday)))
    }

    @Test
    fun `an end date is called out with its year`() {
        val end = monday.plusDays(30)
        val expected = "Every day · until ${end.format(DateTimeFormatter.ofPattern(datePattern))}"
        assertEquals(expected, summaryOf(reminder(startDate = monday, endDate = end)))
    }

    @Test
    fun `start comes before end when both apply`() {
        val start = monday.plusDays(5)
        val end = monday.plusDays(40)
        val expected = "Every day" +
            " · starts ${start.format(DateTimeFormatter.ofPattern(dateNoYearPattern))}" +
            " · until ${end.format(DateTimeFormatter.ofPattern(datePattern))}"
        assertEquals(expected, summaryOf(reminder(startDate = start, endDate = end)))
    }

    // --- rowSubtitle ---

    @Test
    fun `appends the times in clock order`() {
        val r = reminder(times = listOf(LocalTime.of(22, 0), LocalTime.of(8, 0)))
        assertEquals("Every day · 08:00, 22:00", subtitleOf(r))
    }

    @Test
    fun `a duplicate time is listed once`() {
        // Pre-dedupe rows can still hold the same time twice
        val r = reminder(times = listOf(LocalTime.of(9, 0), LocalTime.of(9, 0)))
        assertEquals("Every day · 09:00", subtitleOf(r))
    }

    @Test
    fun `no times leaves the cadence alone`() {
        assertEquals("Every day", subtitleOf(reminder(times = emptyList())))
    }

    @Test
    fun `paused reads the same as active`() {
        // The switch and dimming carry the paused state; the text doesn't repeat it
        assertEquals(subtitleOf(reminder()), subtitleOf(reminder(isActive = false)))
    }

    @Test
    fun `qualifiers sit between cadence and times`() {
        val end = monday.plusDays(30)
        val expected = "Every day · until ${end.format(DateTimeFormatter.ofPattern(datePattern))} · 09:00"
        assertEquals(expected, subtitleOf(reminder(endDate = end)))
    }
}
