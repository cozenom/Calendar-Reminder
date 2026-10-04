package com.davidp.simpleweeklyreminders.data.notification

import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.SILENT_SOUND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android fixes a channel's sound and importance once created, so the derived ID must change
 * with either — or a new tone would silently keep playing the old one.
 */
class ReminderChannelsTest {

    private val tone = "content://media/internal/audio/media/42"

    @Test
    fun sameInputs_giveSameId() {
        assertEquals(
            reminderChannelId(1, tone, Importance.HIGH),
            reminderChannelId(1, tone, Importance.HIGH)
        )
    }

    @Test
    fun differentTone_givesNewId() {
        assertNotEquals(
            reminderChannelId(1, tone, Importance.HIGH),
            reminderChannelId(1, "content://media/internal/audio/media/43", Importance.HIGH)
        )
    }

    @Test
    fun differentImportance_givesNewId() {
        assertNotEquals(
            reminderChannelId(1, tone, Importance.HIGH),
            reminderChannelId(1, tone, Importance.MEDIUM)
        )
    }

    @Test
    fun silent_differsFromATone() {
        assertNotEquals(
            reminderChannelId(1, tone, Importance.HIGH),
            reminderChannelId(1, SILENT_SOUND, Importance.HIGH)
        )
    }

    @Test
    fun id_startsWithItsReminderPrefix() {
        assertTrue(reminderChannelId(12, tone, Importance.HIGH).startsWith(reminderChannelPrefix(12)))
    }

    @Test
    fun prefix_doesNotMatchAnotherReminderSharingDigits() {
        // Deleting reminder 1's channels must not sweep reminder 12's
        assertFalse(reminderChannelId(12, tone, Importance.HIGH).startsWith(reminderChannelPrefix(1)))
    }
}
