package com.davidp.simpleweeklyreminders.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.davidp.simpleweeklyreminders.R

/**
 * DM Sans, bundled as a single variable font (opsz + wght axes). One file covers every
 * weight, so each entry below pins a wght value rather than shipping a separate TTF.
 * Variation settings need API 26; minSdk is 26.
 */
@OptIn(ExperimentalTextApi::class)
private fun dmSans(weight: FontWeight) = Font(
    resId = R.font.dm_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

private val DmSans = FontFamily(
    dmSans(FontWeight.Normal),
    dmSans(FontWeight.Medium),
    dmSans(FontWeight.SemiBold),
    dmSans(FontWeight.Bold)
)

/**
 * The design sets `font-variant-numeric: tabular-nums` on every screen, so figures line up
 * in the day grid, the time column and the chips. Applied to every style rather than the
 * numeric ones only, matching that.
 */
private fun style(
    size: Double,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Double = size * 1.35,
    letterSpacing: TextUnit = 0.sp
) = TextStyle(
    fontFamily = DmSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
    fontFeatureSettings = "tnum"
)

val Typography = Typography(
    // Screen titles ("Calendar", "Reminders") — 28/500, slightly tightened
    headlineMedium = style(28.0, FontWeight.Medium, 34.0, (-0.5).sp),
    // Overlay titles ("Archive", "Settings", "Theme pack")
    headlineSmall = style(22.0, FontWeight.Medium, 28.0, (-0.2).sp),
    // Bottom-sheet titles ("Sort & filter", "New reminder", "Choose an icon")
    titleLarge = style(20.0, FontWeight.Medium, 26.0),
    // Month label ("August 2026"), selected-day heading
    titleMedium = style(17.0, FontWeight.Medium, 22.0),
    // Reminder row title
    titleSmall = style(15.5, FontWeight.Medium, 20.0),

    bodyLarge = style(15.0),
    bodyMedium = style(14.0),
    bodySmall = style(13.0),

    // Time chips, buttons
    labelLarge = style(12.5, FontWeight.Medium, 16.0),
    // Status meta, small counts
    labelMedium = style(12.0, FontWeight.Medium, 16.0),
    // Nav bar labels, legend
    labelSmall = style(11.0, FontWeight.Normal, 14.0)
)

/** Styles the M3 scale has no slot for. Reached as `MaterialTheme.appTypography.sectionLabel`. */
object AppTypography {
    /** Uppercase group heading above a settings/form section — 10/500, widely tracked. */
    val sectionLabel = style(10.0, FontWeight.Medium, 14.0, 0.9.sp)
}
