package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.settings.datePattern
import com.davidp.simpleweeklyreminders.data.settings.dateNoYearPattern
import com.davidp.simpleweeklyreminders.data.settings.timePattern
import com.davidp.simpleweeklyreminders.ui.components.ImportanceChevrons
import com.davidp.simpleweeklyreminders.ui.form.ReminderFormSheet
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.reminderAccent
import com.davidp.simpleweeklyreminders.ui.theme.reminderWash
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import java.time.LocalDate

/** Left inset that lines the expanded notes up under the title rather than the icon tile. */
private val NOTES_INSET = 45.dp

/** The grip's slot. Reserved even when drag is off so the row doesn't shift sideways. */
private val DRAG_HANDLE_SIZE = 20.dp

/**
 * One reminder as set up: icon, title, cadence + times, on/off. No today's-status here —
 * that's the Calendar tab's job, and showing it twice meant two places that could disagree.
 */
@Composable
fun ReminderItem(
    reminder: Reminder,
    onArchive: () -> Unit,
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier,
    dragEnabled: Boolean = true,
    // Applied to the grip icon, not the row: that icon *is* the drag handle, so this is
    // where the caller's `draggableHandle()` has to land.
    dragHandleModifier: Modifier = Modifier
) {
    var showEditSheet by remember { mutableStateOf(false) }
    // Saveable, not plain remember: losing an expanded note on a scroll out of view (or a
    // rotation) would be irritating. LazyColumn restores this per item key when the row
    // scrolls back in.
    var showNotes by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val timePattern = settings.timeFormat.timePattern(context)
    val datePattern = settings.dateFormat.datePattern(context)
    val dateNoYearPattern = settings.dateFormat.dateNoYearPattern(context)
    val hasNotes = !reminder.notes.isNullOrBlank()
    // Falls back to the theme accent when per-reminder colours are off or none is set
    val accent = reminderAccent(reminder.color)

    Column(modifier = modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        // Paused reminders stay legible but visibly recede
        Column(
            modifier = Modifier
                .alpha(if (reminder.isActive) 1f else 0.55f)
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dragEnabled) {
                    Icon(
                        imageVector = Icons.Filled.DragIndicator,
                        contentDescription = "Drag to reorder",
                        modifier = dragHandleModifier
                            .size(DRAG_HANDLE_SIZE)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                } else {
                    // Sorting/filtering pauses drag; keep the layout identical, just no grip
                    Spacer(modifier = Modifier.width(DRAG_HANDLE_SIZE))
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(MaterialTheme.appShapes.small)
                        .background(reminderWash(accent)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconFromKey(reminder.icon).icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Chevrons lead the title so they align in one column down the list
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ImportanceChevrons(
                            importance = reminder.importance,
                            chevronWidth = 12.dp,
                            chevronHeight = 5.dp
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            reminder.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        // Notes live behind a visible glyph, not the overflow: it doubles as an
                        // at-a-glance "this one has a note" marker and lights up when expanded.
                        if (hasNotes) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Notes,
                                contentDescription = if (showNotes) "Hide notes" else "Show notes",
                                tint = if (showNotes) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showNotes = !showNotes }
                                    .padding(5.dp)
                            )
                        }
                    }
                    Text(
                        rowSubtitle(
                            reminder = reminder,
                            timePattern = timePattern,
                            datePattern = datePattern,
                            dateNoYearPattern = dateNoYearPattern,
                            today = LocalDate.now()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { showMenu = false; showEditSheet = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                            onClick = { showMenu = false; onArchive() }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { viewModel.update(reminder.copy(isActive = it)) }
                )
            }

            if (hasNotes && showNotes) {
                Text(
                    reminder.notes.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = NOTES_INSET, top = 8.dp)
                )
            }
        }
    }

    if (showEditSheet) {
        ReminderFormSheet(
            initial = reminder,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                viewModel.update(updated)
                showEditSheet = false
            }
        )
    }
}
