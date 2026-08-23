package com.davidp.simpleweeklyreminders.ui.calendar

import com.davidp.simpleweeklyreminders.data.model.OccurrenceStatus
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.statusOf
import java.time.LocalDate
import java.time.LocalDateTime

/** One day's occurrences, in time order, plus the two counts the calendar renders. */
data class DayStatus(
    val segments: List<OccurrenceStatus>,
    val doneCount: Int,
    val hasMissed: Boolean
) {
    val total: Int get() = segments.size
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
