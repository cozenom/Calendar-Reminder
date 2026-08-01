package com.davidp.simpleweeklyreminders

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.davidp.simpleweeklyreminders.data.model.DEFAULT_ICON_KEY
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Default for a newly added time slot: a couple of minutes from now, on a whole minute. */
private fun defaultNewTime(): LocalTime =
    LocalTime.now().plusMinutes(2).truncatedTo(ChronoUnit.MINUTES)

/**
 * Bottom sheet with the shared add/edit reminder form.
 * Pass [initial] = null to create a new reminder, or an existing one to edit it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormSheet(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ReminderForm(initial = initial, onSave = onSave, onCancel = onDismiss)
    }
}

@Composable
private fun ReminderForm(
    initial: Reminder?,
    onSave: (Reminder) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "Reminder") }
    var times by remember { mutableStateOf(initial?.reminderTimes ?: listOf(defaultNewTime())) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(initial?.endDate) }
    var reminderDays by remember { mutableStateOf(initial?.reminderDays ?: setOf(1, 2, 3, 4, 5, 6, 7)) }
    var recurrenceMode by remember { mutableStateOf(initial?.reminderType ?: ReminderType.SPECIFIC_DAYS) }
    var dayInterval by remember { mutableIntStateOf(initial?.dayInterval ?: 1) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var selectedIcon by remember { mutableStateOf(initial?.icon ?: DEFAULT_ICON_KEY) }
    // Null for a new reminder — HIGH is a migration default (see Reminder.kt), so picking
    // a level is mandatory here
    var importance by remember { mutableStateOf(initial?.importance) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Text(
            if (initial == null) "New Reminder" else "Edit Reminder",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        // Identity: icon + title together; tapping the icon opens the picker
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = { showIconPicker = true },
                shape = MaterialTheme.appShapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconFromKey(selectedIcon).icon,
                        contentDescription = "Change icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(MaterialTheme.dimensions.spacingSmall))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(onClick = { title = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        RecurrenceToggle(mode = recurrenceMode, onChanged = { recurrenceMode = it })
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
        when (recurrenceMode) {
            ReminderType.EVERY_N_DAYS -> DayIntervalSelector(interval = dayInterval, onIntervalChange = { dayInterval = it })
            ReminderType.SPECIFIC_DAYS -> WeekdaySelector(selectedDays = reminderDays, onDaysChanged = { reminderDays = it })
            ReminderType.ONE_TIME -> {} // single date picked below, nothing to select here
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        ImportanceSelector(importance = importance, onChanged = { importance = it })
        if (importance == null) {
            Text(
                "Choose an importance level",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        Text(
            "Times",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        times.forEachIndexed { index, time ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimePickerField(
                    time = time,
                    onTimeSelected = { newTime ->
                        times = times.toMutableList().also { it[index] = newTime }
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { times = times.toMutableList().also { it.removeAt(index) } },
                    // A reminder without times would silently never fire
                    enabled = times.size > 1
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove time")
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
        }
        TextButton(
            onClick = { times = times + defaultNewTime() },
            shape = MaterialTheme.appShapes.medium
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add time")
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        OutlinedButton(
            onClick = { showStartDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.appShapes.medium
        ) {
            val label = if (recurrenceMode == ReminderType.ONE_TIME) "Date" else "Start Date"
            Text("$label: ${startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        val endBeforeStart = recurrenceMode != ReminderType.ONE_TIME && endDate?.isBefore(startDate) == true
        val oneTimeInPast = recurrenceMode == ReminderType.ONE_TIME &&
            times.all { LocalDateTime.of(startDate, it) <= LocalDateTime.now() }
        if (recurrenceMode != ReminderType.ONE_TIME) {
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
                        if (endDate != null) "End: ${endDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                        else "Set End Date",
                        maxLines = 1
                    )
                }
                if (endDate != null) {
                    TextButton(onClick = { endDate = null }, shape = MaterialTheme.appShapes.medium) {
                        Text("Clear")
                    }
                }
            }
            if (endBeforeStart) {
                Text(
                    "End date is before the start date, so this reminder would never fire",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (oneTimeInPast) {
            Text(
                "This date and time have already passed, so this reminder would never fire",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val base = initial ?: Reminder(title = "", reminderTimes = emptyList())
                    // distinct(): the same time entered twice would create duplicate
                    // logs, leaving an orphan that always shows up as "missed"
                    val distinctTimes = times.distinct()
                    val isOneTime = recurrenceMode == ReminderType.ONE_TIME
                    onSave(
                        base.copy(
                            title = title.ifBlank { "Reminder" },
                            reminderTimes = distinctTimes,
                            startDate = startDate,
                            endDate = if (isOneTime) startDate else endDate,
                            reminderDays = if (isOneTime) setOf(startDate.dayOfWeek.value) else reminderDays,
                            notes = notes.ifBlank { null },
                            icon = selectedIcon,
                            dayInterval = if (recurrenceMode == ReminderType.EVERY_N_DAYS) dayInterval else null,
                            reminderType = recurrenceMode,
                            importance = requireNotNull(importance)
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium,
                enabled = !endBeforeStart && !oneTimeInPast && importance != null
            ) {
                Text(if (initial == null) "Add Reminder" else "Save")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium
            ) {
                Text("Cancel")
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
    }

    if (showStartDatePicker) {
        CalendarDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { startDate = it; showStartDatePicker = false },
            initialDate = startDate
        )
    }
    if (showEndDatePicker) {
        CalendarDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { endDate = it; showEndDatePicker = false },
            initialDate = endDate ?: LocalDate.now()
        )
    }
    if (showIconPicker) {
        IconPickerDialog(
            currentKey = selectedIcon,
            onIconSelected = { selectedIcon = it },
            onDismiss = { showIconPicker = false }
        )
    }
}

/**
 * Stateless time field: shows [time] and opens a Material 3 time picker on tap.
 * Unlike the old Material3TimePicker it doesn't cache the time internally, so
 * rows stay correct when a time is removed from the list above them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timeFormat = if (is24HourFormat) "HH:mm" else "hh:mm a"

    OutlinedButton(
        onClick = { showTimePicker = true },
        modifier = modifier,
        shape = MaterialTheme.appShapes.medium
    ) {
        Text("Time: ${time.format(DateTimeFormatter.ofPattern(timeFormat))}")
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            val timePickerState = rememberTimePickerState(
                initialHour = time.hour,
                initialMinute = time.minute,
                is24Hour = is24HourFormat
            )
            Surface(
                shape = MaterialTheme.appShapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute))
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
