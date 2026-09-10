package com.davidp.simpleweeklyreminders.debug

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.notification.ReminderWorker
import com.davidp.simpleweeklyreminders.data.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Debug-only dev tools, surfaced as Settings → Developer. Compiled into debug builds only;
 * src/release has a no-op stub with the same API so main code compiles for both.
 */
object DebugTools {
    const val ENABLED = true

    /**
     * Inserts a realistic spread of reminders: every recurrence type, importance, colour,
     * backdated history with a done/missed mix, one paused, one future-dated, and a few
     * archived. Goes through [ReminderRepository.insert] so logs are generated the real way.
     * @return how many reminders were added
     */
    suspend fun seedSampleReminders(context: Context): Int = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val logDao = database.reminderLogDao()
        val repository = ReminderRepository(database.reminderDao(), logDao)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        // Fixed seed: the same done/missed pattern every run, so a bug seen once reproduces
        val random = Random(SEED)
        val samples = sampleReminders(today)

        // One transaction: ~1-2k log rows land at once and the UI sees a single update
        database.withTransaction {
            samples.forEachIndexed { index, sample ->
                val reminder = sample.reminder.copy(
                    sortOrder = index,
                    createdAt = sample.reminder.startDate.atTime(9, 0)
                )
                val id = repository.insert(reminder, now).toInt()
                val saved = reminder.copy(id = id)

                val doneRate = random.nextDouble(0.55, 0.95)
                logDao.getLogsForReminderInRange(id, reminder.startDate.atStartOfDay(), now)
                    .filter { random.nextDouble() < doneRate }
                    .forEach { logDao.updateCompletedStatus(it.id, true) }

                // Same calls the ViewModel's pause/archive make, so state matches real use
                when (sample.after) {
                    After.NONE -> Unit
                    After.PAUSE -> repository.update(saved.copy(isActive = false), now)
                    After.ARCHIVE -> repository.update(
                        saved.copy(isActive = false, endDate = today.minusDays(1), archivedAt = now),
                        now
                    )
                }
            }
        }
        ReminderWorker.schedule(context)
        samples.size
    }

    /**
     * One test reminder per importance, due in [TEST_FIRE_DELAY_S] seconds, to check each
     * notification channel's sound/sticky/swipe behaviour without waiting.
     */
    suspend fun fireTestNotifications(context: Context) {
        val at = LocalDateTime.now().plusSeconds(TEST_FIRE_DELAY_S)
        addOneTimeReminders(context, Importance.entries.map { "Test · ${it.label()}" to it }, at)
    }

    /** One High reminder a minute out — time to lock the phone and test the full alarm path. */
    suspend fun addReminderDueInOneMinute(context: Context) {
        addOneTimeReminders(context, listOf("Due in 1 min" to Importance.HIGH), LocalDateTime.now().plusMinutes(1))
    }

    /**
     * Real one-time reminders via the repository + worker, so they fire through the actual
     * alarm path. Second-precision time (the form only offers minutes) so the wait is short.
     */
    private suspend fun addOneTimeReminders(
        context: Context,
        items: List<Pair<String, Importance>>,
        at: LocalDateTime
    ) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val repository = ReminderRepository(database.reminderDao(), database.reminderLogDao())
        val dateTime = at.truncatedTo(ChronoUnit.SECONDS)
        val date = dateTime.toLocalDate()
        items.forEach { (title, importance) ->
            repository.insert(
                Reminder(
                    title = title, reminderTimes = listOf(dateTime.toLocalTime()),
                    startDate = date, endDate = date, reminderDays = setOf(date.dayOfWeek.value),
                    reminderType = ReminderType.ONE_TIME, importance = importance, icon = "alarm"
                )
            )
        }
        ReminderWorker.schedule(context)
    }

    private fun Importance.label() = name.lowercase().replaceFirstChar { it.uppercase() }

    /** Wipes every reminder and log, plus any armed alarms and shown notifications. */
    suspend fun clearAllReminders(context: Context) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        // Chain alarms are keyed by reminderId; cancel them while the ids still exist.
        // Leftover snooze alarms are harmless: the receiver no-ops on a missing log.
        database.reminderDao().getAllRemindersList().forEach {
            ReminderWorker.cancelAlarm(context, it.id)
        }
        database.clearAllTables()
        NotificationManagerCompat.from(context).cancelAll()
    }

    private const val SEED = 42
    private const val TEST_FIRE_DELAY_S = 10L

    private enum class After { NONE, PAUSE, ARCHIVE }

    private class Sample(val reminder: Reminder, val after: After = After.NONE)

    private val EVERY_DAY = (1..7).toSet()
    private val WEEKDAYS = (1..5).toSet()

    private fun t(hour: Int, minute: Int = 0) = LocalTime.of(hour, minute)

    private fun sampleReminders(today: LocalDate): List<Sample> {
        fun daysAgo(n: Long) = today.minusDays(n)
        // One-time = start and end on the same date, on that date's weekday (as ReminderForm saves it)
        val dentistDate = today.plusDays(3)
        val inspectionDate = daysAgo(12)

        return listOf(
            // Active, recurring
            Sample(Reminder(
                title = "Take vitamins", reminderTimes = listOf(t(8)), startDate = daysAgo(60),
                reminderDays = EVERY_DAY, icon = "medication", color = "moss",
                importance = Importance.HIGH, notes = "With breakfast — D3 + omega-3"
            )),
            Sample(Reminder(
                title = "Drink water", reminderTimes = listOf(t(10), t(13), t(16)),
                startDate = daysAgo(21), reminderType = ReminderType.EVERY_N_DAYS, dayInterval = 1,
                icon = "waterDrop", color = "teal", importance = Importance.LOW
            )),
            Sample(Reminder(
                title = "Morning run", reminderTimes = listOf(t(6, 30)), startDate = daysAgo(45),
                reminderDays = setOf(1, 3, 5), icon = "directionsRun", color = "clay",
                importance = Importance.MEDIUM, notes = "5k loop around the park"
            )),
            Sample(Reminder(
                title = "Stand-up meeting", reminderTimes = listOf(t(9, 30)), startDate = daysAgo(90),
                reminderDays = WEEKDAYS, icon = "work", color = "indigo", importance = Importance.MEDIUM
            )),
            Sample(Reminder(
                title = "Water the plants", reminderTimes = listOf(t(18)), startDate = daysAgo(30),
                reminderType = ReminderType.EVERY_N_DAYS, dayInterval = 3, icon = "eco", color = "moss",
                importance = Importance.LOW, notes = "Fern needs extra on hot days"
            )),
            Sample(Reminder(
                title = "Take out the trash", reminderTimes = listOf(t(20)), startDate = daysAgo(56),
                reminderDays = setOf(2), icon = "cleaningServices", color = "amber",
                importance = Importance.MEDIUM
            )),
            Sample(Reminder(
                title = "Call Mom", reminderTimes = listOf(t(17)), startDate = daysAgo(40),
                reminderDays = setOf(7), icon = "familyRestroom", color = "plum", importance = Importance.HIGH
            )),
            Sample(Reminder(
                title = "Feed the cat", reminderTimes = listOf(t(7, 30), t(19)), startDate = daysAgo(35),
                reminderDays = EVERY_DAY, icon = "pets", color = "clay", importance = Importance.HIGH,
                notes = "Half a can + dry food"
            )),
            Sample(Reminder(
                title = "Meditate", reminderTimes = listOf(t(22)), startDate = daysAgo(14),
                reminderDays = EVERY_DAY, icon = "selfImprovement", color = "teal", importance = Importance.LOW
            )),
            Sample(Reminder(
                title = "Change bedsheets", reminderTimes = listOf(t(11)), startDate = daysAgo(42),
                reminderType = ReminderType.EVERY_N_DAYS, dayInterval = 14, icon = "kingBed",
                color = "indigo", importance = Importance.LOW
            )),
            // No icon, colour or notes — exercises the defaults
            Sample(Reminder(
                title = "Check the mail", reminderTimes = listOf(t(15)), startDate = daysAgo(10),
                reminderDays = (1..6).toSet(), importance = Importance.MEDIUM
            )),
            // Starts in the future — no history yet
            Sample(Reminder(
                title = "Swim laps", reminderTimes = listOf(t(18, 30)), startDate = today.plusDays(5),
                reminderDays = setOf(2, 4, 6), icon = "pool", color = "teal", importance = Importance.MEDIUM
            )),
            // Upcoming one-time
            Sample(Reminder(
                title = "Dentist appointment", reminderTimes = listOf(t(14, 15)),
                startDate = dentistDate, endDate = dentistDate, reminderDays = setOf(dentistDate.dayOfWeek.value),
                reminderType = ReminderType.ONE_TIME, icon = "medicalServices", color = "amber",
                importance = Importance.HIGH, notes = "Bring insurance card"
            )),
            // Paused
            Sample(Reminder(
                title = "Read before bed", reminderTimes = listOf(t(21, 30)), startDate = daysAgo(20),
                reminderDays = EVERY_DAY, icon = "menuBook", color = "plum", importance = Importance.LOW
            ), After.PAUSE),

            // Archived — lapsed on their own (endDate passed)
            Sample(Reminder(
                title = "Physical therapy exercises", reminderTimes = listOf(t(8, 30)),
                startDate = daysAgo(80), endDate = daysAgo(20), reminderDays = setOf(1, 3, 5),
                icon = "accessibilityNew", color = "teal", importance = Importance.HIGH
            )),
            Sample(Reminder(
                title = "Antibiotics", reminderTimes = listOf(t(8), t(20)),
                startDate = daysAgo(30), endDate = daysAgo(20), reminderDays = EVERY_DAY,
                icon = "localPharmacy", color = "clay", importance = Importance.HIGH,
                notes = "Finish the full course"
            )),
            Sample(Reminder(
                title = "Car inspection", reminderTimes = listOf(t(10)),
                startDate = inspectionDate, endDate = inspectionDate,
                reminderDays = setOf(inspectionDate.dayOfWeek.value),
                reminderType = ReminderType.ONE_TIME, icon = "directionsCar", importance = Importance.MEDIUM
            )),
            // Archived — manually, via the same path as the Archive action
            Sample(Reminder(
                title = "Study for exam", reminderTimes = listOf(t(19)), startDate = daysAgo(50),
                reminderDays = WEEKDAYS, icon = "school", color = "indigo", importance = Importance.MEDIUM
            ), After.ARCHIVE),
        )
    }
}
