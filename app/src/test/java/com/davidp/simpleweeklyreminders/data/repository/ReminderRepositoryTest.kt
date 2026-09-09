package com.davidp.simpleweeklyreminders.data.repository

import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Regression tests for ReminderRepository's log-generation and update logic,
 * using in-memory fake DAOs and an explicit `now` so results don't depend on
 * when the tests run. Dates are fixed anchors, same convention as ReminderScheduleTest.
 */
class ReminderRepositoryTest {

    // 2026-01-05 is a Monday
    private val monday: LocalDate = LocalDate.of(2026, 1, 5)

    // Well before any anchor date, used as `now` whenever a test only cares
    // about the generated schedule, not "already started" edge cases.
    private val beforeStart = LocalDateTime.of(monday.minusMonths(1), LocalTime.MIDNIGHT)

    // Earliest possible `after` cutoff so "future" queries return everything.
    private val epoch = LocalDateTime.of(2000, 1, 1, 0, 0)

    private fun newRepository(): Triple<ReminderRepository, FakeReminderDao, FakeReminderLogDao> {
        val reminderDao = FakeReminderDao()
        val logDao = FakeReminderLogDao()
        return Triple(ReminderRepository(reminderDao, logDao), reminderDao, logDao)
    }

    private fun reminder(
        id: Int = 0,
        title: String = "Test",
        reminderTimes: List<LocalTime> = listOf(LocalTime.of(9, 0)),
        startDate: LocalDate = monday,
        endDate: LocalDate? = null,
        reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
        dayInterval: Int? = null,
        reminderType: ReminderType = if (dayInterval != null) ReminderType.EVERY_N_DAYS else ReminderType.SPECIFIC_DAYS,
        isActive: Boolean = true
    ) = Reminder(
        id = id,
        title = title,
        reminderTimes = reminderTimes,
        startDate = startDate,
        endDate = endDate,
        reminderDays = reminderDays,
        dayInterval = dayInterval,
        reminderType = reminderType,
        isActive = isActive
    )

    private suspend fun allLogsFor(logDao: FakeReminderLogDao, reminderId: Int): List<ReminderLog> =
        logDao.getFutureLogsForReminder(reminderId, epoch)

    // --- insert / generateLogsForReminder: schedule modes ---

    @Test
    fun `insert generates logs only for matching weekdays`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(reminderDays = setOf(1), startDate = monday, endDate = monday.plusWeeks(2))

        val id = repo.insert(r, now = beforeStart).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(listOf(monday, monday.plusWeeks(1), monday.plusWeeks(2)), dates)
    }

    @Test
    fun `insert on every-N-days interval steps by the interval from the start date`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(startDate = monday, dayInterval = 3, endDate = monday.plusDays(10))

        val id = repo.insert(r, now = beforeStart).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(listOf(monday, monday.plusDays(3), monday.plusDays(6), monday.plusDays(9)), dates)
    }

    @Test
    fun `update snaps an interval reminder forward to the next aligned day`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(startDate = monday, dayInterval = 3, endDate = monday.plusDays(20))
        val id = repo.insert(r, now = beforeStart).toInt()
        val midCycle = LocalDateTime.of(monday.plusDays(4), LocalTime.MIDNIGHT)

        // Editing regenerates the future only. The interval sequence from start is
        // monday, +3, +6, +9..., so the next aligned day after monday+4 is monday+6.
        repo.update(r.copy(id = id), now = midCycle)

        val futureDates = logDao.getFutureLogsForReminder(id, midCycle)
            .map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(monday.plusDays(6), futureDates.first())
    }

    @Test
    fun `insert on a one-time reminder generates exactly its single-date occurrences`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(
            startDate = monday,
            endDate = monday,
            reminderDays = setOf(1),
            reminderTimes = listOf(LocalTime.of(9, 0), LocalTime.of(18, 0)),
            reminderType = ReminderType.ONE_TIME
        )

        val id = repo.insert(r, now = beforeStart).toInt()

        val logs = allLogsFor(logDao, id)
        assertEquals(2, logs.size)
        assertTrue(logs.all { it.logDateTime.toLocalDate() == monday })
    }

    // --- insert / generateLogsForReminder: backfill and dedup ---

    @Test
    fun `insert backfills an earlier-today slot that has already passed`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(
            startDate = monday,
            endDate = monday,
            reminderDays = setOf(1),
            reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        )
        val todayAtNoon = LocalDateTime.of(monday, LocalTime.of(12, 0))

        val id = repo.insert(r, now = todayAtNoon).toInt()

        // 08:00 had already passed at creation but is still backfilled (tappable, reads
        // missed); 20:00 is upcoming. Both exist so the calendar has real rows to draw.
        val times = allLogsFor(logDao, id).map { it.logDateTime.toLocalTime() }.sorted()
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)), times)
    }

    @Test
    fun `does not duplicate a log that already exists at that exact time`() = runBlocking {
        val (repo, reminderDao, logDao) = newRepository()
        // FakeReminderDao assigns id 1 to the first inserted reminder — seed a
        // pre-existing log for that id before insert() triggers generation.
        logDao.insert(ReminderLog(reminderId = 1, title = "Test", logDateTime = LocalDateTime.of(monday, LocalTime.of(9, 0))))
        val r = reminder(reminderDays = setOf(1), startDate = monday, endDate = monday.plusWeeks(1))

        val id = repo.insert(r, now = beforeStart).toInt()

        val logs = allLogsFor(logDao, id)
        assertEquals(2, logs.size) // the seeded one + next week's, not a duplicate of the seeded slot
        assertEquals(1, logs.count { it.logDateTime == LocalDateTime.of(monday, LocalTime.of(9, 0)) })
    }

    @Test
    fun `inactive reminder generates no logs`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(isActive = false)

        val id = repo.insert(r, now = beforeStart).toInt()

        assertTrue(allLogsFor(logDao, id).isEmpty())
    }

    // --- generateLogsForReminder: endDate and loopStart defaults ---

    @Test
    fun `null endDate defaults to one year from now, inclusive`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(startDate = monday, endDate = null, dayInterval = 365)
        val today = LocalDateTime.of(monday, LocalTime.MIDNIGHT)

        val id = repo.insert(r, now = today).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(listOf(monday, monday.plusYears(1)), dates)
    }

    @Test
    fun `future start date is used as loop start, not today`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val futureStart = monday.plusWeeks(2)
        val r = reminder(startDate = futureStart, endDate = futureStart.plusDays(2))
        val today = LocalDateTime.of(monday, LocalTime.MIDNIGHT)

        val id = repo.insert(r, now = today).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(listOf(futureStart, futureStart.plusDays(1), futureStart.plusDays(2)), dates)
    }

    @Test
    fun `update regenerates the future from today, not the original start date`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val pastStart = monday.minusWeeks(4)
        val r = reminder(startDate = pastStart, endDate = pastStart.plusDays(60))
        val today = LocalDateTime.of(monday, LocalTime.MIDNIGHT)
        val id = repo.insert(r, now = today).toInt()

        // Editing regenerates future logs from today; it must not re-seed the past run.
        repo.update(r.copy(id = id), now = today)

        val futureDates = logDao.getFutureLogsForReminder(id, today)
            .map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(monday, futureDates.first())
    }

    // --- update() ---

    @Test
    fun `pausing deletes future incomplete logs but keeps completed ones and stops regenerating`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val original = reminder(startDate = monday, endDate = monday.plusDays(10))
        val id = repo.insert(original, now = beforeStart).toInt()
        val completedSlot = LocalDateTime.of(monday.plusDays(5), LocalTime.of(9, 0))
        logDao.getFutureLogsForReminder(id, epoch).single { it.logDateTime == completedSlot }
            .let { logDao.updateCompletedStatus(it.id, true) }

        val pauseNow = LocalDateTime.of(monday.plusDays(3), LocalTime.of(12, 0))
        repo.update(original.copy(id = id, isActive = false), now = pauseNow)

        val remaining = allLogsFor(logDao, id).map { it.logDateTime }.toSet()
        assertTrue("completed future log should survive", completedSlot in remaining)
        assertFalse(
            "future incomplete log should be deleted",
            LocalDateTime.of(monday.plusDays(6), LocalTime.of(9, 0)) in remaining
        )
        assertTrue(
            "already-past log should be untouched",
            LocalDateTime.of(monday.plusDays(2), LocalTime.of(9, 0)) in remaining
        )
    }

    @Test
    fun `regenerating an unchanged schedule preserves completion at the same datetime`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val original = reminder(startDate = monday, endDate = monday.plusDays(5))
        val id = repo.insert(original, now = beforeStart).toInt()
        val completedSlot = LocalDateTime.of(monday.plusDays(2), LocalTime.of(9, 0))
        logDao.getFutureLogsForReminder(id, epoch).single { it.logDateTime == completedSlot }
            .let { logDao.updateCompletedStatus(it.id, true) }

        repo.update(original.copy(id = id), now = beforeStart)

        val logs = allLogsFor(logDao, id)
        assertEquals(6, logs.size) // monday..monday+5 inclusive
        assertTrue(logs.single { it.logDateTime == completedSlot }.completed)
    }

    @Test
    fun `changing the time carries a completion over to the new same-day slot`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val original = reminder(
            startDate = monday,
            endDate = monday.plusDays(3),
            reminderTimes = listOf(LocalTime.of(9, 0))
        )
        val id = repo.insert(original, now = beforeStart).toInt()
        val oldSlot = LocalDateTime.of(monday.plusDays(1), LocalTime.of(9, 0))
        logDao.getFutureLogsForReminder(id, epoch).single { it.logDateTime == oldSlot }
            .let { logDao.updateCompletedStatus(it.id, true) }

        val edited = original.copy(id = id, reminderTimes = listOf(LocalTime.of(10, 0)))
        repo.update(edited, now = beforeStart)

        val logsOnThatDay = allLogsFor(logDao, id).filter { it.logDateTime.toLocalDate() == monday.plusDays(1) }
        assertEquals(1, logsOnThatDay.size)
        assertEquals(LocalTime.of(10, 0), logsOnThatDay.single().logDateTime.toLocalTime())
        assertTrue(logsOnThatDay.single().completed)
    }

    @Test
    fun `title change propagates to historical logs, not just future ones`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val original = reminder(title = "Old Title", startDate = monday, endDate = monday.plusDays(3))
        val id = repo.insert(original, now = beforeStart).toInt()
        // A completed log from before the reminder's schedule window (a past, historical entry).
        logDao.insert(
            ReminderLog(
                reminderId = id,
                title = "Old Title",
                logDateTime = LocalDateTime.of(monday.minusDays(10), LocalTime.of(9, 0)),
                completed = true
            )
        )

        repo.update(original.copy(id = id, title = "New Title"), now = beforeStart)

        val logs = allLogsFor(logDao, id)
        assertTrue(logs.all { it.title == "New Title" })
    }

    // --- archiveStats ---

    @Test
    fun `archiveStats counts done and missed within the reminder span only`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(id = 1, startDate = monday, endDate = monday.plusDays(2))
        val now = LocalDateTime.of(monday.plusDays(10), LocalTime.NOON)

        suspend fun add(at: LocalDateTime, completed: Boolean, reminderId: Int = 1) =
            logDao.insert(ReminderLog(reminderId = reminderId, title = "T", logDateTime = at, completed = completed))

        add(monday.atTime(9, 0), completed = true)
        add(monday.plusDays(1).atTime(9, 0), completed = true)
        add(monday.plusDays(2).atTime(9, 0), completed = false)          // missed (elapsed, incomplete)
        add(monday.plusDays(5).atTime(9, 0), completed = false)          // after endDate — excluded
        add(monday.minusDays(1).atTime(9, 0), completed = true)          // before startDate — excluded
        add(monday.atTime(9, 0), completed = true, reminderId = 2)       // other reminder — excluded

        val counts = repo.archiveStats(r, now)

        assertEquals(2, counts.done)
        assertEquals(1, counts.missed)
        assertEquals(3, counts.total)
    }

    // --- backfill on create (past occurrences) ---

    @Test
    fun `insert backfills past occurrences from a backdated start`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        // Daily reminder started a week before "now" — every day up to now is in the past
        val now = LocalDateTime.of(monday.plusDays(6), LocalTime.NOON)
        val r = reminder(startDate = monday, endDate = monday.plusDays(6))

        val id = repo.insert(r, now = now).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals((0L..6L).map { monday.plusDays(it) }, dates)
        // Backfilled slots are uncompleted (they read as missed until acted on)
        assertTrue(allLogsFor(logDao, id).none { it.completed })
    }

    @Test
    fun `insert backfills every-N-days on the correct cadence`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val now = LocalDateTime.of(monday.plusDays(10), LocalTime.NOON)
        val r = reminder(startDate = monday, dayInterval = 3, endDate = monday.plusDays(9))

        val id = repo.insert(r, now = now).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        // Phase anchored at startDate: days 0, 3, 6, 9 — no seam
        assertEquals(listOf(monday, monday.plusDays(3), monday.plusDays(6), monday.plusDays(9)), dates)
    }

    @Test
    fun `update does not backfill past occurrences`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val now = LocalDateTime.of(monday.plusDays(3), LocalTime.NOON)
        val r = reminder(startDate = monday, endDate = monday.plusDays(6))
        val id = repo.insert(r, now = now).toInt()

        val today = now.toLocalDate()
        val pastBefore = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.filter { it < today }.sorted()
        assertTrue(pastBefore.isNotEmpty())

        // An edit regenerates the future only — past logs must be left exactly as they were
        repo.update(r.copy(id = id, title = "Renamed"), now = now)

        val pastAfter = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.filter { it < today }.sorted()
        assertEquals(pastBefore, pastAfter)
    }

    // --- topUpLogs (self-sustaining chain) ---

    @Test
    fun `topUpLogs advances the horizon additively so the chain never runs dry`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        // No end date: insert fills to now+1yr; the chain would otherwise run out there
        val r = reminder(startDate = monday, endDate = null)
        val t0 = LocalDateTime.of(monday, LocalTime.MIDNIGHT)
        val id = repo.insert(r, now = t0).toInt()
        val countAfterInsert = allLogsFor(logDao, id).size

        // Simulate a year passing (the chain reached the materialized edge) and top up
        val t1 = t0.plusYears(1)
        repo.topUpLogs(r.copy(id = id), now = t1)

        val all = allLogsFor(logDao, id)
        assertTrue(all.size > countAfterInsert)          // additive: new future rows added
        assertTrue(all.any { it.logDateTime > t1 })       // a next occurrence now exists past t1
    }
}
