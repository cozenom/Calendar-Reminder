package com.davidp.simpleweeklyreminders.data.model

import java.time.LocalDateTime

/** The next date-time this reminder is scheduled to fire, or null if none (paused/ended). */
fun nextOccurrence(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
    if (!reminder.isActive || reminder.reminderTimes.isEmpty()) return null

    val sortedTimes = reminder.reminderTimes.sorted()
    var date = maxOf(reminder.startDate, now.toLocalDate())
    val endDate = reminder.endDate ?: date.plusYears(1)

    while (date <= endDate) {
        if (reminder.isScheduledOn(date)) {
            for (time in sortedTimes) {
                val dateTime = LocalDateTime.of(date, time)
                if (dateTime > now) return dateTime
            }
        }
        date = date.plusDays(1)
    }
    return null
}

/**
 * Orders reminders for the given [SortMode] and [direction]. MANUAL ignores direction — the
 * list is already in `sortOrder ASC, createdAt ASC` order from the DB query, and drag order
 * has no "reverse". IMPORTANCE and NEXT_OCCURRENCE use a stable sort so MANUAL order still
 * breaks ties (e.g. same importance, or both paused).
 */
fun List<Reminder>.sortedFor(
    mode: SortMode,
    direction: SortDirection = mode.defaultDirection(),
    now: LocalDateTime = LocalDateTime.now()
): List<Reminder> {
    if (mode == SortMode.MANUAL) return this

    val ascending = when (mode) {
        SortMode.DATE_ADDED -> sortedBy { it.createdAt }
        SortMode.IMPORTANCE -> sortedBy { it.importance.ordinal }
        SortMode.NEXT_OCCURRENCE -> sortedWith(compareBy(nullsLast()) { nextOccurrence(it, now) })
        SortMode.MANUAL -> this
    }
    if (direction == SortDirection.ASCENDING) return ascending

    // Descending: reverse, but keep "no next occurrence" (paused/lapsed) pinned at the end
    // rather than flipping to the front — those reminders aren't meaningfully "latest",
    // they're just not applicable to this sort.
    return if (mode == SortMode.NEXT_OCCURRENCE) {
        val (withOccurrence, without) = ascending.partition { nextOccurrence(it, now) != null }
        withOccurrence.reversed() + without
    } else {
        ascending.reversed()
    }
}
