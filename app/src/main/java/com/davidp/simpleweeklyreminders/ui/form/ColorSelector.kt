package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.ui.theme.LocalIsDarkTheme
import com.davidp.simpleweeklyreminders.ui.theme.ReminderSwatches
import com.davidp.simpleweeklyreminders.ui.theme.onReminderAccent

/**
 * Per-reminder colour. Only shown when "Per-reminder colours" is on in Settings — the form
 * decides that, this composable just renders the row.
 *
 * The first option is "theme accent" (a null key), so a reminder can always be put back to
 * following the theme without clearing the setting for everything else.
 */
@Composable
fun ColorSelector(selectedKey: String?, onChanged: (String?) -> Unit) {
    val dark = LocalIsDarkTheme.current

    Column {
        Text(
            "Colour",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Swatch(
                color = MaterialTheme.colorScheme.primary,
                label = "Theme colour",
                isSelected = selectedKey == null,
                onClick = { onChanged(null) },
                modifier = Modifier.weight(1f)
            )
            ReminderSwatches.forEach { swatch ->
                Swatch(
                    color = if (dark) swatch.dark else swatch.light,
                    label = swatch.label,
                    isSelected = selectedKey == swatch.key,
                    onClick = { onChanged(swatch.key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Swatch(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick, role = Role.RadioButton)
            .semantics {
                contentDescription = label
                selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = onReminderAccent(color),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
