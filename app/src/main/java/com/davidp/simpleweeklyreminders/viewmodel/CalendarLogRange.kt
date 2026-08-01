package com.davidp.simpleweeklyreminders.viewmodel

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Inclusive datetime bounds for a log query, matching the BETWEEN the log DAO uses. */
internal data class LogRange(val start: LocalDateTime, val end: LocalDateTime)

/**
 * Log range for the Calendar tab: [month] padded a month either side so a swipe slices
 * logs already in memory, widened to reach [selectedDate] when the user has swiped away
 * from their selection. One observer for both the grid and the day list.
 */
internal fun calendarLogRange(month: YearMonth, selectedDate: LocalDate): LogRange {
    val selectedMonth = YearMonth.from(selectedDate)
    val first = minOf(month.minusMonths(1), selectedMonth)
    val last = maxOf(month.plusMonths(1), selectedMonth)
    return LogRange(first.atDay(1).atStartOfDay(), last.atEndOfMonth().endOfDay())
}

/** Last representable instant of the day, so an inclusive range covers 23:59:59.999…. */
internal fun LocalDate.endOfDay(): LocalDateTime = plusDays(1).atStartOfDay().minusNanos(1)
