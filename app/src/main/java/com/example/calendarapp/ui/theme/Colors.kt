package com.example.calendarapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for medical status indicators and inventory warnings.
 * Maintains medical meaning (green=good, red=bad, orange=warning) while
 * adapting to light/dark mode for proper readability.
 */
data class MedicalColors(
    // Medication intake status
    val medicationTakenContainer: Color,
    val medicationTakenContent: Color,
    val medicationMissedContainer: Color,
    val medicationMissedContent: Color,

    // Inventory status (medication remaining)
    val inventoryGoodContainer: Color,
    val inventoryGoodContent: Color,
    val inventoryWarningContainer: Color,
    val inventoryWarningContent: Color,
    val inventoryUrgentContainer: Color,
    val inventoryUrgentContent: Color,
    val inventoryEmptyContainer: Color,
    val inventoryEmptyContent: Color,

    // Calendar indicator dots
    val doseTakenIndicator: Color,
    val doseMissedIndicator: Color,
    val estimatedRefillIndicator: Color,
)

val LightMedicalColors = MedicalColors(
    // Medication intake - light mode
    medicationTakenContainer = Color(0xFFE8F5E9),  // Light green
    medicationTakenContent = Color(0xFF2E7D32),    // Dark green
    medicationMissedContainer = Color(0xFFFFEBEE), // Light red
    medicationMissedContent = Color(0xFFC62828),   // Dark red

    // Inventory good stock - light mode
    inventoryGoodContainer = Color(0xFFE8F5E9),    // Light green
    inventoryGoodContent = Color(0xFF2E7D32),      // Dark green

    // Inventory warning (<7 days) - light mode
    inventoryWarningContainer = Color(0xFFFFF3E0), // Light orange
    inventoryWarningContent = Color(0xFFE65100),   // Dark orange

    // Inventory urgent (≤3 days) - light mode
    inventoryUrgentContainer = Color(0xFFFFEBEE),  // Light red
    inventoryUrgentContent = Color(0xFFC62828),    // Dark red

    // Inventory empty (0 remaining) - light mode
    inventoryEmptyContainer = Color(0xFFFFCDD2),   // Darker red
    inventoryEmptyContent = Color(0xFFB71C1C),     // Very dark red

    // Calendar indicators - light mode
    doseTakenIndicator = Color(0xFF4CAF50),        // Standard Material green
    doseMissedIndicator = Color(0xFFF44336),       // Standard Material red
    estimatedRefillIndicator = Color(0xFFFFA500),  // Standard orange
)

val DarkMedicalColors = MedicalColors(
    // Medication intake - dark mode
    medicationTakenContainer = Color(0xFF1B5E20),  // Dark green
    medicationTakenContent = Color(0xFFA5D6A7),    // Light green
    medicationMissedContainer = Color(0xFFB71C1C), // Dark red
    medicationMissedContent = Color(0xFFEF9A9A),   // Light red

    // Inventory good stock - dark mode
    inventoryGoodContainer = Color(0xFF1B5E20),    // Dark green
    inventoryGoodContent = Color(0xFFA5D6A7),      // Light green

    // Inventory warning (<7 days) - dark mode
    inventoryWarningContainer = Color(0xFFBF360C), // Dark orange
    inventoryWarningContent = Color(0xFFFFCC80),   // Light orange

    // Inventory urgent (≤3 days) - dark mode
    inventoryUrgentContainer = Color(0xFF8B0000),  // Dark red
    inventoryUrgentContent = Color(0xFFEF9A9A),    // Light red

    // Inventory empty (0 remaining) - dark mode
    inventoryEmptyContainer = Color(0xFFB71C1C),   // Very dark red
    inventoryEmptyContent = Color(0xFFEF9A9A),     // Light red

    // Calendar indicators - dark mode
    doseTakenIndicator = Color(0xFF4CAF50),        // Standard Material green
    doseMissedIndicator = Color(0xFFF44336),       // Standard Material red
    estimatedRefillIndicator = Color(0xFFFFB74D),  // Lighter orange
)

val LocalMedicalColors = staticCompositionLocalOf { LightMedicalColors }
