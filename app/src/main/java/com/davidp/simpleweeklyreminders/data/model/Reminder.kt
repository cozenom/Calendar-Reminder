package com.davidp.simpleweeklyreminders.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class ReminderType { SPECIFIC_DAYS, EVERY_N_DAYS, ONE_TIME }

// Declared low-to-high so ordinal comparisons sort correctly at read time — never persist
// the ordinal itself (stored as TEXT via Converters), so inserting a level later (e.g.
// between MEDIUM and HIGH) needs a new case here, not a data migration.
enum class Importance { LOW, MEDIUM, HIGH }

/** How the Reminders tab list is ordered. MANUAL is the drag-reorderable default. */
enum class SortMode { MANUAL, DATE_ADDED, IMPORTANCE, NEXT_OCCURRENCE }

enum class SortDirection { ASCENDING, DESCENDING }

/** Sensible starting direction when a sort mode is first selected (still user-toggleable). */
fun SortMode.defaultDirection(): SortDirection = when (this) {
    SortMode.IMPORTANCE -> SortDirection.DESCENDING // High first reads as "most important on top"
    SortMode.MANUAL, SortMode.DATE_ADDED, SortMode.NEXT_OCCURRENCE -> SortDirection.ASCENDING
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
    val completedColor: String? = null,
    val icon: String? = null,
    val dayInterval: Int? = null,        // interval count, only meaningful when reminderType == EVERY_N_DAYS
    val reminderType: ReminderType = ReminderType.SPECIFIC_DAYS,
    val isActive: Boolean = true,        // false = paused, skipped in scheduling
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val sortOrder: Int = 0,               // user-defined drag order, fallback = createdAt
    val archivedAt: LocalDateTime? = null, // set on manual archive(), cleared on restore(); null for auto-lapsed (see archivedSince)
    // Default HIGH preserves today's actual notification behavior (sticky, swipe = snooze)
    // for every existing reminder until the importance-driven behavior work (2.2) ships.
    val importance: Importance = Importance.HIGH
)

/** Whether this reminder's schedule includes the given date (ignores isActive). */
fun Reminder.isScheduledOn(date: LocalDate): Boolean {
    if (date < startDate) return false
    endDate?.let { if (date > it) return false }
    return when (reminderType) {
        ReminderType.EVERY_N_DAYS -> ChronoUnit.DAYS.between(startDate, date) % (dayInterval ?: 1) == 0L
        ReminderType.SPECIFIC_DAYS, ReminderType.ONE_TIME -> reminderDays.contains(date.dayOfWeek.value)
    }
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
fun Reminder.isArchived(today: LocalDate = LocalDate.now()): Boolean = endDate != null && endDate < today

/**
 * The moment this reminder became archived, or null if it isn't archived. Prefers the
 * precise [archivedAt] timestamp (manual archive); auto-lapse has no event to hook, so
 * falls back to day-granularity (day after endDate) — the earliest moment it's provably true.
 */
fun Reminder.archivedSince(today: LocalDate = LocalDate.now()): LocalDateTime? {
    if (!isArchived(today)) return null
    return archivedAt ?: endDate?.plusDays(1)?.atStartOfDay()
}
