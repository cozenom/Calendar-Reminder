package com.example.calendarapp

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.calendarapp.ui.theme.CalendarAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.data.model.Reminder
import com.example.calendarapp.data.model.ReminderIcon
import com.example.calendarapp.data.model.ReminderLog
import com.example.calendarapp.data.notification.ReminderWorker
import com.example.calendarapp.ui.theme.dimensions
import com.example.calendarapp.ui.theme.reminderColors
import com.example.calendarapp.ui.theme.shapes
import com.example.calendarapp.viewmodel.ReminderViewModel
import com.example.calendarapp.viewmodel.ReminderViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ReminderViewModel
    private lateinit var alarmManager: AlarmManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

        requestRequiredPermissions()
        ReminderWorker.schedule(this)

        viewModel = ViewModelProvider(
            this, ReminderViewModelFactory(application)
        )[ReminderViewModel::class.java]

        setContent {
            CalendarAppTheme {
                ReminderApp(viewModel)
            }
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {}

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {}

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }
    }
}

@Composable
fun ReminderApp(viewModel: ReminderViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Reminders", "Calendar")
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index })
            }
        }
    }, floatingActionButton = {
        if (selectedTab == 0) {
            FloatingActionButton(onClick = { showAddReminderDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> RemindersTab(viewModel)
                1 -> CalendarTab(viewModel)
            }
        }
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onAddReminder = { reminder ->
                viewModel.insert(reminder)
                showAddReminderDialog = false
            }
        )
    }
}

@Composable
fun RemindersTab(viewModel: ReminderViewModel) {
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your Reminders", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
        ReminderList(
            reminders = reminders,
            onDeleteReminder = { viewModel.delete(it) },
            viewModel = viewModel
        )
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAddReminder: (reminder: Reminder) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Reminder") }, text = {
        AddReminderForm(onAddReminder = onAddReminder)
    }, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
fun ReminderList(
    reminders: List<Reminder>,
    onDeleteReminder: (Reminder) -> Unit,
    viewModel: ReminderViewModel
) {
    LazyColumn {
        items(reminders) { reminder ->
            ReminderItem(
                reminder = reminder,
                onDelete = { onDeleteReminder(reminder) },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onDelete: () -> Unit,
    viewModel: ReminderViewModel
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(reminder.title) }
    var editedTimes by remember { mutableStateOf(reminder.reminderTimes) }
    var editedFrequency by remember { mutableIntStateOf(reminder.frequency) }
    var editedStartDate by remember { mutableStateOf(reminder.startDate) }
    var editedEndDate by remember { mutableStateOf(reminder.endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var editedReminderDays by remember { mutableStateOf(reminder.reminderDays) }
    var editedNotes by remember { mutableStateOf(reminder.notes ?: "") }
    var editedIcon by remember { mutableStateOf(reminder.icon ?: ReminderIcon.default.key) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                FrequencySelector(frequency = editedFrequency, onFrequencyChange = {
                    editedFrequency = it
                    editedTimes = List(it) { index ->
                        if (index < editedTimes.size) editedTimes[index] else LocalTime.now()
                    }
                })
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                editedTimes.forEachIndexed { index, time ->
                    Material3TimePicker(initialTime = time, onTimeSelected = { newTime ->
                        editedTimes = editedTimes.toMutableList().also { it[index] = newTime }
                    })
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Start Date: ${editedStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
                }
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            if (editedEndDate != null) "End: ${editedEndDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                            else "Set End Date",
                            maxLines = 1
                        )
                    }
                    if (editedEndDate != null) {
                        TextButton(onClick = { editedEndDate = null }, shape = MaterialTheme.shapes.medium) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                WeekdaySelector(selectedDays = editedReminderDays, onDaysChanged = { editedReminderDays = it })
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
                Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                IconPicker(selectedKey = editedIcon, onIconSelected = { editedIcon = it })
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.update(
                                reminder.copy(
                                    title = editedTitle,
                                    reminderTimes = editedTimes,
                                    frequency = editedFrequency,
                                    startDate = editedStartDate,
                                    endDate = editedEndDate,
                                    reminderDays = editedReminderDays,
                                    notes = editedNotes.ifBlank { null },
                                    icon = editedIcon
                                )
                            )
                            isEditing = false
                        },
                        enabled = editedTitle.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Save")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cancel")
                    }
                }
            } else {
                // Display mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ReminderIcon.fromKey(reminder.icon).icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                    )
                    Text(reminder.title, style = MaterialTheme.typography.headlineSmall)
                }
                reminder.reminderTimes.forEachIndexed { index, time ->
                    Text("Time ${index + 1}: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
                Text("Frequency: ${reminder.frequency} times daily")
                Text("Start Date: ${reminder.startDate}")
                reminder.endDate?.let { Text("End Date: $it") }
                Text("Days: ${reminder.reminderDays.sorted().joinToString(", ") { getDayName(it) }}")
                reminder.notes?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Edit")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
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

@Composable
fun AddReminderForm(onAddReminder: (reminder: Reminder) -> Unit) {
    var title by remember { mutableStateOf("Reminder") }
    var frequency by remember { mutableStateOf(1) }
    var reminderTimes by remember { mutableStateOf(listOf(LocalTime.now().plusMinutes(2))) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var notes by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(ReminderIcon.default.key) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        FrequencySelector(frequency = frequency, onFrequencyChange = {
            frequency = it
            reminderTimes = List(it) { index ->
                if (index < reminderTimes.size) reminderTimes[index] else LocalTime.now().plusMinutes(2)
            }
        })
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        reminderTimes.forEachIndexed { index, time ->
            Material3TimePicker(initialTime = time, onTimeSelected = { newTime ->
                reminderTimes = reminderTimes.toMutableList().also { it[index] = newTime }
            })
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
        }

        androidx.compose.material3.OutlinedButton(
            onClick = { showStartDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Start Date: ${startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    if (endDate != null) "End: ${endDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                    else "Set End Date",
                    maxLines = 1
                )
            }
            if (endDate != null) {
                TextButton(onClick = { endDate = null }, shape = MaterialTheme.shapes.medium) {
                    Text("Clear")
                }
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

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

        WeekdaySelector(selectedDays = reminderDays, onDaysChanged = { reminderDays = it })
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        IconPicker(selectedKey = selectedIcon, onIconSelected = { selectedIcon = it })
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    val reminder = Reminder(
                        title = title,
                        reminderTimes = reminderTimes,
                        frequency = frequency,
                        startDate = startDate,
                        endDate = endDate,
                        reminderDays = reminderDays,
                        notes = notes.ifBlank { null },
                        icon = selectedIcon
                    )
                    onAddReminder(reminder)
                    title = "Reminder"
                    frequency = 1
                    reminderTimes = listOf(LocalTime.now().plusMinutes(2))
                    startDate = LocalDate.now()
                    endDate = null
                    reminderDays = setOf(1, 2, 3, 4, 5, 6, 7)
                    notes = ""
                    selectedIcon = ReminderIcon.default.key
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Add Reminder", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun FrequencySelector(frequency: Int, onFrequencyChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Daily Frequency:")
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { if (frequency > 1) onFrequencyChange(frequency - 1) },
            modifier = Modifier.width(MaterialTheme.dimensions.frequencyButtonWidth),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "-", fontSize = 20.sp)
        }
        Text(text = frequency.toString(), modifier = Modifier.padding(horizontal = 8.dp))
        Button(
            onClick = { onFrequencyChange(frequency + 1) },
            modifier = Modifier.width(MaterialTheme.dimensions.frequencyButtonWidth),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = "+", fontSize = 20.sp)
        }
    }
}

@Composable
fun WeekdaySelector(selectedDays: Set<Int>, onDaysChanged: (Set<Int>) -> Unit) {
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekdays.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index + 1)
            WeekdayButton(day = day, isSelected = isSelected, onClick = {
                val newSet = if (isSelected) selectedDays - (index + 1) else selectedDays + (index + 1)
                onDaysChanged(newSet)
            })
        }
    }
}

@Composable
fun WeekdayButton(day: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(MaterialTheme.dimensions.weekdayButtonSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day, color = contentColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3TimePicker(initialTime: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(initialTime) }
    var showTimePicker by remember { mutableStateOf(false) }
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = is24HourFormat
    )

    val timeFormat = if (is24HourFormat) "HH:mm" else "hh:mm a"

    androidx.compose.material3.OutlinedButton(
        onClick = { showTimePicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Text("Time: ${selectedTime.format(DateTimeFormatter.ofPattern(timeFormat))}")
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
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
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            onTimeSelected(selectedTime)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTab(viewModel: ReminderViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedLog by remember { mutableStateOf<ReminderLog?>(null) }

    val initialPage = 600
    val baseYearMonth = YearMonth.now()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1200 })
    val coroutineScope = rememberCoroutineScope()

    val currentMonth = remember(pagerState.currentPage) {
        baseYearMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    val activeReminders by viewModel.getActiveReminders(selectedDate).collectAsState(initial = emptyList())
    val selectedDateLogs by viewModel.getLogsForDate(selectedDate).collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Calendar", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        // Month navigation
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        // Legend
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reminderColors = MaterialTheme.reminderColors
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(MaterialTheme.dimensions.indicatorDotLarge).background(reminderColors.completedIndicator, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(MaterialTheme.dimensions.indicatorDotLarge).background(reminderColors.pendingIndicator, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Swipeable calendar
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) { page ->
                val monthForPage = baseYearMonth.plusMonths((page - initialPage).toLong())
                val logsForPage by viewModel.getLogsForMonth(monthForPage).collectAsState(initial = emptyList())

                CalendarView(
                    currentMonth = monthForPage,
                    onDateSelected = { selectedDate = it },
                    selectedDate = selectedDate,
                    logs = logsForPage
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        // Selected date's reminders
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

                if (selectedDateLogs.isEmpty()) {
                    Text("No reminders scheduled for this day")
                } else {
                    LazyColumn {
                        items(selectedDateLogs.groupBy { it.title }.values.toList()) { titleLogs ->
                            titleLogs.forEachIndexed { _, log ->
                                ReminderEventItem(log = log, onClick = { selectedLog = log })
                            }
                        }
                    }
                }
            }
        }
    }

    selectedLog?.let { log ->
        val reminder = activeReminders.find { it.id == log.reminderId }
        EventDetailsDialog(
            log = log,
            onDismiss = { selectedLog = null },
            onStatusChange = { newStatus ->
                viewModel.updateLogCompletedStatus(log.id, newStatus)
                selectedLog = null
            }
        )
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    selectedDate: LocalDate?,
    logs: List<ReminderLog>
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value
    val totalDays = daysInMonth + firstDayOfMonth - 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        content = {
            items(7) { index ->
                Text(
                    text = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")[index],
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            items(totalDays) { index ->
                if (index >= firstDayOfMonth - 1) {
                    val day = index - firstDayOfMonth + 2
                    val date = currentMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val dayLogs = logs.filter { it.logDateTime.toLocalDate() == date }

                    Column(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onDateSelected(date) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.toString(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        FlexibleDotRow(logs = dayLogs, maxDots = 8)
                    }
                } else {
                    Text("")
                }
            }
        }
    )
}

@Composable
fun FlexibleDotRow(logs: List<ReminderLog>, maxDots: Int) {
    val reminderColors = MaterialTheme.reminderColors
    Row(
        modifier = Modifier.fillMaxWidth().height(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val displayedLogs = logs.take(maxDots)
        displayedLogs.forEach { log ->
            val dotColor = if (log.completed) reminderColors.completedIndicator else reminderColors.pendingIndicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .background(dotColor, CircleShape)
            )
        }
        if (logs.size > maxDots) {
            Text("+", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun CalendarDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate
) {
    var selectedDate by remember { mutableStateOf(initialDate) }

    val initialPage = 600
    val baseYearMonth = YearMonth.from(initialDate)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1200 })
    val coroutineScope = rememberCoroutineScope()

    val currentMonth = remember(pagerState.currentPage) {
        baseYearMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        },
        text = {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                val monthForPage = baseYearMonth.plusMonths((page - initialPage).toLong())
                CalendarView(
                    currentMonth = monthForPage,
                    onDateSelected = { selectedDate = it; onDateSelected(it) },
                    selectedDate = selectedDate,
                    logs = emptyList()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest, shape = MaterialTheme.shapes.medium) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReminderEventItem(log: ReminderLog, onClick: () -> Unit) {
    val reminderColors = MaterialTheme.reminderColors
    val containerColor = if (log.completed) reminderColors.completedContainer else reminderColors.pendingContainer
    val contentColor = if (log.completed) reminderColors.completedContent else reminderColors.pendingContent
    val statusIcon = if (log.completed) "✓" else "✗"

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = statusIcon, style = MaterialTheme.typography.titleMedium, color = contentColor, modifier = Modifier.padding(end = 12.dp))
            Text(
                text = log.logDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.width(60.dp),
                color = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = log.title, style = MaterialTheme.typography.bodyLarge, color = contentColor, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun EventDetailsDialog(log: ReminderLog, onDismiss: () -> Unit, onStatusChange: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(log.title) },
        text = {
            Column {
                Text("Time: ${log.logDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                Text("Status: ${if (log.completed) "Completed" else "Pending"}")
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                Button(
                    onClick = { onStatusChange(!log.completed) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(if (log.completed) "✗ Mark as Pending" else "✓ Mark as Done")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.medium) { Text("Close") }
        }
    )
}

@Composable
fun IconPicker(selectedKey: String?, onIconSelected: (String) -> Unit) {
    val options = ReminderIcon.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { option ->
            val isSelected = (selectedKey ?: ReminderIcon.default.key) == option.key
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onIconSelected(option.key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = option.icon, contentDescription = option.label, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

fun getDayName(day: Int): String {
    return when (day) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}
