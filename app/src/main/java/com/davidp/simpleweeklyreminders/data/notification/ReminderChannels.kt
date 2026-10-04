package com.davidp.simpleweeklyreminders.data.notification

import com.davidp.simpleweeklyreminders.data.model.Importance

/** Every per-reminder channel starts with this, so they can all be found and deleted. */
internal const val REMINDER_CHANNEL_PREFIX = "reminder_"

/** One reminder's channels — the trailing "_" keeps reminder 1 from matching reminder 12. */
internal fun reminderChannelPrefix(reminderId: Int): String = "$REMINDER_CHANNEL_PREFIX${reminderId}_"

/**
 * Channel ID for a reminder's custom tone. Derived, not stored.
 * - Android fixes a channel's sound and importance once created, so changing either must
 *   produce a new ID — hence both are hashed in.
 * - Deterministic: picking a previous tone again reuses its ID, and Android restores that
 *   channel's settings (the same tone), so nothing extra piles up.
 */
internal fun reminderChannelId(reminderId: Int, sound: String, importance: Importance): String {
    // String.hashCode is specified by the JVM, so the ID is stable across runs and devices
    val hash = "$sound|${importance.name}".hashCode().toUInt().toString(16)
    return reminderChannelPrefix(reminderId) + hash
}
