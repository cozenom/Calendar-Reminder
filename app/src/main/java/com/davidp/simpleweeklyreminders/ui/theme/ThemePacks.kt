package com.davidp.simpleweeklyreminders.ui.theme

import androidx.compose.ui.graphics.Color
import com.davidp.simpleweeklyreminders.data.settings.ThemePack

/**
 * One pack's accent family. Packs change the accent only — the warm neutral paper surfaces
 * are shared, so a pack is six colours rather than a whole second scheme.
 *
 * [deep] and [pale] are the ends of the ramp: [deep] is the readable ink on a light container
 * and the content colour on a dark-theme filled accent; [pale] is its mirror.
 */
data class ThemePackTones(
    val label: String,
    val description: String,
    val accentLight: Color,
    val containerLight: Color,
    val accentDark: Color,
    val containerDark: Color,
    val deep: Color,
    val pale: Color
)

/**
 * The eight packs. `accentLight` / `accentDark` / `containerLight` come from the design;
 * the remaining three are the same mechanical steps applied to each hue (a near-black tone
 * for `deep`, a near-white one for `pale`, and a dark muted container).
 */
val ThemePackPalette: Map<ThemePack, ThemePackTones> = mapOf(
    ThemePack.PAPER to ThemePackTones(
        label = "Paper",
        description = "Warm neutral · slate teal",
        accentLight = Color(0xFF2F5D6B), containerLight = Color(0xFFCDE7EF),
        accentDark = Color(0xFF93CEDF), containerDark = Color(0xFF1E4550),
        deep = Color(0xFF06333F), pale = Color(0xFFBFE6F2)
    ),
    ThemePack.MOSS to ThemePackTones(
        label = "Moss",
        description = "Warm neutral · deep green",
        accentLight = Color(0xFF4F7A52), containerLight = Color(0xFFDFEFDD),
        accentDark = Color(0xFFA9D4A6), containerDark = Color(0xFF24422A),
        deep = Color(0xFF0C2A12), pale = Color(0xFFC9E6C4)
    ),
    ThemePack.CLAY to ThemePackTones(
        label = "Clay",
        description = "Warm · terracotta",
        accentLight = Color(0xFFB0603F), containerLight = Color(0xFFF7E2D6),
        accentDark = Color(0xFFE8A886), containerDark = Color(0xFF4E2A19),
        deep = Color(0xFF3A1B0C), pale = Color(0xFFF5C9B2)
    ),
    ThemePack.INK to ThemePackTones(
        label = "Ink",
        description = "Near-monochrome",
        accentLight = Color(0xFF3A3A38), containerLight = Color(0xFFE2E1DD),
        accentDark = Color(0xFFC9C7C1), containerDark = Color(0xFF3A3A38),
        deep = Color(0xFF1A1A18), pale = Color(0xFFE2E1DD)
    ),
    ThemePack.INDIGO to ThemePackTones(
        label = "Indigo",
        description = "Cool grey · deep blue",
        accentLight = Color(0xFF3F4C93), containerLight = Color(0xFFDDE1FF),
        accentDark = Color(0xFFB5BEFF), containerDark = Color(0xFF2A3268),
        deep = Color(0xFF131A47), pale = Color(0xFFD5DAFF)
    ),
    ThemePack.PLUM to ThemePackTones(
        label = "Plum",
        description = "Warm grey · violet",
        accentLight = Color(0xFF6D4685), containerLight = Color(0xFFEEDFF6),
        accentDark = Color(0xFFDCB6EE), containerDark = Color(0xFF452A56),
        deep = Color(0xFF2B1638), pale = Color(0xFFEBD4F5)
    ),
    ThemePack.DUNE to ThemePackTones(
        label = "Dune",
        description = "Sand · bronze",
        accentLight = Color(0xFF7A5A32), containerLight = Color(0xFFF2E3CE),
        accentDark = Color(0xFFE3BE8A), containerDark = Color(0xFF4A3720),
        deep = Color(0xFF2E2010), pale = Color(0xFFF0D9B8)
    ),
    ThemePack.TIDE to ThemePackTones(
        label = "Tide",
        description = "Cool · deep sea blue",
        accentLight = Color(0xFF255E86), containerLight = Color(0xFFD3E6F5),
        accentDark = Color(0xFF9CCBEB), containerDark = Color(0xFF113E5C),
        deep = Color(0xFF052738), pale = Color(0xFFC4E0F4)
    )
)

fun tonesFor(pack: ThemePack): ThemePackTones =
    ThemePackPalette[pack] ?: ThemePackPalette.getValue(ThemePack.PAPER)
