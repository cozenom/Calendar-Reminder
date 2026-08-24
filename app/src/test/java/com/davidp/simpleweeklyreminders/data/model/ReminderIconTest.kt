package com.davidp.simpleweeklyreminders.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The icon set is defined twice: `ReminderIconCategories` (Compose ImageVectors, for the UI)
 * and `iconDrawableRes` (XML drawables, for notifications — a notification can't render an
 * ImageVector). Nothing in the compiler ties the two key lists together, and a key present in
 * one but not the other fails silently: the notification quietly falls back to the generic
 * bell. These tests are the only thing holding them in sync.
 */
class ReminderIconTest {

    private val allOptions = AllReminderIcons

    @Test
    fun `every picker icon has a notification drawable`() {
        val missing = allOptions.map { it.key }.filter { iconDrawableRes(it) == null }
        assertTrue(
            "Icon keys with no drawable in IconDrawableRes.kt (their notifications would " +
                "fall back to the generic bell): $missing",
            missing.isEmpty()
        )
    }

    @Test
    fun `icon keys are unique`() {
        val keys = allOptions.map { it.key }
        val duplicates = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("Duplicate icon keys: $duplicates", duplicates.isEmpty())
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `the default icon key resolves`() {
        assertEquals(DEFAULT_ICON_KEY, iconFromKey(DEFAULT_ICON_KEY).key)
        assertTrue(iconDrawableRes(DEFAULT_ICON_KEY) != null)
    }

    @Test
    fun `an unknown or null key falls back to the first icon`() {
        val fallback = AllReminderIcons.first()
        assertSame(fallback, iconFromKey(null))
        assertSame(fallback, iconFromKey("noSuchIcon"))
    }

    @Test
    fun `iconFromKey returns the matching option`() {
        allOptions.forEach { option ->
            assertSame("iconFromKey(${option.key}) should round-trip", option, iconFromKey(option.key))
        }
    }
}
