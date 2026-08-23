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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.settings.AppSettingsState
import com.davidp.simpleweeklyreminders.data.settings.ThemeMode
import com.davidp.simpleweeklyreminders.data.settings.ThemePack

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
 * The single definition of "is the app dark right now": the user's setting, falling back to
 * the system value only for SYSTEM. Shared by the theme and by MainActivity's system-bar
 * setup, which needs the answer before composition starts.
 */
fun ThemeMode.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}

/**
 * Warm neutral paper surfaces, shared by every theme pack. Only the accent family below
 * changes per pack, so the app keeps one identity and the packs stay a six-colour swap.
 */
private fun lightSchemeFor(tones: ThemePackTones) = LightNeutralScheme.copy(
    primary = tones.accentLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = tones.containerLight,
    onPrimaryContainer = tones.deep,
    secondary = tones.accentLight,
    onSecondary = Color(0xFFFFFFFF),
    // Softer than the container: the wash behind an icon, not a selected surface
    secondaryContainer = tones.containerLight.copy(alpha = 0.55f).compositeOver(Color(0xFFFAF9F6)),
    onSecondaryContainer = tones.deep,
    inversePrimary = tones.accentDark
)

private fun darkSchemeFor(tones: ThemePackTones) = DarkNeutralScheme.copy(
    primary = tones.accentDark,
    onPrimary = tones.deep,
    primaryContainer = tones.containerDark,
    onPrimaryContainer = tones.pale,
    secondary = tones.accentDark,
    onSecondary = tones.deep,
    secondaryContainer = tones.containerDark.copy(alpha = 0.7f).compositeOver(Color(0xFF161512)),
    onSecondaryContainer = tones.pale,
    inversePrimary = tones.accentLight
)

private val LightNeutralScheme = lightColorScheme(
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
    scrim = Color(0xFF000000)
)

private val DarkNeutralScheme = darkColorScheme(
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
    scrim = Color(0xFF000000)
)

@Composable
fun CalendarAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Off by default: on Android 12+ a dynamic scheme replaces the app's palette outright,
    // so leaving this on would mean the designed look is never seen. Opt-in via Settings,
    // where it appears as one more entry in the theme-pack list.
    dynamicColor: Boolean = false,
    themePack: ThemePack = ThemePack.PAPER,
    content: @Composable () -> Unit
) {
    val darkTheme = themeMode.resolveDark(isSystemInDarkTheme())
    val tones = tonesFor(themePack)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkSchemeFor(tones)
        else -> lightSchemeFor(tones)
    }

    // Status colours follow whatever accent ended up in the scheme, so a pack — or a dynamic
    // wallpaper scheme — colours completed days without a second source of truth.
    val reminderColors = remember(colorScheme.primary, colorScheme.onPrimary, darkTheme) {
        reminderColorsFor(
            accent = colorScheme.primary,
            onAccent = colorScheme.onPrimary,
            dark = darkTheme
        )
    }

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
