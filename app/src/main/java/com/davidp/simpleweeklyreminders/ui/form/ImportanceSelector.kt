package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.ui.components.ImportanceChevrons
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors

/**
 * Tap-to-select segmented control, styled like WeekdaySelector's day buttons but in each
 * level's own color so all three read at a glance.
 */
@Composable
fun ImportanceSelector(importance: Importance, onChanged: (Importance) -> Unit) {
    val reminderColors = MaterialTheme.reminderColors
    val options = listOf(
        Triple(Importance.LOW, "Low", reminderColors.importanceLow),
        Triple(Importance.MEDIUM, "Medium", reminderColors.importanceMedium),
        Triple(Importance.HIGH, "High", reminderColors.importanceHigh)
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (level, label, color) ->
                ImportanceButton(
                    label = label,
                    level = level,
                    color = color,
                    isSelected = importance == level,
                    onClick = { onChanged(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Explained at the point of choice: High's notification behaves differently from
        // Android's swipe-to-clear default, and there's no other place a user would learn it
        Text(
            text = importanceCaption(importance),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )
    }
}

private fun importanceCaption(importance: Importance): String = when (importance) {
    Importance.LOW -> "Quiet. Swipe to clear."
    Importance.MEDIUM -> "Plays a sound. Swipe to clear."
    Importance.HIGH -> "Alarm tone. Stays until you act on it."
}

/**
 * Chevrons preview the level whether or not it's picked, so all three read at a glance. The
 * selection itself is carried by the fill, matching the chips and calendar segments.
 */
@Composable
private fun ImportanceButton(
    label: String,
    level: Importance,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.appShapes.small)
            .background(if (isSelected) color.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.appShapes.small
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImportanceChevrons(importance = level, chevronWidth = 12.dp, chevronHeight = 5.dp)
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
