package com.davidp.simpleweeklyreminders.viewmodel

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * How long `ReminderViewModel.now` sleeps after emitting the minute [now] falls in: the time
 * left until the next minute boundary. Never zero — a zero delay would spin the tick loop.
 */
internal fun millisUntilNextMinute(now: LocalDateTime): Long {
    val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
    return Duration.between(now, nextMinute).toMillis().coerceAtLeast(1)
}
