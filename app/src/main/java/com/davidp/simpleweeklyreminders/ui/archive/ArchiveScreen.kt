package com.davidp.simpleweeklyreminders.ui.archive

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.archivedSince
import com.davidp.simpleweeklyreminders.data.settings.ArchiveSettings
import com.davidp.simpleweeklyreminders.data.settings.datePattern
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
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

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
            }
            Text("Archive", style = MaterialTheme.typography.titleLarge)
        }

        if (loadedArchived.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                    Text(
                        "Nothing archived yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
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

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = MaterialTheme.appShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            reminder.endDate?.let {
                val datePattern = LocalAppSettings.current.dateFormat.datePattern(LocalContext.current)
                Text(
                    "Ended ${it.format(DateTimeFormatter.ofPattern(datePattern))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (reminder.reminderType != ReminderType.ONE_TIME) {
                    TextButton(onClick = onRestore, shape = MaterialTheme.appShapes.medium) {
                        Text("Restore")
                    }
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Delete Forever", color = MaterialTheme.colorScheme.error)
                }
            }
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
