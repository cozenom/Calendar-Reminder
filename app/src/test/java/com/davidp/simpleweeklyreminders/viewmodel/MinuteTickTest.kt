package com.davidp.simpleweeklyreminders.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

/** The clock flow's sleep between ticks. An off-by-one at the boundary would spin or skip. */
class MinuteTickTest {

    private val minute: LocalDateTime = LocalDateTime.of(2026, 1, 5, 12, 30)

    @Test
    fun `mid-minute sleeps for the remainder`() {
        assertEquals(44_500L, millisUntilNextMinute(minute.plusSeconds(15).plusNanos(500_000_000)))
    }

    @Test
    fun `exactly on the minute sleeps a full minute`() {
        assertEquals(60_000L, millisUntilNextMinute(minute))
    }

    @Test
    fun `one millisecond before the boundary sleeps that millisecond`() {
        assertEquals(1L, millisUntilNextMinute(minute.plusSeconds(59).plusNanos(999_000_000)))
    }

    @Test
    fun `sub-millisecond remainders round to one, never zero`() {
        // toMillis() truncates; without the floor this would be a 0ms sleep and a busy loop
        assertEquals(1L, millisUntilNextMinute(minute.plusSeconds(59).plusNanos(999_999_000)))
    }
}
