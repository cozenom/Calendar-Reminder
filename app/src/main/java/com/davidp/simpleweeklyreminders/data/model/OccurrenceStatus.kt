package com.davidp.simpleweeklyreminders.data.model

import java.time.LocalDateTime

/**
 * How one scheduled occurrence stands right now.
 *
 * Derived, never stored — a log row only carries `completed` and `snoozedUntil`. This is the
 * single definition of those three states; the calendar and the missed-summary notification
 * both go through [statusOf] so they can't drift apart. See docs/architecture.md.
 */
enum class OccurrenceStatus { DONE, PENDING, MISSED }

/**
 * A pending snooze counts as PENDING, not MISSED: the user deferred it on purpose and the
 * notification will fire again, so calling it missed would be wrong. Once `snoozedUntil`
 * itself passes without completion, it becomes MISSED like anything else.
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

/** A set of occurrences tallied by [statusOf]. Used for the archive row's done/missed line. */
data class OccurrenceCounts(val done: Int, val missed: Int, val pending: Int) {
    val total: Int get() = done + missed + pending
}

fun countOutcomes(logs: List<ReminderLog>, now: LocalDateTime): OccurrenceCounts {
    var done = 0
    var missed = 0
    var pending = 0
    for (log in logs) when (statusOf(log, now)) {
        OccurrenceStatus.DONE -> done++
        OccurrenceStatus.MISSED -> missed++
        OccurrenceStatus.PENDING -> pending++
    }
    return OccurrenceCounts(done, missed, pending)
}
