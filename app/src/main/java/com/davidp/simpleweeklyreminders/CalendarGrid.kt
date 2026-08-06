package com.davidp.simpleweeklyreminders

import com.davidp.simpleweeklyreminders.data.settings.WeekStart

/**
 * Blank cells before day 1 of a month, counted from the chosen first column.
 * [firstDow] is ISO day-of-week (Mon=1 .. Sun=7).
 */
fun leadingBlankCount(firstDow: Int, weekStart: WeekStart): Int = when (weekStart) {
    WeekStart.MONDAY -> firstDow - 1  // Mon->0 .. Sun->6
    WeekStart.SUNDAY -> firstDow % 7  // Sun->0, Mon->1 .. Sat->6
}
