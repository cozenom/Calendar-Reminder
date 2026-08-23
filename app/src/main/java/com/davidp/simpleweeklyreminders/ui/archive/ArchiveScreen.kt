package com.davidp.simpleweeklyreminders.ui.archive

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.archivedSince
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.settings.ArchiveSettings
import com.davidp.simpleweeklyreminders.data.settings.datePattern
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import java.time.format.DateTimeFormatter

/** Reminders that lapsed into the Archive after the user last viewed it. */
fun newlyArchivedCount(archived: List<Reminder>, context: Context): Int {
    val lastViewed = ArchiveSettings.getLastViewed(context)
    return archived.count { it.archivedSince()?.isAfter(lastViewed) == true }
}

@Composable
fun ArchiveScreen(viewModel: ReminderViewModel, onBack: () -> Unit) {
    val archived by viewModel.archivedReminders.collectAsState()
    val loadedArchived = archived ?: return
    val context = LocalContext.current
    // Viewing this screen clears the "new since last checked" badge/notice.
    LaunchedEffect(Unit) { ArchiveSettings.markViewedNow(context) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Archive", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            text = if (loadedArchived.isEmpty()) "Reminders you end or archive are kept here"
            else "${loadedArchived.size} lapsed reminder${if (loadedArchived.size == 1) "" else "s"} · kept until you delete them",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 57.dp, end = 20.dp, bottom = 12.dp)
        )

        if (loadedArchived.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                    Text(
                        "Nothing archived yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(loadedArchived, key = { it.id }) { reminder ->
                    ArchivedReminderItem(
                        reminder = reminder,
                        onRestore = { viewModel.restore(reminder) },
                        onDelete = { viewModel.delete(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArchivedReminderItem(reminder: Reminder, onRestore: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val datePattern = LocalAppSettings.current.dateFormat.datePattern(LocalContext.current)
    val subtitle = when {
        reminder.reminderType == ReminderType.ONE_TIME ->
            "One-time · ${reminder.startDate.format(DateTimeFormatter.ofPattern(datePattern))}"
        reminder.endDate != null ->
            "Ended ${reminder.endDate?.format(DateTimeFormatter.ofPattern(datePattern))}"
        else -> "Archived"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.appShapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFromKey(reminder.icon).icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 50.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // A one-time reminder has no schedule to resume, so restoring it would do nothing
            if (reminder.reminderType != ReminderType.ONE_TIME) {
                ArchiveAction(
                    icon = Icons.Filled.RestartAlt,
                    label = "Restore",
                    onClick = onRestore,
                    filled = true
                )
            }
            ArchiveAction(
                icon = Icons.Outlined.Delete,
                label = "Delete",
                onClick = { showDeleteConfirm = true },
                filled = false
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reminder") },
            text = { Text("Permanently delete \"${reminder.title}\" and its history? This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    shape = MaterialTheme.appShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, shape = MaterialTheme.appShapes.medium) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** Pill action on an archive card: accent wash for the primary, hairline outline for the rest. */
@Composable
private fun ArchiveAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    filled: Boolean
) {
    val colors = MaterialTheme.reminderColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) colors.doneWash else Color.Transparent)
            .then(
                if (filled) Modifier
                else Modifier.border(1.dp, colors.hairline, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) colors.done else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) colors.done else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
