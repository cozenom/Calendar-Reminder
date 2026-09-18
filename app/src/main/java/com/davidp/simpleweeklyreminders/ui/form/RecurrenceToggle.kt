package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.ui.theme.appShapes

/**
 * Three-way repeat mode as a segmented control. Was an ExposedDropdownMenu — a dropdown hid
 * two of only three options behind a tap, and the choice changes which selector appears below.
 *
 * [enabled] = false shows the current mode but takes no taps: the type is fixed once a
 * reminder exists (see ReminderForm), like its start date.
 */
@Composable
fun RecurrenceToggle(mode: ReminderType, onChanged: (ReminderType) -> Unit, enabled: Boolean = true) {
    val options = listOf(
        ReminderType.SPECIFIC_DAYS to "Weekdays",
        ReminderType.EVERY_N_DAYS to "Every N days",
        ReminderType.ONE_TIME to "One-time"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (type, label) ->
            val selected = type == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.appShapes.small)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        onClick = { onChanged(type) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                // Locked: the unselected labels fade to M3's disabled alpha, the selected one
                // stays readable so the current type is still obvious
                val color = when {
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = color
                )
            }
        }
    }
}
