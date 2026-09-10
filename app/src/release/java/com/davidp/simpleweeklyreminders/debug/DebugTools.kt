package com.davidp.simpleweeklyreminders.debug

import android.content.Context

/**
 * Release stub of the debug-only dev tools (real version in src/debug). Keeps the same API
 * so main code compiles; ENABLED = false lets R8 strip the Developer section entirely.
 */
object DebugTools {
    const val ENABLED = false

    suspend fun seedSampleReminders(context: Context): Int = 0

    suspend fun fireTestNotifications(context: Context) = Unit

    suspend fun addReminderDueInOneMinute(context: Context) = Unit

    suspend fun clearAllReminders(context: Context) = Unit
}
