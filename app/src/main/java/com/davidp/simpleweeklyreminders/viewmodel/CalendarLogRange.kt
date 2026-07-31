package com.davidp.simpleweeklyreminders.viewmodel

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Inclusive datetime bounds for a log query, matching the BETWEEN the log DAO uses. */
internal data class LogRange(val start: LocalDateTime, val end: LocalDateTime)

/**
 * The range of logs the Calendar tab needs while showing [month] with [selectedDate]
 * selected. Padded by one month either side so swiping to a neighbouring page slices logs
 * already in memory instead of opening a query per page, and widened to reach
 * [selectedDate] when the user has swiped away from their selection — that keeps the day
 * list populated without giving it a second observer of the same rows, which is what let
 * the day list and the month grid's pips disagree.
 */
internal fun calendarLogRange(month: YearMonth, selectedDate: LocalDate): LogRange {
    val selectedMonth = YearMonth.from(selectedDate)
    val first = minOf(month.minusMonths(1), selectedMonth)
    val last = maxOf(month.plusMonths(1), selectedMonth)
    return LogRange(first.atDay(1).atStartOfDay(), last.atEndOfMonth().endOfDay())
}

/** Last representable instant of the day, so an inclusive range covers 23:59:59.999…. */
internal fun LocalDate.endOfDay(): LocalDateTime = plusDays(1).atStartOfDay().minusNanos(1)
