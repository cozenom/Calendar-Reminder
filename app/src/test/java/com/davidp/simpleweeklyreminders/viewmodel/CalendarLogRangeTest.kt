package com.davidp.simpleweeklyreminders.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The Calendar tab's month grid and its selected-day list both slice one emission of this
 * range, so anything it fails to cover shows up as an empty day list or missing pips.
 * Dates are fixed (never "today") so results don't depend on when the tests run.
 */
class CalendarLogRangeTest {

    private val march = YearMonth.of(2026, 3)

    @Test
    fun `covers the displayed month`() {
        val range = calendarLogRange(march, LocalDate.of(2026, 3, 15))
        assertTrue(range.start <= march.atDay(1).atStartOfDay())
        assertTrue(range.end >= march.atEndOfMonth().atTime(23, 59))
    }

    @Test
    fun `pads one month either side for the pager's neighbouring pages`() {
        val range = calendarLogRange(march, LocalDate.of(2026, 3, 15))
        assertEquals(YearMonth.of(2026, 2).atDay(1).atStartOfDay(), range.start)
        assertEquals(YearMonth.of(2026, 4).atEndOfMonth().endOfDay(), range.end)
    }

    @Test
    fun `end is the last instant of the day, not midnight`() {
        val range = calendarLogRange(march, march.atDay(15))
        // A log at 23:59 on the final day must fall inside the inclusive BETWEEN
        assertTrue(range.end > YearMonth.of(2026, 4).atEndOfMonth().atTime(23, 59))
        assertEquals(YearMonth.of(2026, 4).atEndOfMonth(), range.end.toLocalDate())
    }

    @Test
    fun `selection inside the padding does not widen the range`() {
        val padded = calendarLogRange(march, LocalDate.of(2026, 3, 15))
        val selectedInNextMonth = calendarLogRange(march, LocalDate.of(2026, 4, 20))
        assertEquals(padded, selectedInNextMonth)
    }

    @Test
    fun `widens forward to reach a selection swiped away from`() {
        // Selection kept in August while the pager sits on March
        val range = calendarLogRange(march, LocalDate.of(2026, 8, 10))
        assertEquals(YearMonth.of(2026, 2).atDay(1).atStartOfDay(), range.start)
        assertEquals(YearMonth.of(2026, 8).atEndOfMonth().endOfDay(), range.end)
    }

    @Test
    fun `widens backward to reach a selection swiped away from`() {
        val range = calendarLogRange(march, LocalDate.of(2025, 10, 4))
        assertEquals(YearMonth.of(2025, 10).atDay(1).atStartOfDay(), range.start)
        assertEquals(YearMonth.of(2026, 4).atEndOfMonth().endOfDay(), range.end)
    }

    @Test
    fun `always contains the selected day`() {
        val cases = listOf(
            LocalDate.of(2026, 3, 15),  // same month
            LocalDate.of(2026, 4, 30),  // inside the padding
            LocalDate.of(2027, 1, 1),   // far ahead
            LocalDate.of(2024, 6, 30)   // far behind
        )
        for (selected in cases) {
            val range = calendarLogRange(march, selected)
            assertTrue("$selected not covered", range.start <= selected.atStartOfDay())
            assertTrue("$selected not covered", range.end >= selected.endOfDay())
        }
    }

    @Test
    fun `spans a year boundary without inverting`() {
        val january = YearMonth.of(2026, 1)
        val range = calendarLogRange(january, january.atDay(10))
        assertEquals(YearMonth.of(2025, 12).atDay(1).atStartOfDay(), range.start)
        assertEquals(YearMonth.of(2026, 2).atEndOfMonth().endOfDay(), range.end)
        assertTrue(range.start < range.end)
    }
}
