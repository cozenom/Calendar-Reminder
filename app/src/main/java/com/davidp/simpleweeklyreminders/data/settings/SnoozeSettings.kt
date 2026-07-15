package com.davidp.simpleweeklyreminders.data.settings

import android.content.Context
import androidx.core.content.edit

/**
 * Single source of truth for the snooze duration. All snooze code must read
 * the duration through here so a future settings screen only has to call
 * [setSnoozeMinutes].
 */
object SnoozeSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
    private const val DEFAULT_SNOOZE_MINUTES = 10

    fun getSnoozeMinutes(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
    }

    fun setSnoozeMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_SNOOZE_MINUTES, minutes) }
    }
}
