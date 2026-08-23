package com.davidp.simpleweeklyreminders.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.settings.AppSettingsState
import com.davidp.simpleweeklyreminders.data.settings.ThemeMode

object AppShapes {
    val small = RoundedCornerShape(12.dp)       // Day cells, small chips
    val medium = RoundedCornerShape(16.dp)      // Group containers, buttons (most common)
    val large = RoundedCornerShape(20.dp)       // Cards
    val extraLarge = RoundedCornerShape(28.dp)  // Bottom sheets, time picker
}

object AppDimensions {
    // Spacing
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp

    // Indicator dots
    val indicatorDotLarge = 8.dp

    // Button sizes
    val frequencyButtonWidth = 48.dp
}

/** Custom theme values off MaterialTheme, e.g. `MaterialTheme.appShapes.medium`. */
val MaterialTheme.appShapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = AppShapes

val MaterialTheme.dimensions: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = AppDimensions

val MaterialTheme.appTypography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = AppTypography

val MaterialTheme.reminderColors: ReminderColors
    @Composable
    @ReadOnlyComposable
    get() = LocalReminderColors.current

/** Reactive settings snapshot for any composable that formats times/dates. */
val LocalAppSettings = staticCompositionLocalOf { AppSettingsState() }

/**
 * Whether the resolved theme is dark. Not the same as `isSystemInDarkTheme()` — the user can
 * override the system with the Theme setting. Read it when a colour has to be picked outside
 * the M3 scheme, e.g. per-reminder swatches.
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * "Paper" — warm neutral paper with a slate-teal accent. The app's own identity, used unless
 * the user opts into wallpaper colors. Further theme packs are planned; see redesign.txt.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2F5D6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE7EF),
    onPrimaryContainer = Color(0xFF06333F),

    secondary = Color(0xFF2F5D6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3EEF2),
    onSecondaryContainer = Color(0xFF06333F),

    // Reserved for "deferred" meaning — the snooze glyph on chips and day rows.
    tertiary = Color(0xFF9A6714),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2E3CE),
    onTertiaryContainer = Color(0xFF3A2708),

    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFAF9F6),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFF2F0EA),
    onSurfaceVariant = Color(0xFF5C584F),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF9F6),   // bottom sheets
    surfaceContainer = Color(0xFFF2F0EA),      // navigation bar
    surfaceContainerHigh = Color(0xFFEAE7DF),
    surfaceContainerHighest = Color(0xFFE6E3DA),

    outline = Color(0xFFC6C1B5),
    outlineVariant = Color(0xFFEAE7DF),

    error = Color(0xFFB3382B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DDD8),
    onErrorContainer = Color(0xFF4A211B),

    inverseSurface = Color(0xFF322F2A),
    inverseOnSurface = Color(0xFFF5F2EC),
    inversePrimary = Color(0xFF93CEDF),
    scrim = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93CEDF),
    onPrimary = Color(0xFF06333F),
    primaryContainer = Color(0xFF1E4550),
    onPrimaryContainer = Color(0xFFBFE6F2),

    secondary = Color(0xFF93CEDF),
    onSecondary = Color(0xFF06333F),
    secondaryContainer = Color(0xFF1E3B44),
    onSecondaryContainer = Color(0xFFBFE6F2),

    tertiary = Color(0xFFE5BC72),
    onTertiary = Color(0xFF3A2708),
    tertiaryContainer = Color(0xFF4A3618),
    onTertiaryContainer = Color(0xFFF2E3CE),

    background = Color(0xFF161512),
    onBackground = Color(0xFFEAE7DF),
    surface = Color(0xFF161512),
    onSurface = Color(0xFFEAE7DF),
    surfaceVariant = Color(0xFF1F1E1A),
    onSurfaceVariant = Color(0xFFB3AEA2),

    surfaceContainerLowest = Color(0xFF0F0E0C),
    surfaceContainerLow = Color(0xFF161512),   // bottom sheets
    surfaceContainer = Color(0xFF1F1E1A),      // navigation bar
    surfaceContainerHigh = Color(0xFF262420),
    surfaceContainerHighest = Color(0xFF2E2B26),

    outline = Color(0xFF4A473E),
    outlineVariant = Color(0xFF262420),

    error = Color(0xFFF0958A),
    onError = Color(0xFF4A211B),
    errorContainer = Color(0xFF4A211B),
    onErrorContainer = Color(0xFFF7DDD8),

    inverseSurface = Color(0xFFEAE7DF),
    inverseOnSurface = Color(0xFF1C1B18),
    inversePrimary = Color(0xFF2F5D6B),
    scrim = Color(0xFF000000)
)

@Composable
fun CalendarAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Off by default: on Android 12+ a dynamic scheme replaces the app's palette outright,
    // so leaving this on would mean the designed "Paper" look is never seen. Opt-in via Settings.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val reminderColors = if (darkTheme) DarkReminderColors else LightReminderColors

    CompositionLocalProvider(
        LocalReminderColors provides reminderColors,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
