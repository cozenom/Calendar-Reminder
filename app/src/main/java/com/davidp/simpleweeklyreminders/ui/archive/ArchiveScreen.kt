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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.OccurrenceCounts
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.archivedSince
import com.davidp.simpleweeklyreminders.data.model.hasLapsed
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.settings.ArchiveSettings
import com.davidp.simpleweeklyreminders.data.settings.dateNoYearPattern
import com.davidp.simpleweeklyreminders.data.settings.datePattern
import com.davidp.simpleweeklyreminders.ui.calendar.CalendarDialog
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.components.EmptyState
import com.davidp.simpleweeklyreminders.ui.components.GroupSurface
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import java.time.LocalDate
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
            EmptyState(icon = Icons.Outlined.Inventory2, title = "Nothing archived yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(loadedArchived, key = { it.id }) { reminder ->
                    ArchivedReminderItem(
                        reminder = reminder,
                        loadStats = { viewModel.loadArchiveStats(reminder) },
                        onRestore = { endDate -> viewModel.restore(reminder, endDate) },
                        onDelete = { viewModel.delete(reminder) }
                    )
                }
            }
        }
    }
}

/** "41 done, 3 missed" — omits a zero side, null while loading or when nothing was tracked. */
private fun statsSummary(counts: OccurrenceCounts?): String? {
    val c = counts ?: return null
    return when {
        c.done + c.missed == 0 -> null
        c.missed == 0 -> "${c.done} done"
        c.done == 0 -> "${c.missed} missed"
        else -> "${c.done} done, ${c.missed} missed"
    }
}

@Composable
fun ArchivedReminderItem(
    reminder: Reminder,
    loadStats: suspend () -> OccurrenceCounts,
    /** Restores with the given end date: the reminder's own, or the one picked in the dialog. */
    onRestore: (endDate: LocalDate?) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    // The end date being chosen in the restore dialog; null = Never
    var restoreEndDate by remember { mutableStateOf<LocalDate?>(null) }
    // Loaded once per row (one-shot suspend aggregate), not observed
    var stats by remember(reminder.id) { mutableStateOf<OccurrenceCounts?>(null) }
    LaunchedEffect(reminder.id) { stats = loadStats() }

    val dateFormat = LocalAppSettings.current.dateFormat
    val datePattern = dateFormat.datePattern(LocalContext.current)
    val dateNoYearPattern = dateFormat.dateNoYearPattern(LocalContext.current)
    // archivedAt first: a manual archive keeps the user's end date, which may still be ahead
    val base = when {
        reminder.reminderType == ReminderType.ONE_TIME ->
            "One-time · ${reminder.startDate.format(DateTimeFormatter.ofPattern(datePattern))}"
        reminder.archivedAt != null ->
            "Archived ${reminder.archivedAt?.toLocalDate()?.format(DateTimeFormatter.ofPattern(datePattern))}"
        reminder.endDate != null ->
            "Ended ${reminder.endDate?.format(DateTimeFormatter.ofPattern(datePattern))}"
        else -> "Archived"
    }
    val subtitle = base + (statsSummary(stats)?.let { " · $it" } ?: "")

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
                    // Past its end date there's nothing left to run, so ask for a new one;
                    // otherwise (manual archive) restore with the end date it already has
                    onClick = {
                        if (reminder.hasLapsed()) {
                            restoreEndDate = null // each opening starts from "Never"
                            showRestoreDialog = true
                        } else {
                            onRestore(reminder.endDate)
                        }
                    },
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

    // Same shape as the Delete confirm above (filled action + Cancel); the choice uses the
    // radio rows from the sort sheet. "On a date" opens the picker — tap it again to change.
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Reminder") },
            text = {
                Column {
                    Text(
                        "\"${reminder.title}\" ended " +
                            "${reminder.endDate?.format(DateTimeFormatter.ofPattern(datePattern))}. " +
                            "Choose when it should end now."
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GroupSurface(Modifier.fillMaxWidth().selectableGroup()) {
                        EndOptionRow(
                            label = "Never",
                            selected = restoreEndDate == null,
                            onClick = { restoreEndDate = null }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        EndOptionRow(
                            label = "On a date",
                            selected = restoreEndDate != null,
                            onClick = { showEndDatePicker = true },
                            value = restoreEndDate?.format(DateTimeFormatter.ofPattern(dateNoYearPattern))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRestoreDialog = false; onRestore(restoreEndDate) },
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }, shape = MaterialTheme.appShapes.medium) {
                    Text("Cancel")
                }
            }
        )
    }

    // Opens over the restore dialog. Picking selects "On a date"; cancelling leaves the choice
    // as it was. Restore still has to be tapped to commit.
    if (showEndDatePicker) {
        val today = LocalDate.now()
        CalendarDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { restoreEndDate = it; showEndDatePicker = false },
            initialDate = restoreEndDate ?: today,
            // A past date would land it straight back in the Archive
            minDate = today
        )
    }
}

/**
 * One radio row in the restore dialog — same row as the sort sheet's "Sort by" options.
 * [value] shows on the right of the selected "On a date" row (the picked date).
 */
@Composable
private fun EndOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    value: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
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
