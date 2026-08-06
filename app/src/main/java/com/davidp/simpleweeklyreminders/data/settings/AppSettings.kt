package com.davidp.simpleweeklyreminders.data.settings

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// User-facing settings. Theme/dynamic color are UI-only; time/date/week are read
// wherever those values are formatted; snooze + missedSummary gate behaviour.
enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class TimeFormatPref { SYSTEM, H12, H24 }
enum class DateFormatPref { SYSTEM, MONTH_FIRST, DAY_FIRST }
enum class WeekStart { SUNDAY, MONDAY }

/** Immutable snapshot the whole app reads; defaults are the pre-settings behaviour. */
data class AppSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val timeFormat: TimeFormatPref = TimeFormatPref.SYSTEM,
    val dateFormat: DateFormatPref = DateFormatPref.SYSTEM,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val missedSummaryEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
    }
}

// One DataStore file per process; the delegate must be top-level (a single instance).
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Sole gateway to the settings DataStore. [flow] is the reactive read the UI/theme
 * observe; [read] is the one-shot suspend read for background coroutines (the
 * notification receiver) and the cold-start theme value. Writes are suspend + atomic.
 */
class SettingsRepository(private val context: Context) {

    val flow: Flow<AppSettingsState> = context.settingsDataStore.data.map { it.toAppSettingsState() }

    suspend fun read(): AppSettingsState = context.settingsDataStore.data.first().toAppSettingsState()

    suspend fun setThemeMode(mode: ThemeMode) =
        edit { it[SettingsKeys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) =
        edit { it[SettingsKeys.DYNAMIC_COLOR] = enabled }

    suspend fun setTimeFormat(pref: TimeFormatPref) =
        edit { it[SettingsKeys.TIME_FORMAT] = pref.name }

    suspend fun setDateFormat(pref: DateFormatPref) =
        edit { it[SettingsKeys.DATE_FORMAT] = pref.name }

    suspend fun setWeekStart(start: WeekStart) =
        edit { it[SettingsKeys.WEEK_START] = start.name }

    suspend fun setSnoozeMinutes(minutes: Int) =
        edit { it[SettingsKeys.SNOOZE_MINUTES] = minutes }

    suspend fun setMissedSummaryEnabled(enabled: Boolean) =
        edit { it[SettingsKeys.MISSED_SUMMARY] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}

// internal (not private) so the mapping + fallback can be unit-tested without a Context —
// Preferences itself is pure and buildable in a JVM test via mutablePreferencesOf(...).
internal object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val TIME_FORMAT = stringPreferencesKey("time_format")
    val DATE_FORMAT = stringPreferencesKey("date_format")
    val WEEK_START = stringPreferencesKey("week_start")
    val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
    val MISSED_SUMMARY = booleanPreferencesKey("missed_summary_enabled")
}

internal fun Preferences.toAppSettingsState() = AppSettingsState(
    themeMode = parseEnum(this[SettingsKeys.THEME_MODE], ThemeMode.SYSTEM),
    dynamicColor = this[SettingsKeys.DYNAMIC_COLOR] ?: true,
    timeFormat = parseEnum(this[SettingsKeys.TIME_FORMAT], TimeFormatPref.SYSTEM),
    dateFormat = parseEnum(this[SettingsKeys.DATE_FORMAT], DateFormatPref.SYSTEM),
    weekStart = parseEnum(this[SettingsKeys.WEEK_START], WeekStart.MONDAY),
    snoozeMinutes = this[SettingsKeys.SNOOZE_MINUTES] ?: AppSettingsState.DEFAULT_SNOOZE_MINUTES,
    missedSummaryEnabled = this[SettingsKeys.MISSED_SUMMARY] ?: true,
)

// Fall back to the default if the stored string is missing or no longer a valid constant.
internal inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T =
    value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

/** SYSTEM defers to the device 12/24h setting; H12/H24 force it. */
fun TimeFormatPref.is24Hour(context: Context): Boolean = when (this) {
    TimeFormatPref.H24 -> true
    TimeFormatPref.H12 -> false
    TimeFormatPref.SYSTEM -> DateFormat.is24HourFormat(context)
}

/** The single display pattern all time text goes through. */
fun TimeFormatPref.timePattern(context: Context): String =
    if (is24Hour(context)) "HH:mm" else "h:mm a"

/** SYSTEM reads the device locale's day/month order; the others force it. */
fun DateFormatPref.isDayFirst(context: Context): Boolean = when (this) {
    DateFormatPref.DAY_FIRST -> true
    DateFormatPref.MONTH_FIRST -> false
    DateFormatPref.SYSTEM -> {
        val order = DateFormat.getDateFormatOrder(context)
        order.indexOf('d') < order.indexOf('M')
    }
}

// Three granularities used across the app; ordering flips with the day-first setting.
fun DateFormatPref.fullDatePattern(context: Context): String =
    if (isDayFirst(context)) "EEEE, d MMMM yyyy" else "EEEE, MMMM d, yyyy"

fun DateFormatPref.datePattern(context: Context): String =
    if (isDayFirst(context)) "d MMM yyyy" else "MMM d, yyyy"

fun DateFormatPref.dateNoYearPattern(context: Context): String =
    if (isDayFirst(context)) "d MMM" else "MMM d"
