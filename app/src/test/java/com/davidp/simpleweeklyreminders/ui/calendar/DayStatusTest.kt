package com.davidp.simpleweeklyreminders.ui.calendar

import com.davidp.simpleweeklyreminders.data.model.OccurrenceStatus
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.countOutcomes
import com.davidp.simpleweeklyreminders.data.model.statusOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The calendar's done/pending/missed split. Status is derived from `completed` +
 * `logDateTime` + `snoozedUntil`, never stored, so this is where that rule is pinned down.
 */
class DayStatusTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 6, 12, 0)
    private val today: LocalDate = now.toLocalDate()

    private fun log(
        id: Int,
        at: LocalDateTime,
        completed: Boolean = false,
        snoozedUntil: LocalDateTime? = null,
        reminderId: Int = 1
    ) = ReminderLog(
        id = id,
        reminderId = reminderId,
        title = "Meds",
        logDateTime = at,
        completed = completed,
        snoozedUntil = snoozedUntil
    )

    @Test
    fun completedIsDone_evenWhenInThePast() {
        val status = statusOf(log(1, now.minusHours(3), completed = true), now)
        assertEquals(OccurrenceStatus.DONE, status)
    }

    @Test
    fun completedIsDone_evenWhenInTheFuture() {
        val status = statusOf(log(1, now.plusHours(3), completed = true), now)
        assertEquals(OccurrenceStatus.DONE, status)
    }

    @Test
    fun uncompletedFutureIsPending() {
        assertEquals(OccurrenceStatus.PENDING, statusOf(log(1, now.plusHours(1)), now))
    }

    @Test
    fun uncompletedPastIsMissed() {
        assertEquals(OccurrenceStatus.MISSED, statusOf(log(1, now.minusHours(1)), now))
    }

    @Test
    fun pendingSnoozeIsPending_notMissed() {
        // Deferred on purpose and the notification will fire again — flagging the day as
        // missed would be wrong.
        val snoozed = log(1, now.minusHours(1), snoozedUntil = now.plusMinutes(10))
        assertEquals(OccurrenceStatus.PENDING, statusOf(snoozed, now))
    }

    @Test
    fun elapsedSnoozeIsMissed() {
        val snoozed = log(1, now.minusHours(2), snoozedUntil = now.minusMinutes(5))
        assertEquals(OccurrenceStatus.MISSED, statusOf(snoozed, now))
    }

    @Test
    fun segmentsAreInTimeOrder_regardlessOfInputOrder() {
        val logs = listOf(
            log(1, today.atTime(18, 0)),                    // pending
            log(2, today.atTime(8, 0), completed = true),   // done
            log(3, today.atTime(11, 40))                    // missed
        )
        val segments = dayStatuses(logs, now).getValue(today).segments
        assertEquals(
            listOf(OccurrenceStatus.DONE, OccurrenceStatus.MISSED, OccurrenceStatus.PENDING),
            segments.map { it.status }
        )
    }

    @Test
    fun countsDoneAndFlagsMissed() {
        val logs = listOf(
            log(1, today.atTime(8, 0), completed = true),
            log(2, today.atTime(9, 0), completed = true),
            log(3, today.atTime(11, 40)),
            log(4, today.atTime(18, 0))
        )
        val status = dayStatuses(logs, now).getValue(today)

        assertEquals(4, status.total)
        assertEquals(2, status.doneCount)
        assertTrue(status.hasMissed)
    }

    @Test
    fun allDoneDayHasNoMissedFlag() {
        val logs = listOf(
            log(1, today.atTime(8, 0), completed = true),
            log(2, today.atTime(9, 0), completed = true)
        )
        val status = dayStatuses(logs, now).getValue(today)

        assertEquals(2, status.doneCount)
        assertFalse(status.hasMissed)
    }

    @Test
    fun allPendingFutureDayHasNoMissedFlag() {
        val tomorrow = today.plusDays(1)
        val status = dayStatuses(listOf(log(1, tomorrow.atTime(9, 0))), now).getValue(tomorrow)

        assertEquals(0, status.doneCount)
        assertFalse(status.hasMissed)
    }

    @Test
    fun daysAreSeparated() {
        val logs = listOf(
            log(1, today.minusDays(1).atTime(9, 0), completed = true),
            log(2, today.atTime(9, 0)),
            log(3, today.plusDays(1).atTime(9, 0))
        )
        val byDay = dayStatuses(logs, now)

        assertEquals(3, byDay.size)
        assertEquals(1, byDay.getValue(today.minusDays(1)).doneCount)
        assertTrue(byDay.getValue(today).hasMissed)
        assertFalse(byDay.getValue(today.plusDays(1)).hasMissed)
    }

    @Test
    fun dayWithNoOccurrencesIsAbsent_notEmpty() {
        // The cell uses absence to mean "nothing scheduled" and skips drawing a bar,
        // which is different from "scheduled but none done".
        val byDay = dayStatuses(listOf(log(1, today.atTime(9, 0))), now)
        assertNull(byDay[today.plusDays(1)])
    }

    @Test
    fun emptyInputGivesEmptyMap() {
        assertTrue(dayStatuses(emptyList(), now).isEmpty())
    }

    // --- countOutcomes (archive row tally) ---

    @Test
    fun countOutcomes_talliesEachStatus() {
        val logs = listOf(
            log(1, now.minusHours(3), completed = true),
            log(2, now.minusHours(2), completed = true),
            log(3, now.minusHours(1)),                              // missed
            log(4, now.plusHours(1)),                               // pending
            log(5, now.minusHours(1), snoozedUntil = now.plusMinutes(10)) // pending snooze
        )
        val counts = countOutcomes(logs, now)

        assertEquals(2, counts.done)
        assertEquals(1, counts.missed)
        assertEquals(2, counts.pending)
        assertEquals(5, counts.total)
    }

    @Test
    fun countOutcomes_emptyIsAllZero() {
        val counts = countOutcomes(emptyList(), now)
        assertEquals(0, counts.total)
    }

    // --- per-reminder colour ---

    @Test
    fun segmentsCarryTheirOwnReminderColour() {
        val logs = listOf(
            log(1, today.atTime(8, 0), reminderId = 1),
            log(2, today.atTime(9, 0), reminderId = 2)
        )
        val segments = dayStatuses(logs, now, mapOf(1 to "plum", 2 to "moss"))
            .getValue(today).segments

        assertEquals(listOf("plum", "moss"), segments.map { it.colorKey })
    }

    @Test
    fun reminderWithNoColourGivesNullKey() {
        // Null means "follow the theme accent" — the strip must not invent one
        val logs = listOf(log(1, today.atTime(8, 0), reminderId = 7))
        val segments = dayStatuses(logs, now, mapOf(7 to null)).getValue(today).segments

        assertNull(segments.single().colorKey)
    }

    @Test
    fun unknownReminderIdGivesNullKey() {
        // An archived or deleted reminder can leave logs behind; they shouldn't crash
        val logs = listOf(log(1, today.atTime(8, 0), reminderId = 99))
        val segments = dayStatuses(logs, now, mapOf(1 to "teal")).getValue(today).segments

        assertNull(segments.single().colorKey)
    }
}
