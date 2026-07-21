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

    /** No baseline yet (never opened Archive): treat as "now" so nothing floods in as new. */
    fun getLastViewed(context: Context): LocalDateTime {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_VIEWED, null)
        return stored?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
    }

    fun markViewedNow(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_LAST_VIEWED, LocalDateTime.now().toString()) }
    }
}
