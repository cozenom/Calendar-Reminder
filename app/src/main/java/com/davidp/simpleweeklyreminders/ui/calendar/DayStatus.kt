package com.davidp.simpleweeklyreminders.ui.calendar

import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * How one scheduled occurrence stands right now.
 *
 * Derived, never stored — a log row only carries `completed` and `snoozedUntil`. See
 * docs/architecture.md.
 */
enum class OccurrenceStatus { DONE, PENDING, MISSED }

/** One day's occurrences, in time order, plus the two counts the calendar renders. */
data class DayStatus(
    val segments: List<OccurrenceStatus>,
    val doneCount: Int,
    val hasMissed: Boolean
) {
    val total: Int get() = segments.size
}

/**
 * A pending snooze counts as PENDING, not MISSED: the user deferred it on purpose and the
 * notification will fire again, so flagging the day as missed would be wrong. Once
 * `snoozedUntil` itself passes without completion, it becomes MISSED like anything else.
 */
fun statusOf(log: ReminderLog, now: LocalDateTime): OccurrenceStatus {
    val snoozedUntil = log.snoozedUntil
    return when {
        log.completed -> OccurrenceStatus.DONE
        snoozedUntil != null && snoozedUntil.isAfter(now) -> OccurrenceStatus.PENDING
        log.logDateTime.isBefore(now) -> OccurrenceStatus.MISSED
        else -> OccurrenceStatus.PENDING
    }
}

/**
 * Folds a month's logs into one entry per day. Called once per calendar page instead of
 * filtering the whole list inside each of the 42 day cells.
 *
 * Days with no occurrences are absent from the map rather than present-and-empty, so a cell
 * can tell "nothing scheduled" from "scheduled, none done" and skip drawing a bar entirely.
 */
fun dayStatuses(logs: List<ReminderLog>, now: LocalDateTime): Map<LocalDate, DayStatus> =
    logs.groupBy { it.logDateTime.toLocalDate() }
        .mapValues { (_, dayLogs) ->
            val segments = dayLogs.sortedBy { it.logDateTime }.map { statusOf(it, now) }
            DayStatus(
                segments = segments,
                doneCount = segments.count { it == OccurrenceStatus.DONE },
                hasMissed = segments.any { it == OccurrenceStatus.MISSED }
            )
        }
