package com.davidp.simpleweeklyreminders.ui.archive

import android.content.Context
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.archivedSince
import com.davidp.simpleweeklyreminders.data.settings.ArchiveSettings
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Reminders that lapsed into the Archive on their own after [lastViewed] — drives the badge
 * on the Archive icon and the launch snackbar. [today] is the shared clock's date, the same
 * one [archived] was filtered with.
 *
 * Manual archives are excluded: the user just did that themselves, so "N archived since you
 * last checked" would be telling them their own news. Only an end date passing while they
 * weren't looking is worth a heads-up.
 */
fun newlyArchivedCount(archived: List<Reminder>, lastViewed: LocalDateTime, today: LocalDate): Int =
    archived.count { it.archivedAt == null && it.archivedSince(today)?.isAfter(lastViewed) == true }

fun newlyArchivedCount(archived: List<Reminder>, context: Context, today: LocalDate): Int =
    newlyArchivedCount(archived, ArchiveSettings.getLastViewed(context), today)
