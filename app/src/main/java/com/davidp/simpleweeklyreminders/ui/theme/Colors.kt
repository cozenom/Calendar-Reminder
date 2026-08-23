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

val LightReminderColors = ReminderColors(
    done = Color(0xFF2F5D6B),               // accent
    onDone = Color(0xFFFFFFFF),
    doneWash = Color(0xFFE3EEF2),
    neutral = Color(0xFFF2F0EA),            // group surface
    track = Color(0x221C1B18),              // ink @ 13%
    missed = Color(0xFF1C1B18),             // ink
    hairline = Color(0x2E1C1B18),           // ink @ 18%

    importanceLow = Color(0xFF3E6E9E),
    importanceMedium = Color(0xFF9A6714),
    importanceHigh = Color(0xFFB3382B),
)

val DarkReminderColors = ReminderColors(
    done = Color(0xFF93CEDF),
    onDone = Color(0xFF06333F),
    doneWash = Color(0xFF1E3B44),
    neutral = Color(0xFF1F1E1A),
    track = Color(0x26EAE7DF),              // ink @ 15%
    missed = Color(0xFFEAE7DF),
    hairline = Color(0x38EAE7DF),           // ink @ 22%

    importanceLow = Color(0xFF8FBEE0),
    importanceMedium = Color(0xFFE5BC72),
    importanceHigh = Color(0xFFF0958A),
)

val LocalReminderColors = staticCompositionLocalOf { LightReminderColors }
