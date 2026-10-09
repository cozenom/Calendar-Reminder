package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.davidp.simpleweeklyreminders.data.model.DEFAULT_ICON_KEY
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.IntervalUnit
import com.davidp.simpleweeklyreminders.data.model.MonthlyMode
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.model.nthOccurrenceDate
import com.davidp.simpleweeklyreminders.data.settings.datePattern
import com.davidp.simpleweeklyreminders.data.settings.dateNoYearPattern
import com.davidp.simpleweeklyreminders.data.settings.is24Hour
import com.davidp.simpleweeklyreminders.data.settings.timePattern
import com.davidp.simpleweeklyreminders.debug.DebugTools
import com.davidp.simpleweeklyreminders.ui.calendar.CalendarDialog
import com.davidp.simpleweeklyreminders.ui.components.GroupSurface
import com.davidp.simpleweeklyreminders.ui.components.SectionLabel
import com.davidp.simpleweeklyreminders.ui.reminders.monthlyDayLabel
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import com.davidp.simpleweeklyreminders.ui.theme.reminderAccent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Default for a newly added time slot.
 * - Debug: 2 min out, so a test reminder fires fast.
 * - Release: the next full hour.
 */
private fun defaultNewTime(): LocalTime {
    val now = LocalTime.now()
    return if (DebugTools.ENABLED) now.plusMinutes(2).truncatedTo(ChronoUnit.MINUTES)
    else now.plusHours(1).truncatedTo(ChronoUnit.HOURS)
}

/**
 * Bottom sheet with the shared add/edit reminder form.
 * - [initial] = null: blank new reminder
 * - [initial] + [isNew] = true: new reminder prefilled from it (Duplicate)
 * - [initial] alone: edit it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormSheet(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    isNew: Boolean = initial == null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ReminderForm(initial = initial, isNew = isNew, onSave = onSave, onCancel = onDismiss)
    }
}

@Composable
private fun ReminderForm(
    initial: Reminder?,
    isNew: Boolean,
    onSave: (Reminder) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "Reminder") }
    var times by remember { mutableStateOf(initial?.reminderTimes ?: listOf(defaultNewTime())) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(initial?.endDate) }
    var reminderDays by remember { mutableStateOf(initial?.reminderDays ?: setOf(1, 2, 3, 4, 5, 6, 7)) }
    var recurrenceMode by remember { mutableStateOf(initial?.reminderType ?: ReminderType.SPECIFIC_DAYS) }
    var interval by remember { mutableIntStateOf(initial?.interval ?: 1) }
    var intervalUnit by remember { mutableStateOf(initial?.intervalUnit ?: IntervalUnit.DAYS) }
    var monthlyMode by remember { mutableStateOf(initial?.monthlyMode ?: MonthlyMode.DAY_OF_MONTH) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var selectedIcon by remember { mutableStateOf(initial?.icon ?: DEFAULT_ICON_KEY) }
    var selectedColor by remember { mutableStateOf(initial?.color) }
    // MEDIUM for a new reminder — the middle level is the safe assumption, and HIGH is
    // only a migration default (see Reminder.kt), not one to inherit silently
    var importance by remember { mutableStateOf(initial?.importance ?: Importance.MEDIUM) }
    var sound by remember { mutableStateOf(initial?.sound) }
    var snoozeMinutes by remember { mutableStateOf(initial?.snoozeMinutes) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    // Count for "End after N times"; null = end by date (or never)
    var endAfter by remember { mutableStateOf<Int?>(null) }
    var showEndMenu by remember { mutableStateOf(false) }
    var showEndCountDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        Text(
            if (isNew) "New reminder" else "Edit reminder",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 4.dp)
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
                        // Previews the reminder's own colour as soon as one is picked
                        tint = reminderAccent(selectedColor),
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f)
            )
        }
        // Only offered when the user has turned per-reminder colours on in Settings
        if (LocalAppSettings.current.perReminderColors) {
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            ColorSelector(selectedKey = selectedColor, onChanged = { selectedColor = it })
        }
        SectionLabel("Repeats")
        // Type is fixed once created, like the start date: a one-time reminder has a locked
        // past date it could never fire on, and a recurring one switched to one-time would
        // keep months of history under a schedule that no longer describes it. Delete and
        // re-add instead. The days/interval below stay editable.
        RecurrenceToggle(
            mode = recurrenceMode,
            onChanged = { recurrenceMode = it },
            enabled = isNew
        )
        when (recurrenceMode) {
            ReminderType.INTERVAL -> {
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                IntervalSelector(
                    interval = interval,
                    unit = intervalUnit,
                    onIntervalChange = { interval = it },
                    onUnitChange = { intervalUnit = it }
                )
                when (intervalUnit) {
                    IntervalUnit.WEEKS -> {
                        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                        WeekdaySelector(selectedDays = reminderDays, onDaysChanged = { reminderDays = it })
                    }
                    // Both choices read off the start date, so they relabel as it changes
                    IntervalUnit.MONTHS -> {
                        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                        SegmentedToggle(
                            options = MonthlyMode.entries.map { it to "On ${monthlyDayLabel(startDate, it)}" },
                            selected = monthlyMode,
                            onChanged = { monthlyMode = it }
                        )
                    }
                    IntervalUnit.DAYS, IntervalUnit.YEARS -> {} // start date says it all
                }
            }
            ReminderType.SPECIFIC_DAYS -> {
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                WeekdaySelector(selectedDays = reminderDays, onDaysChanged = { reminderDays = it })
            }
            ReminderType.ONE_TIME -> {} // single date picked below, nothing to select here
        }

        SectionLabel("Times")
        GroupSurface(Modifier.fillMaxWidth()) {
            times.forEachIndexed { index, time ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Remove time",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { times = times + defaultNewTime() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Add a time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SectionLabel("Importance")
        ImportanceSelector(importance = importance, onChanged = { importance = it })
        // Hidden for Low: its channel is silent, so a tone would never play
        if (LocalAppSettings.current.perReminderSounds && importance != Importance.LOW) {
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
            SoundSelector(sound = sound, onChanged = { sound = it })
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
        SnoozeSelector(minutes = snoozeMinutes, onChanged = { snoozeMinutes = it })

        val dateFormat = LocalAppSettings.current.dateFormat
        val datePattern = dateFormat.datePattern(LocalContext.current)
        val dateNoYearPattern = dateFormat.dateNoYearPattern(LocalContext.current)
        // "After N times" stays a count while the form is open, so changing the schedule moves
        // the end with it. Only the resulting date is saved: no column for the count.
        val countFrom = maxOf(startDate, LocalDate.now())
        val countedEnd = remember(endAfter, countFrom, reminderDays, recurrenceMode, interval, intervalUnit, monthlyMode) {
            endAfter?.let { n ->
                Reminder(
                    title = "",
                    reminderTimes = times,
                    startDate = startDate,
                    reminderDays = reminderDays,
                    interval = interval,
                    reminderType = recurrenceMode,
                    intervalUnit = intervalUnit,
                    monthlyMode = monthlyMode
                ).nthOccurrenceDate(n, countFrom)
            }
        }
        val effectiveEnd = if (endAfter != null) countedEnd else endDate
        val endBeforeStart = recurrenceMode != ReminderType.ONE_TIME && effectiveEnd?.isBefore(startDate) == true
        val oneTimeInPast = recurrenceMode == ReminderType.ONE_TIME &&
            times.all { LocalDateTime.of(startDate, it) <= LocalDateTime.now() }

        SectionLabel("Runs")
        GroupSurface(Modifier.fillMaxWidth()) {
            // Start date is fixed once created: its logs (incl. backfilled past ones) are
            // already generated, and moving it would desync them / shift the interval phase.
            ValueRow(
                label = if (recurrenceMode == ReminderType.ONE_TIME) "Date" else "Starts",
                value = startDate.format(DateTimeFormatter.ofPattern(datePattern)),
                onClick = { showStartDatePicker = true },
                enabled = isNew
            )
            if (recurrenceMode != ReminderType.ONE_TIME) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val endText = effectiveEnd?.format(DateTimeFormatter.ofPattern(dateNoYearPattern))
                // Box anchors the menu under the row
                Box {
                    ValueRow(
                        label = "Ends",
                        value = when {
                            endAfter != null -> listOfNotNull("After $endAfter times", endText).joinToString(" · ")
                            else -> endText ?: "Never"
                        },
                        muted = effectiveEnd == null && endAfter == null,
                        onClick = { showEndMenu = true },
                        trailing = {
                            if (endDate != null || endAfter != null) {
                                TextButton(onClick = { endDate = null; endAfter = null }) { Text("Clear") }
                            }
                        }
                    )
                    DropdownMenu(expanded = showEndMenu, onDismissRequest = { showEndMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("On a date") },
                            onClick = { showEndMenu = false; showEndDatePicker = true }
                        )
                        DropdownMenuItem(
                            text = { Text("After a number of times") },
                            onClick = { showEndMenu = false; showEndCountDialog = true }
                        )
                    }
                }
            }
        }
        if (!isNew) {
            Text(
                "Start date and repeat type are fixed once created — delete and re-add to change them",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
        if (endBeforeStart) {
            Text(
                "End date is before the start date, so this reminder would never fire",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
        if (oneTimeInPast) {
            Text(
                "This date and time have already passed, so this reminder would never fire",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }

        SectionLabel("Notes")
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("Optional") },
            shape = MaterialTheme.appShapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium
            ) {
                Text("Cancel", modifier = Modifier.padding(vertical = 6.dp))
            }
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
                            endDate = if (isOneTime) startDate else effectiveEnd,
                            reminderDays = if (isOneTime) setOf(startDate.dayOfWeek.value) else reminderDays,
                            notes = notes.ifBlank { null },
                            icon = selectedIcon,
                            color = selectedColor,
                            interval = if (recurrenceMode == ReminderType.INTERVAL) interval else null,
                            reminderType = recurrenceMode,
                            intervalUnit = intervalUnit,
                            monthlyMode = monthlyMode,
                            importance = importance,
                            // Kept even while hidden (Low, or setting off), like colour
                            sound = sound,
                            snoozeMinutes = snoozeMinutes
                        )
                    )
                },
                // Save is the wider of the two, as in the design
                modifier = Modifier.weight(1.4f),
                shape = MaterialTheme.appShapes.medium,
                enabled = !endBeforeStart && !oneTimeInPast
            ) {
                Text("Save", modifier = Modifier.padding(vertical = 6.dp))
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
        // Can't end before it starts, and a past end date would archive it on save
        val earliestEnd = maxOf(startDate, LocalDate.now())
        CalendarDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { endDate = it; endAfter = null; showEndDatePicker = false },
            initialDate = endDate?.takeIf { it >= earliestEnd } ?: earliestEnd,
            minDate = earliestEnd
        )
    }
    if (showEndCountDialog) {
        EndCountDialog(
            initial = endAfter,
            countFrom = maxOf(startDate, LocalDate.now()),
            onConfirm = { endAfter = it; showEndCountDialog = false },
            onDismiss = { showEndCountDialog = false }
        )
    }
    if (showIconPicker) {
        IconPickerSheet(
            currentKey = selectedIcon,
            onIconSelected = { selectedIcon = it },
            onDismiss = { showIconPicker = false }
        )
    }
}

private const val MAX_END_COUNT = 999

/**
 * Number entry for "End after N times". Counts from [countFrom] (today, or a future start).
 * - Starts empty (or the current count) with the keyboard up: typing is the only step
 * - Keyboard "Done" confirms
 */
@Composable
private fun EndCountDialog(
    initial: Int?,
    countFrom: LocalDate,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial?.toString().orEmpty()) }
    val count = text.toIntOrNull()?.takeIf { it in 1..MAX_END_COUNT }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val datePattern = LocalAppSettings.current.dateFormat.datePattern(LocalContext.current)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End after") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    suffix = { Text("times") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { count?.let(onConfirm) }),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Text(
                    "Counting days from ${countFrom.format(DateTimeFormatter.ofPattern(datePattern))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { count?.let(onConfirm) }, enabled = count != null) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Label on the left, current value on the right — the settings-style row used by "Runs". */
@Composable
private fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    muted: Boolean = false,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (muted || !enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
        trailing()
        Spacer(modifier = Modifier.width(6.dp))
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
    val timePref = LocalAppSettings.current.timeFormat
    val is24HourFormat = timePref.is24Hour(context)
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clickable { showTimePicker = true }
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = time.format(DateTimeFormatter.ofPattern(timePref.timePattern(context))),
            style = MaterialTheme.typography.titleMedium
        )
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
