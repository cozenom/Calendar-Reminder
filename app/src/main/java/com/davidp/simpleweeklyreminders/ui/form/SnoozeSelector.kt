package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.settings.SNOOZE_PRESET_MINUTES
import com.davidp.simpleweeklyreminders.ui.components.GroupSurface
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings

/**
 * Snooze length row. [minutes] null = the Settings length, shown as "Default (10m)" so the
 * user sees what they'd get without leaving the form.
 */
@Composable
fun SnoozeSelector(minutes: Int?, onChanged: (Int?) -> Unit) {
    val globalMinutes = LocalAppSettings.current.snoozeMinutes
    var showMenu by remember { mutableStateOf(false) }

    GroupSurface(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showMenu = true }
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Snooze", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            // Box anchors the menu under the value
            Box {
                Text(
                    if (minutes == null) "Default (${globalMinutes}m)" else "${minutes}m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (minutes == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Default (${globalMinutes}m)") },
                        onClick = { onChanged(null); showMenu = false }
                    )
                    SNOOZE_PRESET_MINUTES.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text("${preset}m") },
                            onClick = { onChanged(preset); showMenu = false }
                        )
                    }
                }
            }
        }
    }
}
