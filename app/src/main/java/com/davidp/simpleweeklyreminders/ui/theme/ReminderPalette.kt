package com.davidp.simpleweeklyreminders.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Optional per-reminder accent, off unless the user turns on "Per-reminder colours".
 *
 * Stored on `Reminder.color` as the [key], not a hex string: each swatch needs a different
 * tone in light and dark (a hue readable on paper is muddy on near-black), and a key lets
 * both be looked up here rather than baked into the row at save time.
 */
data class ReminderSwatch(
    val key: String,
    val label: String,
    val light: Color,
    val dark: Color
)

/** The design's per-reminder hues, each paired with a dark-theme tone. */
val ReminderSwatches = listOf(
    ReminderSwatch("teal", "Teal", Color(0xFF2E7D8F), Color(0xFF7FC6D8)),
    ReminderSwatch("indigo", "Indigo", Color(0xFF4C5BA6), Color(0xFFB5BEFF)),
    ReminderSwatch("plum", "Plum", Color(0xFF8C5B96), Color(0xFFD4A9DC)),
    ReminderSwatch("moss", "Moss", Color(0xFF4F7A52), Color(0xFFA9D4A6)),
    ReminderSwatch("clay", "Clay", Color(0xFFB0603F), Color(0xFFE8A886)),
    ReminderSwatch("amber", "Amber", Color(0xFFB4791F), Color(0xFFE5BC72))
)

fun swatchFor(key: String?): ReminderSwatch? =
    key?.let { k -> ReminderSwatches.firstOrNull { it.key == k } }

/**
 * The accent a reminder should draw itself in: its own swatch when the feature is on and one
 * is set, otherwise the theme's primary. Reminders saved with a colour keep it in the database
 * while the setting is off — they just render in the theme accent until it's turned back on.
 */
@Composable
@ReadOnlyComposable
fun reminderAccent(colorKey: String?): Color {
    if (!LocalAppSettings.current.perReminderColors) return MaterialTheme.colorScheme.primary
    val swatch = swatchFor(colorKey) ?: return MaterialTheme.colorScheme.primary
    return if (LocalIsDarkTheme.current) swatch.dark else swatch.light
}

/**
 * Readable content colour on a filled [accent]. Picked from the fill's own luminance rather
 * than a fixed onPrimary: the dark-theme tones are as light as their light-theme counterparts
 * are dark, so a theme-fixed choice would wash out in one of the two.
 */
fun onReminderAccent(accent: Color): Color =
    if (accent.luminance() > 0.5f) Color(0xFF1C1B18) else Color(0xFFFFFFFF)

/** Soft fill behind an icon in the reminder's own accent. */
fun reminderWash(accent: Color): Color = accent.copy(alpha = 0.16f)
