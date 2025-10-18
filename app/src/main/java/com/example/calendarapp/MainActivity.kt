package com.example.calendarapp

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.notification.MedicationReminderWorker
import com.example.calendarapp.viewmodel.MedicationReminderViewModel
import com.example.calendarapp.viewmodel.MedicationReminderViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import android.text.format.DateFormat
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MedicationReminderViewModel
    private lateinit var alarmManager: AlarmManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied.
                }
            }


        requestRequiredPermissions()
        MedicationReminderWorker.schedule(this)
        Log.d("MainActivity", "Scheduled MedicationReminderWorker")
        // Schedule the worker
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "MedicationReminderWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        viewModel = ViewModelProvider(
            this, MedicationReminderViewModelFactory(application)
        )[MedicationReminderViewModel::class.java]

        setContent {
            MaterialTheme {
                MedicationReminderApp(viewModel)
            }
        }

    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show an explanation to the user *asynchronously*
                }

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

@Composable
fun MedicationReminderApp(viewModel: MedicationReminderViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Medications", "Calendar")
    var showAddMedicationDialog by remember { mutableStateOf(false) }

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
            FloatingActionButton(onClick = { showAddMedicationDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Medication")
            }
        }
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MedicationsTab(viewModel)
                1 -> CalendarTab(viewModel)
            }
        }
    }

    if (showAddMedicationDialog) {
        AddMedicationDialog(
            onDismiss = { showAddMedicationDialog = false },
            onAddReminder = { reminder ->
                viewModel.insert(reminder)
                showAddMedicationDialog = false
            },
            reminders = viewModel.allReminders.collectAsState(initial = emptyList()).value
        )
    }
}


@Composable
fun MedicationsTab(viewModel: MedicationReminderViewModel) {
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())
    val currentDate = remember { LocalDate.now() }
    val intakes by viewModel.getIntakesForDate(currentDate).collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your Medications", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        ReminderList(
            reminders = reminders,
            intakes = intakes,
            onDeleteReminder = { viewModel.delete(it) },
            onEditReminder = { viewModel.createOrUpdateReminder(it) }
        )
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAddReminder: (MedicationReminder) -> Unit,
    reminders: List<MedicationReminder>
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Medication") }, text = {
        AddReminderForm(
            onAddReminder = onAddReminder, reminders = reminders
        )
    }, confirmButton = {}, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    })
}

@Composable
fun ReminderList(
    reminders: List<MedicationReminder>,
    intakes: List<MedicationIntake>,
    onDeleteReminder: (MedicationReminder) -> Unit,
    onEditReminder: (MedicationReminder) -> Unit
) {
    LazyColumn {
        items(reminders) { reminder ->
            ReminderItem(
                reminder = reminder,
                intakes = intakes.filter { it.reminderId == reminder.id },
                onDelete = { onDeleteReminder(reminder) },
                onEdit = onEditReminder
            )
        }
    }
}

@Composable
fun ReminderItem(
    reminder: MedicationReminder,
    intakes: List<MedicationIntake>,
    onDelete: () -> Unit,
    onEdit: (MedicationReminder) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(reminder.medicationName) }
    var editedTimes by remember { mutableStateOf(reminder.reminderTimes) }
    var editedFrequency by remember { mutableIntStateOf(reminder.frequency) }
    var editedStartDate by remember { mutableStateOf(reminder.startDate) }
    var editedEndDate by remember { mutableStateOf(reminder.endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var editedReminderDays by remember { mutableStateOf(reminder.reminderDays) }

    val currentDate = LocalDate.now()
    val currentDayIntakes = intakes.filter { it.intakeDateTime.toLocalDate() == currentDate }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                // Editing mode UI
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Medication Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                FrequencySelector(frequency = editedFrequency, onFrequencyChange = {
                    editedFrequency = it
                    editedTimes = List(it) { index ->
                        if (index < editedTimes.size) editedTimes[index] else LocalTime.now()
                    }
                })
                Spacer(modifier = Modifier.height(8.dp))
                editedTimes.forEachIndexed { index, time ->
                    AndroidTimePicker(initialTime = time, onTimeSelected = { newTime ->
                        editedTimes = editedTimes.toMutableList().also { it[index] = newTime }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(onClick = { showStartDatePicker = true }) {
                    Text("Select Start Date: ${editedStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = { showEndDatePicker = true }) {
                        Text("Select End Date: ${editedEndDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Not set"}")
                    }
                    if (editedEndDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { editedEndDate = null }) {
                            Text("Clear End Date")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                WeekdaySelector(
                    selectedDays = editedReminderDays,
                    onDaysChanged = { editedReminderDays = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = {
                        val updatedReminder = reminder.copy(
                            medicationName = editedName.ifBlank { "Medication" },
                            reminderTimes = editedTimes,
                            frequency = editedFrequency,
                            startDate = editedStartDate,
                            endDate = editedEndDate,
                            reminderDays = editedReminderDays
                        )
                        onEdit(updatedReminder)
                        isEditing = false
                    }) {
                        Text("Save")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                }
            } else {
                // Display mode UI
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        reminder.medicationName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                reminder.reminderTimes.forEachIndexed { index, time ->
                    Text("Time ${index + 1}: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
                Text("Frequency: ${reminder.frequency} times daily")
                Text("Start Date: ${reminder.startDate}")
                reminder.endDate?.let { Text("End Date: $it") }
                Text("Days: ${
                    reminder.reminderDays.sorted().joinToString(", ") { getDayName(it) }
                }")

                Spacer(modifier = Modifier.height(8.dp))

                // Display intakes for the current day with status
                Text("Today's intakes:", style = MaterialTheme.typography.titleMedium)
                if (currentDayIntakes.isNotEmpty()) {
                    currentDayIntakes.forEach { intake ->
                        val backgroundColor = if (intake.taken) {
                            Color(200, 255, 200) // Light green background for taken
                        } else {
                            Color(255, 200, 200) // Light red background for not taken
                        }
                        val textColor = if (intake.taken) {
                            Color(0, 100, 0) // Dark green text for taken
                        } else {
                            Color(150, 0, 0) // Dark red text for not taken
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Intake at ${
                                    intake.intakeDateTime.format(
                                        DateTimeFormatter.ofPattern(
                                            "HH:mm"
                                        )
                                    )
                                }",
                                modifier = Modifier.weight(1f),
                                color = textColor
                            )
                            Text(
                                text = if (intake.taken) "Taken" else "Not Taken",
                                color = textColor
                            )
                        }
                    }
                } else {
                    Text("No intakes scheduled for today")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Button(onClick = { isEditing = true }) {
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDelete) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        CalendarDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = {
                editedStartDate = it
                showStartDatePicker = false
            },
            initialDate = editedStartDate,
            reminders = emptyList() // TODO You may want to pass actual reminders here
        )
    }
    if (showEndDatePicker) {
        CalendarDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = {
                editedEndDate = it
                showEndDatePicker = false
            },
            initialDate = editedEndDate ?: LocalDate.now(),
            reminders = emptyList() // TODO You may want to pass actual reminders here
        )
    }
}

@Composable
fun AddReminderForm(
    onAddReminder: (MedicationReminder) -> Unit, reminders: List<MedicationReminder>
) {
    var medicationName by remember { mutableStateOf("Medication") }
    var frequency by remember { mutableIntStateOf(1) }
    var reminderTimes by remember { mutableStateOf(listOf(LocalTime.now())) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = medicationName,
            onValueChange = { medicationName = it },
            label = { Text("Medication Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        FrequencySelector(frequency = frequency, onFrequencyChange = {
            frequency = it
            reminderTimes = List(it) { index ->
                if (index < reminderTimes.size) reminderTimes[index] else LocalTime.now()
            }
        })
        Spacer(modifier = Modifier.height(8.dp))

        reminderTimes.forEachIndexed { index, time ->
            AndroidTimePicker(initialTime = time, onTimeSelected = { newTime ->
                reminderTimes = reminderTimes.toMutableList().also { it[index] = newTime }
            })
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = { showStartDatePicker = true }) {
            Text("Select Start Date: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = { showEndDatePicker = true }) {
                Text("Select End Date: ${endDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Not set"}")
            }
            if (endDate != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { endDate = null }) {
                    Text("Clear End Date")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (showStartDatePicker) {
            CalendarDialog(onDismissRequest = { showStartDatePicker = false }, onDateSelected = {
                startDate = it
                showStartDatePicker = false
            }, initialDate = startDate, reminders = reminders
            )
        }

        if (showEndDatePicker) {
            CalendarDialog(onDismissRequest = { showEndDatePicker = false }, onDateSelected = {
                endDate = it
                showEndDatePicker = false
            }, initialDate = endDate ?: LocalDate.now(), reminders = reminders
            )
        }

        WeekdaySelector(selectedDays = reminderDays, onDaysChanged = { reminderDays = it })
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (medicationName.isNotBlank()) {
                    val reminder = MedicationReminder(
                        medicationName = medicationName,
                        reminderTimes = reminderTimes,
                        frequency = frequency,
                        startDate = startDate,
                        endDate = endDate,
                        reminderDays = reminderDays
                    )
                    onAddReminder(reminder)
                    // Reset form fields
                    medicationName = ""
                    frequency = 1
                    reminderTimes = listOf(LocalTime.now())
                    startDate = LocalDate.now()
                    endDate = null
                    reminderDays = setOf(1, 2, 3, 4, 5, 6, 7)
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Reminder")
        }
    }
}

@Composable
fun FrequencySelector(frequency: Int, onFrequencyChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
    ) {
        Text("Daily Frequency:")
        Spacer(modifier = Modifier.width(4.dp))
        Button(onClick = { if (frequency > 1) onFrequencyChange(frequency - 1) }) {
            Text("-")
        }
        Text(
            text = frequency.toString(), modifier = Modifier.padding(horizontal = 4.dp)
        )
        Button(onClick = { onFrequencyChange(frequency + 1) }) {
            Text("+")
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
                val newSet = if (isSelected) {
                    selectedDays - (index + 1)
                } else {
                    selectedDays + (index + 1)
                }
                onDaysChanged(newSet)
            })
        }
    }
}

@Composable
fun WeekdayButton(day: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape
            ), contentAlignment = Alignment.Center
    ) {
        Text(
            text = day, color = contentColor, style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AndroidTimePicker(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(initialTime) }
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }

    val timeFormat = if (is24HourFormat) "HH:mm" else "hh:mm a"

    Button(onClick = {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                onTimeSelected(selectedTime)
            },
            selectedTime.hour,
            selectedTime.minute,
            is24HourFormat // Use the system setting for 24-hour format
        ).show()
    }) {
        Text("Select Time: ${selectedTime.format(DateTimeFormatter.ofPattern(timeFormat))}")
    }
}

@Composable
fun CalendarTab(viewModel: MedicationReminderViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var selectedIntake by remember {
        mutableStateOf<Pair<MedicationIntake, MedicationReminder>?>(
            null
        )
    }

    val intakesWithReminders by viewModel.getIntakesWithRemindersForMonth(currentMonth)
        .collectAsState(initial = emptyList())
    val selectedDateIntakesWithReminders by viewModel.getIntakesWithRemindersForDate(selectedDate)
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Medication Calendar", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        CalendarView(
            currentMonth = currentMonth,
            onDateSelected = { selectedDate = it },
            selectedDate = selectedDate,
            intakesWithReminders = intakesWithReminders
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show selected date's intakes
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDateIntakesWithReminders.isEmpty()) {
            Text("No medications scheduled for this day")
        } else {
            LazyColumn {
                items(selectedDateIntakesWithReminders) { (intake, reminder) ->
                    MedicationEventItem(
                        intake = intake,
                        reminder = reminder,
                        onClick = { selectedIntake = intake to reminder }
                    )
                }
            }
        }
    }

    selectedIntake?.let { (intake, reminder) ->
        EventDetailsDialog(
            intake = intake,
            reminder = reminder,
            onDismiss = { selectedIntake = null },
            onStatusChange = { newStatus ->
                viewModel.updateIntakeTakenStatus(intake.id, newStatus)
                selectedIntake = null
            }
        )
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    selectedDate: LocalDate?,
    intakesWithReminders: List<Pair<MedicationIntake, MedicationReminder>>
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value
    val totalDays = daysInMonth + firstDayOfMonth - 1

    // Generate a map of medication names to color offsets
    val medicationColorOffsets = remember(intakesWithReminders) {
        intakesWithReminders.map { it.second.medicationName }.distinct().mapIndexed { index, name ->
            name to generateColorOffset(
                index,
                intakesWithReminders.map { it.second.medicationName }.distinct().size
            )
        }.toMap()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        content = {
            items(7) { index ->
                Text(
                    text = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")[index],
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
            items(totalDays) { index ->
                if (index >= firstDayOfMonth - 1) {
                    val day = index - firstDayOfMonth + 2
                    val date = currentMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val dayIntakes =
                        intakesWithReminders.filter { it.first.intakeDateTime.toLocalDate() == date }

                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { onDateSelected(date) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(4.dp)
                    ) {
                        Text(
                            text = day.toString(),
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        FlexibleDotRow(
                            intakes = dayIntakes,
                            medicationColorOffsets = medicationColorOffsets,
                            maxDots = 8
                        )
                    }
                } else {
                    Text("")
                }
            }
        }
    )
}

@Composable
fun FlexibleDotRow(
    intakes: List<Pair<MedicationIntake, MedicationReminder>>,
    medicationColorOffsets: Map<String, Float>,
    maxDots: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val displayedIntakes = intakes.take(maxDots)
        displayedIntakes.forEach { (intake, reminder) ->
            val colorOffset = medicationColorOffsets[reminder.medicationName] ?: 0f
            val dotColor = if (intake.taken) {
                generateGreenHue(colorOffset)
            } else {
                generateRedHue(colorOffset)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .background(dotColor, CircleShape)
            )
        }
        if (intakes.size > maxDots) {
            Text(
                "+",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


fun generateColorOffset(index: Int, total: Int): Float {
    return (index.toFloat() / total) * 30f // Generate offsets within a 30-degree range
}

fun generateGreenHue(offset: Float): Color {
    return Color.hsl((120f + offset) % 360, 0.7f, 0.5f)
}

fun generateRedHue(offset: Float): Color {
    return Color.hsl((0f + offset) % 360, 0.7f, 0.5f)
}

@Composable
fun CalendarDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate,
    reminders: List<MedicationReminder>
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val indicatorColor = MaterialTheme.colorScheme.secondary

    AlertDialog(onDismissRequest = onDismissRequest,
        title = { Text(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))) },
        text = {
            Column {
                Row {
                    Button(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Text("Previous")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Text("Next")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                CalendarView(
                    currentMonth = currentMonth,
                    onDateSelected = {
                        selectedDate = it
                        onDateSelected(it)
                    },
                    selectedDate = selectedDate,
                    intakesWithReminders = emptyList() // TODO You may want to pass actual intakes here
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        })
}

@Composable
fun MedicationEventItem(
    intake: MedicationIntake,
    reminder: MedicationReminder,
    onClick: () -> Unit
) {
    val backgroundColor = if (intake.taken) {
        Color(200, 255, 200) // Light green background for taken
    } else {
        Color(255, 200, 200) // Light red background for not taken
    }
    val textColor = if (intake.taken) {
        Color(0, 100, 0) // Dark green text for taken
    } else {
        Color(150, 0, 0) // Dark red text for not taken
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = intake.intakeDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
            color = textColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = reminder.medicationName,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Composable
fun EventDetailsDialog(
    intake: MedicationIntake,
    reminder: MedicationReminder,
    onDismiss: () -> Unit,
    onStatusChange: (Boolean) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(reminder.medicationName) }, text = {
        Column {
            Text("Time: ${intake.intakeDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            Text("Status: ${if (intake.taken) "Taken" else "Not Taken"}")
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onStatusChange(true) }, enabled = !intake.taken
                ) {
                    Text("Taken")
                }
                Button(
                    onClick = { onStatusChange(false) }, enabled = intake.taken
                ) {
                    Text("Not Taken")
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    })
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