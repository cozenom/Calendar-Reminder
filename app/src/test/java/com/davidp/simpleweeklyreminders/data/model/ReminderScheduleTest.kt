package com.davidp.simpleweeklyreminders.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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
        dayInterval: Int? = null,
        reminderType: ReminderType = if (dayInterval != null) ReminderType.EVERY_N_DAYS else ReminderType.SPECIFIC_DAYS
    ) = Reminder(
        title = "Test",
        reminderTimes = listOf(LocalTime.of(9, 0)),
        startDate = startDate,
        endDate = endDate,
        reminderDays = reminderDays,
        dayInterval = dayInterval,
        reminderType = reminderType
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

    // --- Every-N-days (dayInterval) mode ---

    @Test
    fun `interval of 3 hits every third day from start`() {
        val r = reminder(startDate = monday, dayInterval = 3)
        assertTrue(r.isScheduledOn(monday))
        assertFalse(r.isScheduledOn(monday.plusDays(1)))
        assertFalse(r.isScheduledOn(monday.plusDays(2)))
        assertTrue(r.isScheduledOn(monday.plusDays(3)))
        assertTrue(r.isScheduledOn(monday.plusDays(30)))
    }

    @Test
    fun `interval of 1 hits every day`() {
        val r = reminder(startDate = monday, dayInterval = 1)
        assertTrue(r.isScheduledOn(monday))
        assertTrue(r.isScheduledOn(monday.plusDays(1)))
        assertTrue(r.isScheduledOn(monday.plusDays(2)))
    }

    @Test
    fun `interval mode ignores selected weekdays`() {
        // Weekday set says Mondays only, but interval mode takes precedence
        val r = reminder(startDate = monday, reminderDays = setOf(1), dayInterval = 2)
        assertTrue(r.isScheduledOn(monday.plusDays(2))) // a Wednesday
    }

    @Test
    fun `interval respects start and end boundaries`() {
        val r = reminder(startDate = monday, endDate = monday.plusDays(6), dayInterval = 3)
        assertFalse(r.isScheduledOn(monday.minusDays(3)))
        assertTrue(r.isScheduledOn(monday.plusDays(6)))
        assertFalse(r.isScheduledOn(monday.plusDays(9)))
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
}
