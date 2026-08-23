package com.davidp.simpleweeklyreminders.ui.calendar

import com.davidp.simpleweeklyreminders.data.model.OccurrenceStatus
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.statusOf
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One occurrence in a day's bar: how it stands, and which reminder's colour it should take.
 *
 * [colorKey] is the owning reminder's `color` (a swatch key, see ReminderPalette). Null means
 * "follow the theme accent", which is also what every segment does while the per-reminder
 * colours setting is off.
 */
data class DaySegment(
    val status: OccurrenceStatus,
    val colorKey: String? = null
)

/** One day's occurrences, in time order, plus the two counts the calendar renders. */
data class DayStatus(
    val segments: List<DaySegment>,
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
 *
 * [colorKeyByReminder] maps reminder id to its colour key. Logs don't carry the colour, and a
 * log can outlive edits to its reminder, so it is looked up at render time rather than stored.
 */
fun dayStatuses(
    logs: List<ReminderLog>,
    now: LocalDateTime,
    colorKeyByReminder: Map<Int, String?> = emptyMap()
): Map<LocalDate, DayStatus> =
    logs.groupBy { it.logDateTime.toLocalDate() }
        .mapValues { (_, dayLogs) ->
            val segments = dayLogs.sortedBy { it.logDateTime }.map { log ->
                DaySegment(
                    status = statusOf(log, now),
                    colorKey = colorKeyByReminder[log.reminderId]
                )
            }
            DayStatus(
                segments = segments,
                doneCount = segments.count { it.status == OccurrenceStatus.DONE },
                hasMissed = segments.any { it.status == OccurrenceStatus.MISSED }
            )
        }
