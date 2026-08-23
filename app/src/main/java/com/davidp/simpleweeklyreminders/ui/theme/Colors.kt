package com.davidp.simpleweeklyreminders.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status and importance colors that M3's scheme has no slot for.
 *
 * The palette deliberately carries no green/red pair. Completion is shown by *fill*
 * (accent = done, faint track = pending, hollow outline = missed) so the three states stay
 * distinguishable without relying on hue — green-red is the hardest pair for red-green color
 * blindness, and it read as an alarm on a screen that is mostly routine.
 */
data class ReminderColors(
    /** Filled segment / chip for a completed occurrence. */
    val done: Color,
    val onDone: Color,
    /** Soft accent wash behind a completed row's icon. */
    val doneWash: Color,
    /** Neutral group fill for pending and missed rows, and for inert chips. */
    val neutral: Color,
    /** Faint bar behind the day-cell segments; also an unfilled (pending) segment. */
    val track: Color,
    /** Ink for a missed segment's outline, its corner notch, and the missed glyph. */
    val missed: Color,
    /** Hairline for chip borders and the pending glyph. */
    val hairline: Color,

    // Importance chevrons and the form's selector buttons. Blue/amber/red, not
    // green/amber/red. Chevron count is the primary signal (see ImportanceChevrons).
    val importanceLow: Color,
    val importanceMedium: Color,
    val importanceHigh: Color,
)

/**
 * Builds the status palette around whatever accent the active scheme resolved to — a theme
 * pack, or a dynamic wallpaper scheme. Only `done`/`onDone`/`doneWash` follow the accent; the
 * neutral, track, missed-ink and importance tints are the same in every pack.
 */
fun reminderColorsFor(accent: Color, onAccent: Color, dark: Boolean) = ReminderColors(
    done = accent,
    onDone = onAccent,
    doneWash = accent.copy(alpha = if (dark) 0.22f else 0.16f),
    neutral = if (dark) Color(0xFF1F1E1A) else Color(0xFFF2F0EA),
    track = if (dark) Color(0x26EAE7DF) else Color(0x221C1B18),   // ink @ 15% / 13%
    missed = if (dark) Color(0xFFEAE7DF) else Color(0xFF1C1B18),  // ink
    hairline = if (dark) Color(0x38EAE7DF) else Color(0x2E1C1B18), // ink @ 22% / 18%

    importanceLow = if (dark) Color(0xFF8FBEE0) else Color(0xFF3E6E9E),
    importanceMedium = if (dark) Color(0xFFE5BC72) else Color(0xFF9A6714),
    importanceHigh = if (dark) Color(0xFFF0958A) else Color(0xFFB3382B),
)

/** Fallback for previews and any composable read outside CalendarAppTheme. */
val LightReminderColors = reminderColorsFor(
    accent = Color(0xFF2F5D6B),
    onAccent = Color(0xFFFFFFFF),
    dark = false
)

val LocalReminderColors = staticCompositionLocalOf { LightReminderColors }
