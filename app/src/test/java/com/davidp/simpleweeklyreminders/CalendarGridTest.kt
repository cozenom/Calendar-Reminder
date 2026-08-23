package com.davidp.simpleweeklyreminders

import com.davidp.simpleweeklyreminders.data.settings.WeekStart
import com.davidp.simpleweeklyreminders.ui.calendar.leadingBlankCount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Leading-blank math for the month grid — the off-by-one-prone part of week-start support. */
class CalendarGridTest {

    // firstDow is ISO: Mon=1 .. Sun=7
    @Test
    fun mondayStart_blanksAreDowMinusOne() {
        val expected = listOf(0, 1, 2, 3, 4, 5, 6) // Mon..Sun
        for (dow in 1..7) {
            assertEquals(expected[dow - 1], leadingBlankCount(dow, WeekStart.MONDAY))
        }
    }

    @Test
    fun sundayStart_sundayIsZeroThenShiftsUp() {
        // Mon=1 -> 1, Tue=2 -> 2, ... Sat=6 -> 6, Sun=7 -> 0
        val expected = listOf(1, 2, 3, 4, 5, 6, 0) // Mon..Sun
        for (dow in 1..7) {
            assertEquals(expected[dow - 1], leadingBlankCount(dow, WeekStart.SUNDAY))
        }
    }

    @Test
    fun realMonth_crossCheck() {
        // Aug 2026 starts on a Saturday (dow = 6).
        val firstDow = LocalDate.of(2026, 8, 1).dayOfWeek.value
        assertEquals(6, firstDow)
        assertEquals(5, leadingBlankCount(firstDow, WeekStart.MONDAY))
        assertEquals(6, leadingBlankCount(firstDow, WeekStart.SUNDAY))
    }
}
