package com.davidp.simpleweeklyreminders.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Regression tests for [isScheduledOn], the single source of truth for whether
 * a reminder occurs on a given date. Dates are fixed (never "today") so results
 * don't depend on when the tests run.
 */
class ReminderScheduleTest {

    // 2026-01-05 is a Monday
    private val monday: LocalDate = LocalDate.of(2026, 1, 5)
    private val tuesday: LocalDate = monday.plusDays(1)
    private val sunday: LocalDate = monday.plusDays(6)

    private fun reminder(
        startDate: LocalDate = monday,
        endDate: LocalDate? = null,
        reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
        interval: Int? = null,
        reminderType: ReminderType = if (interval != null) ReminderType.INTERVAL else ReminderType.SPECIFIC_DAYS,
        intervalUnit: IntervalUnit = IntervalUnit.DAYS,
        monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
        archivedAt: LocalDateTime? = null
    ) = Reminder(
        title = "Test",
        reminderTimes = listOf(LocalTime.of(9, 0)),
        startDate = startDate,
        endDate = endDate,
        reminderDays = reminderDays,
        interval = interval,
        reminderType = reminderType,
        intervalUnit = intervalUnit,
        monthlyMode = monthlyMode,
        archivedAt = archivedAt
    )

    // --- Weekly (specific weekdays) mode ---

    @Test
    fun `scheduled on a selected weekday`() {
        val r = reminder(reminderDays = setOf(1)) // Mondays only
        assertTrue(r.isScheduledOn(monday))
        assertTrue(r.isScheduledOn(monday.plusWeeks(1)))
    }

    @Test
    fun `not scheduled on an unselected weekday`() {
        val r = reminder(reminderDays = setOf(1)) // Mondays only
        assertFalse(r.isScheduledOn(tuesday))
        assertFalse(r.isScheduledOn(sunday))
    }

    @Test
    fun `sunday maps to 7 not 0`() {
        val r = reminder(reminderDays = setOf(7))
        assertTrue(r.isScheduledOn(sunday))
        assertFalse(r.isScheduledOn(monday))
    }

    // --- Start and end date boundaries ---

    @Test
    fun `not scheduled before start date`() {
        val r = reminder(startDate = monday)
        assertFalse(r.isScheduledOn(monday.minusDays(1)))
    }

    @Test
    fun `scheduled on the start date itself`() {
        val r = reminder(startDate = monday)
        assertTrue(r.isScheduledOn(monday))
    }

    @Test
    fun `scheduled on the end date itself`() {
        val r = reminder(startDate = monday, endDate = sunday)
        assertTrue(r.isScheduledOn(sunday))
    }

    @Test
    fun `not scheduled after end date`() {
        val r = reminder(startDate = monday, endDate = sunday)
        assertFalse(r.isScheduledOn(sunday.plusDays(1)))
    }

    @Test
    fun `no end date means it runs indefinitely`() {
        val r = reminder(startDate = monday, endDate = null)
        assertTrue(r.isScheduledOn(monday.plusYears(10)))
    }

    // --- Every-N-days (interval) mode ---

    @Test
    fun `interval of 3 hits every third day from start`() {
        val r = reminder(startDate = monday, interval = 3)
        assertTrue(r.isScheduledOn(monday))
        assertFalse(r.isScheduledOn(monday.plusDays(1)))
        assertFalse(r.isScheduledOn(monday.plusDays(2)))
        assertTrue(r.isScheduledOn(monday.plusDays(3)))
        assertTrue(r.isScheduledOn(monday.plusDays(30)))
    }

    @Test
    fun `interval of 1 hits every day`() {
        val r = reminder(startDate = monday, interval = 1)
        assertTrue(r.isScheduledOn(monday))
        assertTrue(r.isScheduledOn(monday.plusDays(1)))
        assertTrue(r.isScheduledOn(monday.plusDays(2)))
    }

    @Test
    fun `interval mode ignores selected weekdays`() {
        // Weekday set says Mondays only, but interval mode takes precedence
        val r = reminder(startDate = monday, reminderDays = setOf(1), interval = 2)
        assertTrue(r.isScheduledOn(monday.plusDays(2))) // a Wednesday
    }

    @Test
    fun `interval respects start and end boundaries`() {
        val r = reminder(startDate = monday, endDate = monday.plusDays(6), interval = 3)
        assertFalse(r.isScheduledOn(monday.minusDays(3)))
        assertTrue(r.isScheduledOn(monday.plusDays(6)))
        assertFalse(r.isScheduledOn(monday.plusDays(9)))
    }

    // --- Every N weeks ---

    @Test
    fun `every 2 weeks fires on the chosen days of every other week`() {
        val r = reminder(interval = 2, intervalUnit = IntervalUnit.WEEKS, reminderDays = setOf(1, 3))
        assertTrue(r.isScheduledOn(monday))
        assertTrue(r.isScheduledOn(monday.plusDays(2))) // Wednesday
        assertFalse(r.isScheduledOn(tuesday))
        assertFalse(r.isScheduledOn(monday.plusWeeks(1))) // the "off" week
        assertTrue(r.isScheduledOn(monday.plusWeeks(2)))
        assertTrue(r.isScheduledOn(monday.plusWeeks(2).plusDays(2)))
    }

    @Test
    fun `every 2 weeks counts from the start's week, not the start day`() {
        // Starts on a Wednesday: that whole week is week 0, so the next Monday is "off"
        val wednesday = monday.plusDays(2)
        val r = reminder(
            startDate = wednesday, interval = 2, intervalUnit = IntervalUnit.WEEKS, reminderDays = setOf(1, 3)
        )
        assertTrue(r.isScheduledOn(wednesday))
        assertFalse(r.isScheduledOn(monday.plusWeeks(1)))
        assertTrue(r.isScheduledOn(monday.plusWeeks(2)))
    }

    // --- Every N months ---

    @Test
    fun `monthly on the 31st falls back to the last day of shorter months`() {
        val r = reminder(startDate = LocalDate.of(2026, 1, 31), interval = 1, intervalUnit = IntervalUnit.MONTHS)
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 2, 28)))
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 4, 30)))
        // Each month clamps from the start date, so March is back on the 31st, not the 28th
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 3, 31)))
        assertFalse(r.isScheduledOn(LocalDate.of(2026, 3, 28)))
    }

    @Test
    fun `every 2 months skips the month between`() {
        val r = reminder(startDate = LocalDate.of(2026, 1, 15), interval = 2, intervalUnit = IntervalUnit.MONTHS)
        assertFalse(r.isScheduledOn(LocalDate.of(2026, 2, 15)))
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `monthly 3rd monday follows the weekday, not the date`() {
        // 2026-01-19 is the 3rd Monday of January
        val r = reminder(
            startDate = LocalDate.of(2026, 1, 19), interval = 1,
            intervalUnit = IntervalUnit.MONTHS, monthlyMode = MonthlyMode.NTH_WEEKDAY
        )
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 2, 16)))
        assertFalse(r.isScheduledOn(LocalDate.of(2026, 2, 19)))
        assertFalse(r.isScheduledOn(LocalDate.of(2026, 2, 23)))
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 3, 16)))
    }

    @Test
    fun `a 5th monday start means the last monday of each month`() {
        // 2026-03-30 is the 5th Monday of March; April only has four
        val r = reminder(
            startDate = LocalDate.of(2026, 3, 30), interval = 1,
            intervalUnit = IntervalUnit.MONTHS, monthlyMode = MonthlyMode.NTH_WEEKDAY
        )
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 4, 27)))
        assertFalse(r.isScheduledOn(LocalDate.of(2026, 4, 20)))
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 6, 29))) // a 5th Monday again
    }

    @Test
    fun `a 4th monday that is also the last stays the 4th`() {
        // 2026-01-26 is both the 4th and last Monday; March has a 5th (the 30th)
        val r = reminder(
            startDate = LocalDate.of(2026, 1, 26), interval = 1,
            intervalUnit = IntervalUnit.MONTHS, monthlyMode = MonthlyMode.NTH_WEEKDAY
        )
        assertTrue(r.isScheduledOn(LocalDate.of(2026, 3, 23)))
        assertFalse(r.isScheduledOn(LocalDate.of(2026, 3, 30)))
    }

    // --- Every N years ---

    @Test
    fun `yearly on feb 29 falls back to feb 28 in non-leap years`() {
        val r = reminder(startDate = LocalDate.of(2028, 2, 29), interval = 1, intervalUnit = IntervalUnit.YEARS)
        assertTrue(r.isScheduledOn(LocalDate.of(2029, 2, 28)))
        assertFalse(r.isScheduledOn(LocalDate.of(2029, 3, 1)))
        assertTrue(r.isScheduledOn(LocalDate.of(2032, 2, 29)))
        assertFalse(r.isScheduledOn(LocalDate.of(2032, 2, 28)))
    }

    @Test
    fun `every 2 years skips the year between`() {
        val r = reminder(interval = 2, intervalUnit = IntervalUnit.YEARS)
        assertFalse(r.isScheduledOn(monday.plusYears(1)))
        assertTrue(r.isScheduledOn(monday.plusYears(2)))
    }

    @Test
    fun `next occurrence reaches past a year for long intervals`() {
        // The look-ahead used to stop at one year, so this read as "no next occurrence"
        val r = reminder(interval = 2, intervalUnit = IntervalUnit.YEARS)
        assertEquals(
            monday.plusYears(2).atTime(9, 0),
            nextOccurrence(r, now = monday.plusDays(1).atStartOfDay())
        )
    }

    // --- One-time (single date) mode ---

    @Test
    fun `one-time reminder fires only on its single date`() {
        val r = reminder(
            startDate = monday,
            endDate = monday,
            reminderDays = setOf(1),
            reminderType = ReminderType.ONE_TIME
        )
        assertTrue(r.isScheduledOn(monday))
        assertFalse(r.isScheduledOn(monday.plusWeeks(1)))
        assertFalse(r.isScheduledOn(monday.minusDays(1)))
    }

    // --- coversDate (date range only, ignores cadence) ---

    @Test
    fun `coversDate ignores the weekday cadence`() {
        // Mondays only, but Tuesday still falls inside the reminder's date range —
        // a log on that day needs its icon and importance either way
        val r = reminder(startDate = monday, reminderDays = setOf(1))
        assertFalse(r.isScheduledOn(tuesday))
        assertTrue(r.coversDate(tuesday))
    }

    @Test
    fun `coversDate ignores the every-N-days cadence`() {
        val r = reminder(startDate = monday, interval = 3)
        assertFalse(r.isScheduledOn(tuesday))
        assertTrue(r.coversDate(tuesday))
    }

    @Test
    fun `coversDate includes both boundary dates`() {
        val r = reminder(startDate = monday, endDate = sunday)
        assertTrue(r.coversDate(monday))
        assertTrue(r.coversDate(sunday))
    }

    @Test
    fun `coversDate excludes days outside the range`() {
        val r = reminder(startDate = monday, endDate = sunday)
        assertFalse(r.coversDate(monday.minusDays(1)))
        assertFalse(r.coversDate(sunday.plusDays(1)))
    }

    @Test
    fun `coversDate with no end date runs indefinitely`() {
        val r = reminder(startDate = monday, endDate = null)
        assertTrue(r.coversDate(monday.plusYears(10)))
    }

    @Test
    fun `coversDate still covers an archived reminder's own past days`() {
        // The calendar looks up icon/importance for old logs whose reminder has lapsed
        val r = reminder(startDate = monday, endDate = monday.plusDays(2))
        assertTrue(r.isArchived(today = monday.plusDays(10)))
        assertTrue(r.coversDate(monday.plusDays(1)))
    }

    // --- isArchived ---

    @Test
    fun `no end date is never archived`() {
        val r = reminder(endDate = null)
        assertFalse(r.isArchived(today = monday))
    }

    @Test
    fun `future end date is not archived`() {
        val r = reminder(endDate = monday.plusDays(1))
        assertFalse(r.isArchived(today = monday))
    }

    @Test
    fun `end date of today is not yet archived`() {
        val r = reminder(endDate = monday)
        assertFalse(r.isArchived(today = monday))
    }

    @Test
    fun `past end date is archived`() {
        val r = reminder(endDate = monday.minusDays(1))
        assertTrue(r.isArchived(today = monday))
    }

    @Test
    fun `archivedAt alone archives a reminder with no end date`() {
        // Manual archive no longer back-dates endDate, so archivedAt must be enough on its own
        val r = reminder(endDate = null, archivedAt = monday.atTime(9, 0))
        assertTrue(r.isArchived(today = monday))
    }

    @Test
    fun `archivedAt archives even while the end date is still ahead`() {
        // The user's future end date survives an archive, and mustn't read as "still running"
        val r = reminder(endDate = monday.plusDays(30), archivedAt = monday.atTime(9, 0))
        assertTrue(r.isArchived(today = monday))
    }

    // --- hasLapsed (drives Restore's "pick a new end date") ---

    @Test
    fun `hasLapsed only once the end date is behind today`() {
        assertFalse(reminder(endDate = null).hasLapsed(today = monday))
        assertFalse(reminder(endDate = monday).hasLapsed(today = monday))
        assertTrue(reminder(endDate = monday.minusDays(1)).hasLapsed(today = monday))
    }

    @Test
    fun `a manual archive with a future end date has not lapsed`() {
        // Restores straight back with its own end date, no picker
        val r = reminder(endDate = monday.plusDays(30), archivedAt = monday.atTime(9, 0))
        assertFalse(r.hasLapsed(today = monday))
    }

    // --- archivedSince ---

    @Test
    fun `not archived means no archivedSince`() {
        val r = reminder(endDate = null)
        assertNull(r.archivedSince(today = monday))
    }

    @Test
    fun `auto-lapsed reminder falls back to day after endDate at midnight`() {
        val r = reminder(endDate = monday.minusDays(1)) // archived as of `monday`
        assertEquals(monday.atStartOfDay(), r.archivedSince(today = monday))
    }

    @Test
    fun `manually archived reminder prefers the precise archivedAt timestamp`() {
        val preciseInstant = monday.minusDays(1).atTime(14, 30)
        val r = reminder(endDate = monday.minusDays(1), archivedAt = preciseInstant)
        assertEquals(preciseInstant, r.archivedSince(today = monday))
    }

    @Test
    fun `archivedAt counts even when the end date is still ahead`() {
        // Replaces "archivedAt on a not-yet-archived reminder is ignored" — archivedAt used to
        // be only a precision hint next to a back-dated endDate. It's now the manual-archive
        // marker itself; restore() clears it, so a stale one can't linger.
        val stamp = monday.atTime(9, 0)
        val r = reminder(endDate = monday.plusDays(1), archivedAt = stamp)
        assertEquals(stamp, r.archivedSince(today = monday))
    }

    @Test
    fun `manual archive with no end date still has an archivedSince to sort by`() {
        val stamp = monday.atTime(9, 0)
        val r = reminder(endDate = null, archivedAt = stamp)
        assertEquals(stamp, r.archivedSince(today = monday))
    }

    // --- nthOccurrenceDate ("End after N times") ---

    @Test
    fun `nth occurrence counts selected weekdays`() {
        val r = reminder(reminderDays = setOf(1, 3, 5)) // Mon/Wed/Fri
        assertEquals(monday, r.nthOccurrenceDate(1, monday))
        assertEquals(LocalDate.of(2026, 1, 9), r.nthOccurrenceDate(3, monday))
        assertEquals(LocalDate.of(2026, 1, 12), r.nthOccurrenceDate(4, monday))
    }

    @Test
    fun `nth occurrence counts from the given date, not the start`() {
        val r = reminder(reminderDays = setOf(1)) // Mondays, started this Monday
        assertEquals(LocalDate.of(2026, 1, 12), r.nthOccurrenceDate(1, tuesday))
    }

    @Test
    fun `nth occurrence ignores the current end date`() {
        val r = reminder(endDate = monday, reminderDays = setOf(1, 3, 5))
        assertEquals(LocalDate.of(2026, 1, 9), r.nthOccurrenceDate(3, monday))
    }

    @Test
    fun `nth occurrence every 2 weeks`() {
        val r = reminder(interval = 2, intervalUnit = IntervalUnit.WEEKS, reminderDays = setOf(1))
        assertEquals(LocalDate.of(2026, 2, 2), r.nthOccurrenceDate(3, monday)) // Jan 5, 19, Feb 2
    }

    @Test
    fun `nth occurrence monthly on the 31st clamps short months`() {
        val r = reminder(startDate = LocalDate.of(2026, 1, 31), interval = 1, intervalUnit = IntervalUnit.MONTHS)
        assertEquals(LocalDate.of(2026, 2, 28), r.nthOccurrenceDate(2, LocalDate.of(2026, 1, 31)))
        assertEquals(LocalDate.of(2026, 3, 31), r.nthOccurrenceDate(3, LocalDate.of(2026, 1, 31)))
    }

    @Test
    fun `nth occurrence yearly from Feb 29`() {
        val start = LocalDate.of(2028, 2, 29)
        val r = reminder(startDate = start, interval = 1, intervalUnit = IntervalUnit.YEARS)
        assertEquals(LocalDate.of(2029, 2, 28), r.nthOccurrenceDate(2, start))
    }

    @Test
    fun `nth occurrence is null with no days or a count below 1`() {
        assertNull(reminder(reminderDays = emptySet()).nthOccurrenceDate(1, monday))
        assertNull(reminder().nthOccurrenceDate(0, monday))
    }
}
