package com.example.calendarapp.ui.theme

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
)

val LocalReminderColors = staticCompositionLocalOf { LightReminderColors }
