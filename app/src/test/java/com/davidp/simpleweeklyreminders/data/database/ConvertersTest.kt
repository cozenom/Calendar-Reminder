package com.davidp.simpleweeklyreminders.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Round-trip tests for the Room type converters. A regression here corrupts
 * or loses users' stored reminders, so every converter must survive
 * value -> String -> value unchanged, including empty and null cases.
 */
class ConvertersTest {

    private val converters = Converters()

    // --- LocalDateTime ---

    @Test
    fun `localDateTime round trips`() {
        val value = LocalDateTime.of(2026, 7, 17, 9, 30)
        assertEquals(value, converters.fromLocalDateTime(converters.toLocalDateTime(value)))
    }

    @Test
    fun `localDateTime with seconds round trips`() {
        val value = LocalDateTime.of(2026, 12, 31, 23, 59, 59)
        assertEquals(value, converters.fromLocalDateTime(converters.toLocalDateTime(value)))
    }

    @Test
    fun `null localDateTime stays null`() {
        assertNull(converters.toLocalDateTime(null))
        assertNull(converters.fromLocalDateTime(null))
    }

    // --- LocalDate ---

    @Test
    fun `localDate round trips`() {
        val value = LocalDate.of(2026, 2, 28)
        assertEquals(value, converters.fromLocalDate(converters.toLocalDate(value)))
    }

    @Test
    fun `null localDate stays null`() {
        assertNull(converters.toLocalDate(null))
        assertNull(converters.fromLocalDate(null))
    }

    // --- Set<Int> (reminder weekdays) ---

    @Test
    fun `weekday set round trips`() {
        val value = setOf(1, 3, 5, 7)
        assertEquals(value, converters.toSetInt(converters.fromSetInt(value)))
    }

    @Test
    fun `empty weekday set round trips as empty`() {
        assertEquals(emptySet<Int>(), converters.toSetInt(converters.fromSetInt(emptySet())))
    }

    @Test
    fun `null weekday string becomes empty set`() {
        assertEquals(emptySet<Int>(), converters.toSetInt(null))
    }

    // --- List<LocalTime> (reminder times) ---

    @Test
    fun `time list round trips preserving order`() {
        val value = listOf(LocalTime.of(9, 0), LocalTime.of(12, 30), LocalTime.of(21, 15))
        assertEquals(value, converters.toListLocalTime(converters.fromListLocalTime(value)))
    }

    @Test
    fun `single time round trips`() {
        val value = listOf(LocalTime.of(0, 0)) // midnight edge case
        assertEquals(value, converters.toListLocalTime(converters.fromListLocalTime(value)))
    }

    @Test
    fun `empty time list round trips as empty`() {
        assertEquals(
            emptyList<LocalTime>(),
            converters.toListLocalTime(converters.fromListLocalTime(emptyList()))
        )
    }

    @Test
    fun `null time string becomes empty list`() {
        assertEquals(emptyList<LocalTime>(), converters.toListLocalTime(null))
    }
}
