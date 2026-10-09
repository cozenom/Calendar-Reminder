package com.davidp.simpleweeklyreminders.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** asDuplicate() is the prefill for the "Duplicate" form; insert() relies on id = 0. */
class ReminderDuplicateTest {

    private val today = LocalDate.of(2026, 10, 9)
    private val now = LocalDateTime.of(2026, 10, 9, 12, 0)

    private val original = Reminder(
        id = 42,
        title = "Water plants",
        reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 12, 31),
        reminderDays = setOf(1, 3, 5),
        notes = "Balcony too",
        color = "moss",
        icon = "plant",
        interval = 2,
        reminderType = ReminderType.INTERVAL,
        intervalUnit = IntervalUnit.WEEKS,
        isActive = false,
        createdAt = LocalDateTime.of(2026, 1, 1, 9, 0),
        sortOrder = 7,
        archivedAt = LocalDateTime.of(2026, 9, 1, 9, 0),
        importance = Importance.LOW,
        sound = SILENT_SOUND,
        snoozeMinutes = 30
    )

    private val copy = original.asDuplicate(today, now)

    @Test
    fun `id resets so Room inserts a new row`() {
        assertEquals(0, copy.id)
    }

    @Test
    fun `starts today with a fresh createdAt`() {
        assertEquals(today, copy.startDate)
        assertEquals(now, copy.createdAt)
    }

    @Test
    fun `a paused or archived original comes back active`() {
        assertTrue(copy.isActive)
        assertNull(copy.archivedAt)
    }

    @Test
    fun `everything else is copied as is`() {
        val expected = original.copy(
            id = 0, startDate = today, isActive = true, archivedAt = null, createdAt = now
        )
        assertEquals(expected, copy)
    }
}
