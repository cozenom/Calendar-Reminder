package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
