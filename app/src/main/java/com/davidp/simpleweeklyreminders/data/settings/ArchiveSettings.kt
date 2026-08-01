package com.davidp.simpleweeklyreminders.data.settings

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDateTime

/**
 * Tracks when the user last viewed the Archive screen, so the archive badge
 * and post-lapse snackbar can tell "newly archived" reminders apart from ones
 * already seen.
 */
object ArchiveSettings {
    private const val PREFS_NAME = "archive_prefs"
    private const val KEY_LAST_VIEWED = "last_viewed_at"

    /**
     * Seeds a baseline at "now" on first call so pre-existing archives don't flood in as
     * new. Must persist — a recomputed "now" would always postdate every archive event,
     * so the badge could never move.
     */
    fun getLastViewed(context: Context): LocalDateTime {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_LAST_VIEWED, null)
        if (stored != null) return LocalDateTime.parse(stored)

        val now = LocalDateTime.now()
        prefs.edit { putString(KEY_LAST_VIEWED, now.toString()) }
        return now
    }

    fun markViewedNow(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_LAST_VIEWED, LocalDateTime.now().toString()) }
    }
}
