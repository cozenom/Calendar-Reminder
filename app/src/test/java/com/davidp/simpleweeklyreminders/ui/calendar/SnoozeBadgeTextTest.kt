package com.davidp.simpleweeklyreminders.ui.calendar

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

/** The calendar's snooze badge: weekday only when the snooze crosses into another day. */
class SnoozeBadgeTextTest {

    private val pattern = "h:mm a"
    private lateinit var savedLocale: Locale

    // Weekday and AM/PM names follow the default locale — pin it so the test is stable
    @Before fun pinLocale() { savedLocale = Locale.getDefault(); Locale.setDefault(Locale.US) }
    @After fun restoreLocale() = Locale.setDefault(savedLocale)

    @Test
    fun sameDaySnoozeShowsTimeOnly() {
        val at = LocalDateTime.of(2026, 11, 25, 9, 0)
        assertEquals("9:30 AM", snoozeBadgeText(at, at.plusMinutes(30), pattern))
    }

    @Test
    fun snoozePastMidnightShowsWeekday() {
        val at = LocalDateTime.of(2026, 11, 25, 23, 50) // Wednesday
        assertEquals("Thu 12:20 AM", snoozeBadgeText(at, at.plusMinutes(30), pattern))
    }
}
