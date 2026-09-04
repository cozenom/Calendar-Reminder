package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.model.isScheduledOn
import com.davidp.simpleweeklyreminders.data.settings.datePattern
import com.davidp.simpleweeklyreminders.data.settings.dateNoYearPattern
import com.davidp.simpleweeklyreminders.data.settings.timePattern
import com.davidp.simpleweeklyreminders.ui.components.ImportanceChevrons
import com.davidp.simpleweeklyreminders.ui.form.ReminderFormSheet
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.onReminderAccent
import com.davidp.simpleweeklyreminders.ui.theme.reminderAccent
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.ui.theme.reminderWash
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Comfortable width for a "12:00 PM" chip with its icon at default font size. */
private val TIME_CHIP_MIN_WIDTH = 90.dp
private val TIME_CHIP_SPACING = 7.dp

/** Left inset that lines the chip row up under the title rather than the icon tile. */
private val CHIP_ROW_INSET = 45.dp

@Composable
fun ReminderItem(
    reminder: Reminder,
    todayLogs: List<ReminderLog>,
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
                            .size(20.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
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
                            now = LocalDateTime.now(),
                            timePattern = timePattern,
                            datePattern = datePattern,
                            dateNoYearPattern = dateNoYearPattern
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { viewModel.update(reminder.copy(isActive = it)) }
                )
            }

            Spacer(modifier = Modifier.height(9.dp))
            Row(
                modifier = Modifier.padding(start = CHIP_ROW_INSET),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeChips(
                    reminder = reminder,
                    todayLogs = todayLogs,
                    timePattern = timePattern,
                    accent = accent,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
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
            }

            if (hasNotes && showNotes) {
                Text(
                    reminder.notes.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = CHIP_ROW_INSET, top = 8.dp)
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

/**
 * Grid of equal-width chips. How many fit per row is computed from the available width and
 * the user's font scale instead of assuming a screen size, so large-font settings gracefully
 * drop to fewer, wider chips.
 */
@Composable
private fun TimeChips(
    reminder: Reminder,
    todayLogs: List<ReminderLog>,
    timePattern: String,
    accent: Color,
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val scheduledToday = reminder.isActive && reminder.isScheduledOn(today)

    BoxWithConstraints(modifier = modifier) {
        val minChipWidth = TIME_CHIP_MIN_WIDTH * LocalDensity.current.fontScale
        val chipsPerRow = ((maxWidth + TIME_CHIP_SPACING) / (minChipWidth + TIME_CHIP_SPACING))
            .toInt()
            .coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(TIME_CHIP_SPACING)) {
            reminder.reminderTimes.distinct().sorted().chunked(chipsPerRow).forEach { rowTimes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TIME_CHIP_SPACING)
                ) {
                    rowTimes.forEach { time ->
                        val log = todayLogs.firstOrNull { it.logDateTime.toLocalTime() == time }
                        TimeChip(
                            label = time.format(DateTimeFormatter.ofPattern(timePattern)),
                            completed = log?.completed == true,
                            snoozed = log?.snoozedUntil != null,
                            actionable = log != null || scheduledToday,
                            accent = accent,
                            onClick = {
                                if (log != null) {
                                    viewModel.updateLogCompletedStatus(log.id, !log.completed)
                                } else {
                                    // No log for this slot today (time had already passed when
                                    // the reminder was created/edited) — record it on demand
                                    viewModel.logAdHocCompletion(reminder, LocalDateTime.of(today, time))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Keep a partial last row's chips the same size as full rows
                    repeat(chipsPerRow - rowTimes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * One scheduled time as a chip. Tapping toggles (or records) today's completion; chips are
 * inert only when the reminder isn't scheduled today. A pending snooze shows a snooze glyph
 * until the notification re-fires.
 *
 * Done fills with the accent, outstanding is an outline — the same fill-not-hue rule the
 * calendar's day segments use.
 */
@Composable
private fun TimeChip(
    label: String,
    completed: Boolean,
    snoozed: Boolean,
    actionable: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.reminderColors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = when {
            completed -> accent
            actionable -> Color.Transparent
            else -> colors.neutral
        },
        contentColor = when {
            completed -> onReminderAccent(accent)
            actionable -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (actionable && !completed) BorderStroke(1.dp, colors.hairline) else null,
        onClick = onClick,
        enabled = actionable
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val showSnooze = snoozed && !completed
            Icon(
                imageVector = when {
                    completed -> Icons.Filled.Check
                    showSnooze -> Icons.Filled.Snooze
                    else -> Icons.Outlined.Schedule
                },
                contentDescription = when {
                    completed -> "Completed"
                    showSnooze -> "Snoozed"
                    else -> null
                },
                tint = if (showSnooze) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
