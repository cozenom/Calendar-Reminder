package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors

/**
 * Tap-to-select segmented control, styled like WeekdaySelector's day buttons but in each
 * level's own color so all three preview before a choice is made. [importance] starts null
 * — HIGH is a migration default (see Reminder.kt), not one to inherit silently.
 */
@Composable
fun ImportanceSelector(importance: Importance?, onChanged: (Importance) -> Unit) {
    val reminderColors = MaterialTheme.reminderColors
    val options = listOf(
        Triple(Importance.LOW, "Low", reminderColors.importanceLow),
        Triple(Importance.MEDIUM, "Medium", reminderColors.importanceMedium),
        Triple(Importance.HIGH, "High", reminderColors.importanceHigh)
    )

    Column {
        Text(
            "Importance",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (level, label, color) ->
                ImportanceButton(
                    label = label,
                    color = color,
                    isSelected = importance == level,
                    onClick = { onChanged(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ImportanceButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) color else Color.Transparent
    // Picked from the fill's own luminance rather than a fixed onSurface/surface
    // token: the dark-theme tints (e.g. amber 300) are just as light as their
    // light-theme counterparts are dark, so a theme-fixed choice would read fine
    // in one theme and wash out in the other for the same color
    val contentColor = if (isSelected) {
        if (color.luminance() > 0.5f) Color.Black else Color.White
    } else color

    Box(
        modifier = modifier
            .clip(MaterialTheme.appShapes.medium)
            .background(backgroundColor)
            .border(width = 1.dp, color = color, shape = MaterialTheme.appShapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
    }
}
