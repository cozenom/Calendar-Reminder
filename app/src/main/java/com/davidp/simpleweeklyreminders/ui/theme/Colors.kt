package com.davidp.simpleweeklyreminders.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for reminder status indicators.
 * Maintains meaning (green=completed, red=pending/missed) while
 * adapting to light/dark mode for proper readability.
 */
data class ReminderColors(
    // Reminder completion status (list items)
    val completedContainer: Color,
    val completedContent: Color,
    val pendingContainer: Color,
    val pendingContent: Color,

    // Calendar indicator dots
    val completedIndicator: Color,
    val pendingIndicator: Color,

    // Importance chevrons (list row, calendar pip, calendar list) — blue/amber/red
    // rather than green/amber/red: green-red is the hardest pair to distinguish
    // for red-green color blindness, so the chevron *count* carries the primary
    // signal and color reinforces it rather than being relied on alone.
    val importanceLow: Color,
    val importanceMedium: Color,
    val importanceHigh: Color,
)

val LightReminderColors = ReminderColors(
    // Completed - light mode
    completedContainer = Color(0xFFE8F5E9),  // Light green
    completedContent = Color(0xFF2E7D32),    // Dark green
    // Pending/missed - light mode
    pendingContainer = Color(0xFFFFEBEE),    // Light red
    pendingContent = Color(0xFFC62828),      // Dark red

    // Calendar indicators - light mode
    completedIndicator = Color(0xFF4CAF50),  // Standard Material green
    pendingIndicator = Color(0xFFF44336),    // Standard Material red

    // Importance chevrons - light mode
    importanceLow = Color(0xFF1976D2),       // Blue 700
    importanceMedium = Color(0xFFF9A825),    // Amber 800
    importanceHigh = Color(0xFFD32F2F),      // Red 700
)

val DarkReminderColors = ReminderColors(
    // Completed - dark mode
    completedContainer = Color(0xFF1B5E20),  // Dark green
    completedContent = Color(0xFFA5D6A7),    // Light green
    // Pending/missed - dark mode
    pendingContainer = Color(0xFFB71C1C),    // Dark red
    pendingContent = Color(0xFFEF9A9A),      // Light red

    // Calendar indicators - dark mode
    completedIndicator = Color(0xFF4CAF50),  // Standard Material green
    pendingIndicator = Color(0xFFF44336),    // Standard Material red

    // Importance chevrons - dark mode
    importanceLow = Color(0xFF64B5F6),       // Blue 300
    importanceMedium = Color(0xFFFFD54F),    // Amber 300
    importanceHigh = Color(0xFFEF9A9A),      // Red 300
)

val LocalReminderColors = staticCompositionLocalOf { LightReminderColors }
