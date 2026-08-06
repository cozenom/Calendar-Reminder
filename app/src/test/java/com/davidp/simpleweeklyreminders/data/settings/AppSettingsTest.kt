package com.davidp.simpleweeklyreminders.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure tests for the DataStore mapping — no Context needed, since Preferences is buildable
 * in a JVM test. Covers defaulting and the "unknown stored enum" fallback that guards against
 * a renamed/garbage constant crashing on read.
 */
class AppSettingsTest {

    @Test
    fun parseEnum_valid() {
        assertEquals(ThemeMode.DARK, parseEnum("DARK", ThemeMode.SYSTEM))
    }

    @Test
    fun parseEnum_nullFallsBackToDefault() {
        assertEquals(ThemeMode.SYSTEM, parseEnum(null, ThemeMode.SYSTEM))
    }

    @Test
    fun parseEnum_unknownFallsBackToDefault() {
        assertEquals(WeekStart.MONDAY, parseEnum("NOT_A_DAY", WeekStart.MONDAY))
    }

    @Test
    fun emptyPreferences_givesAllDefaults() {
        assertEquals(AppSettingsState(), emptyPreferences().toAppSettingsState())
    }

    @Test
    fun storedValues_areRead() {
        val prefs = mutablePreferencesOf().apply {
            this[SettingsKeys.THEME_MODE] = ThemeMode.DARK.name
            this[SettingsKeys.DYNAMIC_COLOR] = false
            this[SettingsKeys.WEEK_START] = WeekStart.SUNDAY.name
            this[SettingsKeys.SNOOZE_MINUTES] = 30
        }
        val state = prefs.toAppSettingsState()

        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(false, state.dynamicColor)
        assertEquals(WeekStart.SUNDAY, state.weekStart)
        assertEquals(30, state.snoozeMinutes)
        // Untouched keys still default
        assertEquals(TimeFormatPref.SYSTEM, state.timeFormat)
    }

    @Test
    fun unknownStoredEnum_fallsBackToDefault() {
        val prefs = mutablePreferencesOf().apply {
            this[SettingsKeys.THEME_MODE] = "GARBAGE"
        }
        assertEquals(ThemeMode.SYSTEM, prefs.toAppSettingsState().themeMode)
    }
}
