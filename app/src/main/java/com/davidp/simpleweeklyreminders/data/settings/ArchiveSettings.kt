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
     * No baseline yet (never opened Archive): seed one at "now" and persist it, so
     * pre-existing archived reminders don't flood in as new but anything archived
     * after this point does. Must persist rather than just returning "now" each call
     * — a recomputed-every-time fallback would always sit later than any archive
     * event, so nothing could ever be "after" it and the badge would never move.
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
