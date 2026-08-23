package com.davidp.simpleweeklyreminders

import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.SortDirection
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.data.model.defaultDirection
import com.davidp.simpleweeklyreminders.data.model.sortedFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Regression tests for [sortedFor], the Reminders tab's sort-mode + direction ordering. */
class ReminderSortTest {

    // Monday 8am — scheduled after every reminder's daily 9am/6pm slots below, so their
    // "next occurrence" falls on the same day and stays easy to reason about.
    private val now: LocalDateTime = LocalDateTime.of(2026, 1, 5, 8, 0)

    private fun reminder(
        title: String,
        importance: Importance = Importance.MEDIUM,
        createdAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
        isActive: Boolean = true,
        reminderTimes: List<LocalTime> = listOf(LocalTime.of(9, 0))
    ) = Reminder(
        title = title,
        importance = importance,
        createdAt = createdAt,
        isActive = isActive,
        reminderTimes = reminderTimes,
        startDate = LocalDate.of(2020, 1, 1),
        reminderDays = setOf(1, 2, 3, 4, 5, 6, 7)
    )

    // --- MANUAL ---

    @Test
    fun `manual ignores direction`() {
        val list = listOf(reminder("C"), reminder("A"), reminder("B"))
        assertEquals(list, list.sortedFor(SortMode.MANUAL, SortDirection.ASCENDING))
        assertEquals(list, list.sortedFor(SortMode.MANUAL, SortDirection.DESCENDING))
    }

    // --- IMPORTANCE ---

    @Test
    fun `importance ascending is low to high`() {
        val low = reminder("Low", importance = Importance.LOW)
        val medium = reminder("Medium", importance = Importance.MEDIUM)
        val high = reminder("High", importance = Importance.HIGH)
        val list = listOf(high, low, medium)

        assertEquals(
            listOf(low, medium, high),
            list.sortedFor(SortMode.IMPORTANCE, SortDirection.ASCENDING)
        )
    }

    @Test
    fun `importance descending is high to low`() {
        val low = reminder("Low", importance = Importance.LOW)
        val medium = reminder("Medium", importance = Importance.MEDIUM)
        val high = reminder("High", importance = Importance.HIGH)
        val list = listOf(low, high, medium)

        assertEquals(
            listOf(high, medium, low),
            list.sortedFor(SortMode.IMPORTANCE, SortDirection.DESCENDING)
        )
    }

    @Test
    fun `importance ties preserve manual order`() {
        val a = reminder("A", importance = Importance.HIGH)
        val b = reminder("B", importance = Importance.HIGH)
        val c = reminder("C", importance = Importance.LOW)
        val list = listOf(a, b, c) // manual order: a, b, c

        // c (LOW) sorts first; a/b (tied HIGH) keep their original relative order.
        assertEquals(
            listOf(c, a, b),
            list.sortedFor(SortMode.IMPORTANCE, SortDirection.ASCENDING)
        )
    }

    // --- DATE_ADDED ---

    @Test
    fun `date added ascending is oldest first`() {
        val old = reminder("Old", createdAt = LocalDateTime.of(2025, 1, 1, 0, 0))
        val mid = reminder("Mid", createdAt = LocalDateTime.of(2025, 6, 1, 0, 0))
        val new = reminder("New", createdAt = LocalDateTime.of(2026, 1, 1, 0, 0))
        val list = listOf(new, old, mid)

        assertEquals(
            listOf(old, mid, new),
            list.sortedFor(SortMode.DATE_ADDED, SortDirection.ASCENDING)
        )
    }

    @Test
    fun `date added descending is newest first`() {
        val old = reminder("Old", createdAt = LocalDateTime.of(2025, 1, 1, 0, 0))
        val mid = reminder("Mid", createdAt = LocalDateTime.of(2025, 6, 1, 0, 0))
        val new = reminder("New", createdAt = LocalDateTime.of(2026, 1, 1, 0, 0))
        val list = listOf(old, new, mid)

        assertEquals(
            listOf(new, mid, old),
            list.sortedFor(SortMode.DATE_ADDED, SortDirection.DESCENDING)
        )
    }

    // --- NEXT_OCCURRENCE ---

    @Test
    fun `next occurrence ascending is soonest first`() {
        val soon = reminder("Soon", reminderTimes = listOf(LocalTime.of(9, 0)))
        val later = reminder("Later", reminderTimes = listOf(LocalTime.of(18, 0)))
        val list = listOf(later, soon)

        assertEquals(
            listOf(soon, later),
            list.sortedFor(SortMode.NEXT_OCCURRENCE, SortDirection.ASCENDING, now)
        )
    }

    @Test
    fun `next occurrence descending is latest first`() {
        val soon = reminder("Soon", reminderTimes = listOf(LocalTime.of(9, 0)))
        val later = reminder("Later", reminderTimes = listOf(LocalTime.of(18, 0)))
        val list = listOf(soon, later)

        assertEquals(
            listOf(later, soon),
            list.sortedFor(SortMode.NEXT_OCCURRENCE, SortDirection.DESCENDING, now)
        )
    }

    @Test
    fun `next occurrence pins paused reminders last ascending`() {
        val active = reminder("Active", isActive = true)
        val paused = reminder("Paused", isActive = false)
        val list = listOf(paused, active)

        assertEquals(
            listOf(active, paused),
            list.sortedFor(SortMode.NEXT_OCCURRENCE, SortDirection.ASCENDING, now)
        )
    }

    @Test
    fun `next occurrence pins paused reminders last descending too`() {
        val active = reminder("Active", isActive = true)
        val paused = reminder("Paused", isActive = false)
        val list = listOf(paused, active)

        // Descending still means "paused sinks to the bottom", not "flips to the top" —
        // a paused reminder was never "latest", it's just not applicable to this sort.
        assertEquals(
            listOf(active, paused),
            list.sortedFor(SortMode.NEXT_OCCURRENCE, SortDirection.DESCENDING, now)
        )
    }

    @Test
    fun `next occurrence treats no reminder times like no occurrence`() {
        val noTimes = reminder("NoTimes", reminderTimes = emptyList())
        val withTimes = reminder("WithTimes", reminderTimes = listOf(LocalTime.of(9, 0)))
        val list = listOf(noTimes, withTimes)

        assertEquals(
            listOf(withTimes, noTimes),
            list.sortedFor(SortMode.NEXT_OCCURRENCE, SortDirection.ASCENDING, now)
        )
    }

    // --- TITLE ---

    @Test
    fun `title ascending sorts A to Z`() {
        val list = listOf(reminder("Vitamins"), reminder("Meds"), reminder("Plants"))
        assertEquals(
            listOf("Meds", "Plants", "Vitamins"),
            list.sortedFor(SortMode.TITLE, SortDirection.ASCENDING, now).map { it.title }
        )
    }

    @Test
    fun `title descending sorts Z to A`() {
        val list = listOf(reminder("Meds"), reminder("Vitamins"), reminder("Plants"))
        assertEquals(
            listOf("Vitamins", "Plants", "Meds"),
            list.sortedFor(SortMode.TITLE, SortDirection.DESCENDING, now).map { it.title }
        )
    }

    @Test
    fun `title sort ignores case`() {
        // A case-sensitive sort would put every capitalised title before every lowercase one
        val list = listOf(reminder("banana"), reminder("Apple"), reminder("cherry"), reminder("Date"))
        assertEquals(
            listOf("Apple", "banana", "cherry", "Date"),
            list.sortedFor(SortMode.TITLE, SortDirection.ASCENDING, now).map { it.title }
        )
    }

    @Test
    fun `equal titles keep manual order`() {
        val first = reminder("Meds", createdAt = LocalDateTime.of(2025, 1, 1, 0, 0))
        val second = reminder("Meds", createdAt = LocalDateTime.of(2025, 6, 1, 0, 0))
        val sorted = listOf(first, second).sortedFor(SortMode.TITLE, SortDirection.ASCENDING, now)
        assertEquals(listOf(first, second), sorted)
    }

    // --- Empty list ---

    @Test
    fun `empty list is handled for every sort mode`() {
        val empty = emptyList<Reminder>()
        SortMode.entries.forEach { mode ->
            assertTrue(empty.sortedFor(mode, mode.defaultDirection(), now).isEmpty())
        }
    }

    // --- defaultDirection ---

    @Test
    fun `default direction favors high-first for importance, ascending otherwise`() {
        assertEquals(SortDirection.DESCENDING, SortMode.IMPORTANCE.defaultDirection())
        assertEquals(SortDirection.ASCENDING, SortMode.MANUAL.defaultDirection())
        assertEquals(SortDirection.ASCENDING, SortMode.DATE_ADDED.defaultDirection())
        assertEquals(SortDirection.ASCENDING, SortMode.NEXT_OCCURRENCE.defaultDirection())
        assertEquals(SortDirection.ASCENDING, SortMode.TITLE.defaultDirection())
    }
}
