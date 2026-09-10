package com.davidp.simpleweeklyreminders.ui.calendar

import com.davidp.simpleweeklyreminders.data.model.OccurrenceStatus
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.isArchived
import com.davidp.simpleweeklyreminders.data.model.isScheduledOn
import com.davidp.simpleweeklyreminders.data.model.statusOf
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Future occurrences the calendar should show that have no materialized log yet, as synthetic
 * ReminderLog rows (id = 0, uncompleted). We only materialize a short forward window (see
 * ReminderRepository), so the calendar computes the rest here instead of storing a year of
 * rows — statusOf() renders a future uncompleted log as PENDING, so these fold and render
 * exactly like real ones (todo #13).
 *
 * TODAY AND LATER ONLY. A past scheduled slot with no log is "not tracked", not missed —
 * synthesising it would invent a failure. Past days stay real-logs-only.
 *
 * Deduped against [existingLogs] by (reminderId, exact time), so a slot that already has a log
 * (incl. one completed early) keeps its real row.
 */
fun syntheticFutureLogs(
    reminders: List<Reminder>,
    existingLogs: List<ReminderLog>,
    range: ClosedRange<LocalDate>,
    today: LocalDate
): List<ReminderLog> {
    val start = maxOf(range.start, today)
    if (start > range.endInclusive) return emptyList()
    val logged = existingLogs.mapTo(HashSet()) { it.reminderId to it.logDateTime }

    val out = mutableListOf<ReminderLog>()
    for (reminder in reminders) {
        // isArchived too: a manual archive keeps its endDate, so the schedule alone won't stop it
        if (!reminder.isActive || reminder.isArchived(today)) continue
        val times = reminder.reminderTimes.distinct()
        var date = start
        while (date <= range.endInclusive) {
            if (reminder.isScheduledOn(date)) {
                for (time in times) {
                    val dateTime = LocalDateTime.of(date, time)
                    if ((reminder.id to dateTime) !in logged) {
                        out += ReminderLog(reminderId = reminder.id, title = reminder.title, logDateTime = dateTime)
                    }
                }
            }
            date = date.plusDays(1)
        }
    }
    return out
}

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
