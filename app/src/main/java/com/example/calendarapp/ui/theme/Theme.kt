package com.example.calendarapp.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Standard shapes used throughout the app.
 */
object AppShapes {
    val small = RoundedCornerShape(8.dp)        // Small cards, indicators
    val medium = RoundedCornerShape(12.dp)      // Buttons, dialogs (most common)
    val large = RoundedCornerShape(16.dp)       // Large dialogs
    val extraLarge = RoundedCornerShape(28.dp)  // Time picker
}

/**
 * Standard dimensions and spacing used throughout the app.
 */
object AppDimensions {
    // Spacing
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp

    // Indicator dots
    val indicatorDotSmall = 4.dp
    val indicatorDotLarge = 8.dp

    // Button sizes
    val frequencyButtonWidth = 48.dp
    val weekdayButtonSize = 40.dp
}

/**
 * Extension properties to access custom theme values from MaterialTheme.
 * Usage:
 *   MaterialTheme.medicalColors.doseTakenIndicator
 *   MaterialTheme.shapes.medium
 *   MaterialTheme.dimensions.spacingSmall
 */
val MaterialTheme.shapes: AppShapes
    @Composable
    @ReadOnlyComposable
    get() = AppShapes

val MaterialTheme.dimensions: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = AppDimensions

val MaterialTheme.medicalColors: MedicalColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMedicalColors.current

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF03A9F4),
    tertiary = Color(0xFF00BCD4)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),
    secondary = Color(0xFF03A9F4),
    tertiary = Color(0xFF00BCD4)
)

@Composable
fun CalendarAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val medicalColors = if (darkTheme) DarkMedicalColors else LightMedicalColors

    CompositionLocalProvider(LocalMedicalColors provides medicalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
