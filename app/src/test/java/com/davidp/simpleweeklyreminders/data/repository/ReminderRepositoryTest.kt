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
    fun `every-N-days interval snaps forward to the next aligned day when generated mid-cycle`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(startDate = monday, dayInterval = 3, endDate = monday.plusDays(20))
        val midCycleNow = LocalDateTime.of(monday.plusDays(4), LocalTime.MIDNIGHT)

        val id = repo.insert(r, now = midCycleNow).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        // Interval sequence from start is monday, +3, +6, +9... so the next aligned
        // day from "today" (monday+4) is monday+6, not monday+4 or monday+5.
        assertEquals(
            listOf(monday.plusDays(6), monday.plusDays(9), monday.plusDays(12), monday.plusDays(15), monday.plusDays(18)),
            dates
        )
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

    // --- insert / generateLogsForReminder: `now` guard and dedup ---

    @Test
    fun `never generates a log at or before now`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val r = reminder(
            startDate = monday,
            endDate = monday,
            reminderDays = setOf(1),
            reminderTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        )
        val todayAtNoon = LocalDateTime.of(monday, LocalTime.of(12, 0))

        val id = repo.insert(r, now = todayAtNoon).toInt()

        val logs = allLogsFor(logDao, id)
        assertEquals(1, logs.size)
        assertEquals(LocalTime.of(20, 0), logs.single().logDateTime.toLocalTime())
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
    fun `already-started reminder loops from today, not the original start date`() = runBlocking {
        val (repo, _, logDao) = newRepository()
        val pastStart = monday.minusWeeks(4)
        val r = reminder(startDate = pastStart, endDate = pastStart.plusDays(60))
        val today = LocalDateTime.of(monday, LocalTime.MIDNIGHT)

        val id = repo.insert(r, now = today).toInt()

        val dates = allLogsFor(logDao, id).map { it.logDateTime.toLocalDate() }.sorted()
        assertEquals(monday, dates.first())
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
}
