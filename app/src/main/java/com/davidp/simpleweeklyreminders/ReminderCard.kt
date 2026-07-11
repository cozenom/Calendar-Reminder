package com.davidp.simpleweeklyreminders

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.DEFAULT_ICON_KEY
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val WEEKDAY_ABBREVIATIONS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Human-readable recurrence line, e.g. "Mon, Wed, Fri", "Every 2 days", "Weekdays". */
fun scheduleSummary(reminder: Reminder): String {
    val base = when {
        reminder.dayInterval != null ->
            if (reminder.dayInterval == 1) "Every day" else "Every ${reminder.dayInterval} days"
        reminder.reminderDays.size == 7 -> "Every day"
        reminder.reminderDays == setOf(1, 2, 3, 4, 5) -> "Weekdays"
        reminder.reminderDays == setOf(6, 7) -> "Weekends"
        else -> reminder.reminderDays.sorted().joinToString(", ") { WEEKDAY_ABBREVIATIONS[it - 1] }
    }

    val today = LocalDate.now()
    val qualifiers = buildList {
        if (reminder.startDate > today) {
            add("starts ${reminder.startDate.format(DateTimeFormatter.ofPattern("MMM d"))}")
        }
        reminder.endDate?.let { add("until ${it.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}") }
    }
    return (listOf(base) + qualifiers).joinToString(" · ")
}

/** Whether this reminder's schedule includes the given date (ignores isActive). */
fun isScheduledOn(reminder: Reminder, date: LocalDate): Boolean {
    if (date < reminder.startDate) return false
    reminder.endDate?.let { if (date > it) return false }
    return if (reminder.dayInterval != null) {
        ChronoUnit.DAYS.between(reminder.startDate, date) % reminder.dayInterval == 0L
    } else {
        reminder.reminderDays.contains(date.dayOfWeek.value)
    }
}

/** The next date-time this reminder is scheduled to fire, or null if none (paused/ended). */
fun nextOccurrence(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
    if (!reminder.isActive || reminder.reminderTimes.isEmpty()) return null

    val sortedTimes = reminder.reminderTimes.sorted()
    var date = maxOf(reminder.startDate, now.toLocalDate())
    val endDate = reminder.endDate ?: date.plusYears(1)

    while (date <= endDate) {
        if (isScheduledOn(reminder, date)) {
            for (time in sortedTimes) {
                val dateTime = LocalDateTime.of(date, time)
                if (dateTime > now) return dateTime
            }
        }
        date = date.plusDays(1)
    }
    return null
}

private fun formatNextOccurrence(dateTime: LocalDateTime, now: LocalDateTime, timePattern: String): String {
    val time = dateTime.format(DateTimeFormatter.ofPattern(timePattern))
    val daysAway = ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate())
    val day = when {
        daysAway == 0L -> "Today"
        daysAway == 1L -> "Tomorrow"
        daysAway < 7L -> dateTime.format(DateTimeFormatter.ofPattern("EEEE"))
        else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    return "$day at $time"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderItem(
    reminder: Reminder,
    todayLogs: List<ReminderLog>,
    onDelete: () -> Unit,
    viewModel: ReminderViewModel,
    dragHandleModifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = isEditing) {
        isEditing = false
    }
    var editedTitle by remember { mutableStateOf(reminder.title) }
    var editedTimes by remember { mutableStateOf(reminder.reminderTimes) }
    var editedFrequency by remember { mutableIntStateOf(reminder.frequency) }
    var editedStartDate by remember { mutableStateOf(reminder.startDate) }
    var editedEndDate by remember { mutableStateOf(reminder.endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var editedReminderDays by remember { mutableStateOf(reminder.reminderDays) }
    var editedDayInterval by remember { mutableIntStateOf(reminder.dayInterval ?: 1) }
    var useEveryNDays by remember { mutableStateOf(reminder.dayInterval != null) }
    var editedNotes by remember { mutableStateOf(reminder.notes ?: "") }
    var editedIcon by remember { mutableStateOf(reminder.icon ?: DEFAULT_ICON_KEY) }
    var showEditIconPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.appShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Title") },
                    trailingIcon = {
                        if (editedTitle.isNotEmpty()) {
                            IconButton(onClick = { editedTitle = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                FrequencySelector(frequency = editedFrequency, onFrequencyChange = {
                    editedFrequency = it
                    editedTimes = List(it) { index ->
                        if (index < editedTimes.size) editedTimes[index]
                        // +2 min like the Add form, so the new slot hasn't already
                        // passed (and lost today's log) by the time the user saves
                        else LocalTime.now().plusMinutes(2).truncatedTo(ChronoUnit.MINUTES)
                    }
                })
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                editedTimes.forEachIndexed { index, time ->
                    Material3TimePicker(initialTime = time, onTimeSelected = { newTime ->
                        editedTimes = editedTimes.toMutableList().also { it[index] = newTime }
                    })
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                }
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Start Date: ${editedStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
                }
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.appShapes.medium
                    ) {
                        Text(
                            if (editedEndDate != null) "End: ${editedEndDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                            else "Set End Date",
                            maxLines = 1
                        )
                    }
                    if (editedEndDate != null) {
                        TextButton(onClick = { editedEndDate = null }, shape = MaterialTheme.appShapes.medium) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                RecurrenceToggle(useEveryNDays = useEveryNDays, onChanged = { useEveryNDays = it })
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                if (useEveryNDays) {
                    DayIntervalSelector(interval = editedDayInterval, onIntervalChange = { editedDayInterval = it })
                } else {
                    WeekdaySelector(selectedDays = editedReminderDays, onDaysChanged = { editedReminderDays = it })
                }
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                IconPickerRow(selectedKey = editedIcon, onChangeTapped = { showEditIconPicker = true })
                if (showEditIconPicker) {
                    IconPickerDialog(
                        currentKey = editedIcon,
                        onIconSelected = { editedIcon = it },
                        onDismiss = { showEditIconPicker = false }
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.update(
                                reminder.copy(
                                    title = editedTitle.ifBlank { "Reminder" },
                                    reminderTimes = editedTimes,
                                    frequency = editedFrequency,
                                    startDate = editedStartDate,
                                    endDate = editedEndDate,
                                    reminderDays = editedReminderDays,
                                    notes = editedNotes.ifBlank { null },
                                    icon = editedIcon,
                                    dayInterval = if (useEveryNDays) editedDayInterval else null
                                )
                            )
                            isEditing = false
                        },
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.appShapes.medium
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.appShapes.medium
                    ) {
                        Text("Cancel")
                    }
                }
            } else {
                // Display mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to reorder",
                        modifier = dragHandleModifier
                            .size(20.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Icon(
                        imageVector = iconFromKey(reminder.icon).icon,
                        contentDescription = null,
                        tint = if (reminder.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (reminder.isActive) scheduleSummary(reminder) else "Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminder.isActive,
                        onCheckedChange = { viewModel.update(reminder.copy(isActive = it)) }
                    )
                }

                val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
                val timePattern = if (is24Hour) "HH:mm" else "h:mm a"

                Spacer(modifier = Modifier.height(12.dp))
                val today = LocalDate.now()
                val scheduledToday = reminder.isActive && isScheduledOn(reminder, today)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminder.reminderTimes.distinct().sorted().forEach { time ->
                        val log = todayLogs.firstOrNull { it.logDateTime.toLocalTime() == time }
                        TimeChip(
                            label = time.format(DateTimeFormatter.ofPattern(timePattern)),
                            completed = log?.completed == true,
                            actionable = log != null || scheduledToday,
                            onClick = {
                                if (log != null) {
                                    viewModel.updateLogCompletedStatus(log.id, !log.completed)
                                } else {
                                    // No log for this slot today (time had already passed when
                                    // the reminder was created/edited) — record it on demand
                                    viewModel.logAdHocCompletion(reminder, LocalDateTime.of(today, time))
                                }
                            }
                        )
                    }
                }

                if (reminder.isActive) {
                    val now = LocalDateTime.now()
                    nextOccurrence(reminder, now)?.let { next ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Next: ${formatNextOccurrence(next, now, timePattern)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                reminder.notes?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            // Re-seed the form from the current reminder so
                            // previously cancelled edits don't reappear
                            editedTitle = reminder.title
                            editedTimes = reminder.reminderTimes
                            editedFrequency = reminder.frequency
                            editedStartDate = reminder.startDate
                            editedEndDate = reminder.endDate
                            editedReminderDays = reminder.reminderDays
                            editedDayInterval = reminder.dayInterval ?: 1
                            useEveryNDays = reminder.dayInterval != null
                            editedNotes = reminder.notes ?: ""
                            editedIcon = reminder.icon ?: DEFAULT_ICON_KEY
                            isEditing = true
                        },
                        shape = MaterialTheme.appShapes.medium
                    ) {
                        Text("Edit")
                    }
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        shape = MaterialTheme.appShapes.medium
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete \"${reminder.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    shape = MaterialTheme.appShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStartDatePicker) {
        CalendarDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { editedStartDate = it; showStartDatePicker = false },
            initialDate = editedStartDate
        )
    }
    if (showEndDatePicker) {
        CalendarDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { editedEndDate = it; showEndDatePicker = false },
            initialDate = editedEndDate ?: LocalDate.now()
        )
    }
}

/**
 * One scheduled time as a chip. Tapping toggles (or records) today's
 * completion; chips are inert only when the reminder isn't scheduled today.
 */
@Composable
private fun TimeChip(
    label: String,
    completed: Boolean,
    actionable: Boolean,
    onClick: () -> Unit
) {
    val reminderColors = MaterialTheme.reminderColors

    Surface(
        shape = MaterialTheme.appShapes.small,
        color = when {
            completed -> reminderColors.completedContainer
            actionable -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            completed -> reminderColors.completedContent
            actionable -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (actionable && !completed) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        onClick = onClick,
        enabled = actionable
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (completed) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
