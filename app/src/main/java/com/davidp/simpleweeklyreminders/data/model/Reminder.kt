package com.davidp.simpleweeklyreminders.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class ReminderType { SPECIFIC_DAYS, INTERVAL, ONE_TIME }

/** What an INTERVAL reminder's [Reminder.interval] counts. */
enum class IntervalUnit { DAYS, WEEKS, MONTHS, YEARS }

/** How a monthly reminder picks its day: same date as startDate ("the 15th"), or "3rd Monday". */
enum class MonthlyMode { DAY_OF_MONTH, NTH_WEEKDAY }

// Declared low-to-high so ordinal comparisons sort correctly at read time — never persist
// the ordinal itself (stored as TEXT via Converters), so inserting a level later (e.g.
// between MEDIUM and HIGH) needs a new case here, not a data migration.
enum class Importance { LOW, MEDIUM, HIGH }

/** Every level — the unfiltered default for the Reminders tab's importance filter. */
val ALL_IMPORTANCES: Set<Importance> = Importance.entries.toSet()

/** How the Reminders tab list is ordered. MANUAL is the drag-reorderable default. */
enum class SortMode { MANUAL, DATE_ADDED, IMPORTANCE, NEXT_OCCURRENCE, TITLE }

enum class SortDirection { ASCENDING, DESCENDING }

/** Sensible starting direction when a sort mode is first selected (still user-toggleable). */
fun SortMode.defaultDirection(): SortDirection = when (this) {
    SortMode.IMPORTANCE -> SortDirection.DESCENDING // High first reads as "most important on top"
    SortMode.MANUAL, SortMode.DATE_ADDED, SortMode.NEXT_OCCURRENCE, SortMode.TITLE ->
        SortDirection.ASCENDING
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val reminderTimes: List<LocalTime>,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday, 7 = Sunday
    val notes: String? = null,
    val color: String? = null,
    val icon: String? = null,
    // Count of [intervalUnit]s, only meaningful when reminderType == INTERVAL. Column keeps its
    // pre-v10 name: minSdk 26's SQLite has no RENAME COLUMN.
    @ColumnInfo(name = "dayInterval") val interval: Int? = null,
    val reminderType: ReminderType = ReminderType.SPECIFIC_DAYS,
    // INTERVAL only. WEEKS also reads reminderDays; MONTHS reads monthlyMode.
    @ColumnInfo(defaultValue = "DAYS") val intervalUnit: IntervalUnit = IntervalUnit.DAYS,
    @ColumnInfo(defaultValue = "DAY_OF_MONTH") val monthlyMode: MonthlyMode = MonthlyMode.DAY_OF_MONTH,
    val isActive: Boolean = true,        // false = paused, skipped in scheduling
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val sortOrder: Int = 0,               // user-defined drag order, fallback = createdAt
    // Set on manual archive(), cleared on restore(). Non-null IS archived: archive() leaves
    // endDate alone so the user's end date survives a restore. Null for auto-lapsed reminders,
    // which are archived by endDate instead (see isArchived).
    val archivedAt: LocalDateTime? = null,
    // Default HIGH preserves today's actual notification behavior (sticky, swipe = snooze)
    // for every existing reminder until the importance-driven behavior work (2.2) ships.
    val importance: Importance = Importance.HIGH,
    // Custom tone: a ringtone URI, SILENT_SOUND, or null for its importance level's tone.
    // Only used while "Per-reminder sounds" is on, and never for LOW (silent channel).
    val sound: String? = null,
    // Snooze length in minutes, or null for the global Settings length
    val snoozeMinutes: Int? = null
)

/** [Reminder.sound] value for "None" in the tone picker — distinct from null (= level tone). */
const val SILENT_SOUND = "silent"

/**
 * Prefill for "Duplicate": same settings, but a fresh row starting [today].
 * - id = 0 so Room inserts; insert() assigns sortOrder
 * - Active and unarchived, whatever the original was
 */
fun Reminder.asDuplicate(today: LocalDate, now: LocalDateTime = LocalDateTime.now()): Reminder =
    copy(id = 0, startDate = today, isActive = true, archivedAt = null, createdAt = now)

/** Whether this reminder's schedule includes the given date (ignores isActive). */
fun Reminder.isScheduledOn(date: LocalDate): Boolean {
    if (date < startDate) return false
    endDate?.let { if (date > it) return false }
    return when (reminderType) {
        ReminderType.INTERVAL -> isIntervalDay(date)
        ReminderType.SPECIFIC_DAYS, ReminderType.ONE_TIME -> reminderDays.contains(date.dayOfWeek.value)
    }
}

/** INTERVAL cadence, anchored on startDate. Assumes date >= startDate. */
private fun Reminder.isIntervalDay(date: LocalDate): Boolean {
    val n = (interval ?: 1).toLong()
    return when (intervalUnit) {
        IntervalUnit.DAYS -> ChronoUnit.DAYS.between(startDate, date) % n == 0L
        // Week of startDate is week 0; reminderDays picks the days inside an "on" week
        IntervalUnit.WEEKS -> ChronoUnit.WEEKS.between(startDate.weekMonday(), date.weekMonday()) % n == 0L &&
            reminderDays.contains(date.dayOfWeek.value)
        IntervalUnit.MONTHS -> {
            val months = ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(date))
            months % n == 0L && when (monthlyMode) {
                // plusMonths clamps: the 31st lands on the 30th / 28th in shorter months
                MonthlyMode.DAY_OF_MONTH -> startDate.plusMonths(months) == date
                MonthlyMode.NTH_WEEKDAY -> date.dayOfWeek == startDate.dayOfWeek &&
                    when (val nth = startDate.nthWeekdayOfMonth()) {
                        // Not every month has a 5th Monday, so a 5th start means "last"
                        5 -> date.plusWeeks(1).month != date.month
                        else -> date.nthWeekdayOfMonth() == nth
                    }
            }
        }
        IntervalUnit.YEARS -> {
            val years = (date.year - startDate.year).toLong()
            // plusYears clamps Feb 29 to Feb 28 in non-leap years
            years % n == 0L && startDate.plusYears(years) == date
        }
    }
}

private fun LocalDate.weekMonday(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Which occurrence of its weekday this date is in its month, 1..5 ("3rd Monday" = 3). */
fun LocalDate.nthWeekdayOfMonth(): Int = (dayOfMonth - 1) / 7 + 1

/**
 * Upper bound on the gap between two occurrences, in days. Anything that looks ahead for "the
 * next one" (log window, next-occurrence sort) must reach at least this far.
 */
fun Reminder.cycleDays(): Long {
    if (reminderType != ReminderType.INTERVAL) return 7
    val n = (interval ?: 1).toLong()
    return when (intervalUnit) {
        IntervalUnit.DAYS -> n
        IntervalUnit.WEEKS -> 7 * n + 7
        IntervalUnit.MONTHS -> 31 * n + 7
        IntervalUnit.YEARS -> 366 * n
    }
}

/**
 * Date of the [n]th scheduled day on or after [from] (ignores endDate), for "End after N
 * times". Counts days, not times: two times a day still counts once. Null if none is found.
 */
fun Reminder.nthOccurrenceDate(n: Int, from: LocalDate): LocalDate? {
    if (n < 1) return null
    val open = copy(endDate = null)
    var date = maxOf(from, startDate)
    // Each occurrence is at most cycleDays() after the last, so n cycles always reach the nth
    val last = date.plusDays(n * cycleDays())
    var seen = 0
    while (date <= last) {
        if (open.isScheduledOn(date) && ++seen == n) return date
        date = date.plusDays(1)
    }
    return null
}

/**
 * Whether this reminder's date range covers the given date. Unlike [isScheduledOn] this
 * ignores the day-of-week/interval cadence — it answers "did this reminder exist on that
 * day", which is what looking up a log's icon/importance needs (including for reminders
 * that have since been archived).
 */
fun Reminder.coversDate(date: LocalDate): Boolean =
    startDate <= date && (endDate == null || endDate >= date)

/** True once this reminder's schedule has fully elapsed — the day after endDate, calendar-date based. */
fun Reminder.hasLapsed(today: LocalDate = LocalDate.now()): Boolean = endDate != null && endDate < today

/**
 * Archived either way it can happen: manually ([archivedAt] stamped) or by lapsing past its
 * end date. Archived no longer implies a past endDate — anything that projects the schedule
 * forward has to check this (or isActive), not trust endDate to stop it.
 */
fun Reminder.isArchived(today: LocalDate = LocalDate.now()): Boolean =
    archivedAt != null || hasLapsed(today)

/**
 * The moment this reminder became archived, or null if it isn't archived. Prefers the
 * precise [archivedAt] timestamp (manual archive); auto-lapse has no event to hook, so
 * falls back to day-granularity (day after endDate) — the earliest moment it's provably true.
 * Non-null for everything [isArchived] accepts, so it can sort the Archive list.
 */
fun Reminder.archivedSince(today: LocalDate = LocalDate.now()): LocalDateTime? {
    if (!isArchived(today)) return null
    return archivedAt ?: endDate?.plusDays(1)?.atStartOfDay()
}
