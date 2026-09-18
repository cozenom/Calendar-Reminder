package com.davidp.simpleweeklyreminders.ui.archive

import com.davidp.simpleweeklyreminders.data.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** What the Archive badge and launch snackbar count as "new since you last checked". */
class NewlyArchivedCountTest {

    private val lastViewed: LocalDateTime = LocalDateTime.of(2026, 9, 10, 12, 0)
    private val today: LocalDate = LocalDate.of(2026, 9, 18)

    private fun reminder(endDate: LocalDate? = null, archivedAt: LocalDateTime? = null) = Reminder(
        title = "Test",
        reminderTimes = listOf(LocalTime.of(9, 0)),
        startDate = today.minusDays(60),
        endDate = endDate,
        archivedAt = archivedAt
    )

    private fun count(vararg reminders: Reminder) = newlyArchivedCount(reminders.toList(), lastViewed, today)

    @Test
    fun `lapsed after last view counts`() {
        assertEquals(1, count(reminder(endDate = lastViewed.toLocalDate().plusDays(2))))
    }

    @Test
    fun `lapsed before last view does not count`() {
        assertEquals(0, count(reminder(endDate = lastViewed.toLocalDate().minusDays(5))))
    }

    @Test
    fun `manual archive never counts even when recent`() {
        // The user did this themselves — nothing to tell them
        assertEquals(0, count(reminder(archivedAt = lastViewed.plusDays(1))))
    }

    @Test
    fun `manual archive with a lapsed end date still does not count`() {
        val manual = reminder(endDate = lastViewed.toLocalDate().plusDays(1), archivedAt = lastViewed.plusDays(3))
        assertEquals(0, count(manual))
    }

    @Test
    fun `active reminders are ignored`() {
        assertEquals(0, count(reminder()))
    }

    @Test
    fun `end date of today has not lapsed yet`() {
        assertEquals(0, count(reminder(endDate = today)))
    }

    @Test
    fun `mixed list counts only the auto-lapsed ones`() {
        val n = count(
            reminder(endDate = lastViewed.toLocalDate().plusDays(1)),
            reminder(endDate = lastViewed.toLocalDate().plusDays(3)),
            reminder(archivedAt = lastViewed.plusDays(2)),
            reminder()
        )
        assertEquals(2, n)
    }
}
