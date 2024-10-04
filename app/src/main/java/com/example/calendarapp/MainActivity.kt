package com.example.calendarapp

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.viewmodel.MedicationReminderViewModel
import com.example.calendarapp.viewmodel.MedicationReminderViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.notification.MedicationReminderWorker
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MedicationReminderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MedicationReminderWorker.schedule(this)
        viewModel = ViewModelProvider(
            this,
            MedicationReminderViewModelFactory(application)
        )[MedicationReminderViewModel::class.java]

        setContent {
            MaterialTheme {
                MedicationReminderApp(viewModel)
            }
        }
    }
}

@Composable
fun MedicationReminderApp(viewModel: MedicationReminderViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Medications", "Calendar", "Add Medication")

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }
        when (selectedTab) {
            0 -> MedicationsTab(viewModel)
            1 -> CalendarTab(viewModel)
            2 -> AddMedicationTab(viewModel)
        }
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
            onEditReminder = { viewModel.update(it) },
            onTakenChange = { intake, taken ->
                viewModel.updateIntakeTakenStatus(intake.id, taken)
            }
        )
    }
}


@Composable
fun AddMedicationTab(viewModel: MedicationReminderViewModel) {
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add New Medication", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        AddReminderForm(
            onAddReminder = { viewModel.insert(it) },
            reminders = reminders
        )
    }
}

@Composable
fun ReminderList(
    reminders: List<MedicationReminder>,
    intakes: List<MedicationIntake>,
    onDeleteReminder: (MedicationReminder) -> Unit,
    onEditReminder: (MedicationReminder) -> Unit,
    onTakenChange: (MedicationIntake, Boolean) -> Unit
) {
    LazyColumn {
        items(reminders) { reminder ->
            ReminderItem(
                reminder = reminder,
                intakes = intakes.filter { it.reminderId == reminder.id },
                onDelete = { onDeleteReminder(reminder) },
                onEdit = { onEditReminder(it) },
                onTakenChange = onTakenChange
            )
        }
    }
}

@Composable
fun ReminderItem(
    reminder: MedicationReminder,
    intakes: List<MedicationIntake>,
    onDelete: () -> Unit,
    onEdit: (MedicationReminder) -> Unit,
    onTakenChange: (MedicationIntake, Boolean) -> Unit
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Medication Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                FrequencySelector(
                    frequency = editedFrequency,
                    onFrequencyChange = {
                        editedFrequency = it
                        editedTimes = List(it) { index ->
                            if (index < editedTimes.size) editedTimes[index] else LocalTime.now()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                editedTimes.forEachIndexed { index, time ->
                    AndroidTimePicker(
                        initialTime = time,
                        onTimeSelected = { newTime ->
                            editedTimes = editedTimes.toMutableList().also { it[index] = newTime }
                        }
                    )
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
                        onEdit(reminder.copy(
                            medicationName = editedName,
                            reminderTimes = editedTimes,
                            frequency = editedFrequency,
                            startDate = editedStartDate,
                            endDate = editedEndDate,
                            reminderDays = editedReminderDays
                        ))
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
                Text(
                    "Days: ${
                        reminder.reminderDays.sorted().joinToString(", ") { getDayName(it) }
                    }"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display intakes with taken status
                intakes.forEach { intake ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intake at ${intake.intakeDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = intake.taken,
                            onCheckedChange = { taken -> onTakenChange(intake, taken) }
                        )
                    }
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
}

@Composable
fun AddReminderForm(onAddReminder: (MedicationReminder) -> Unit, reminders: List<MedicationReminder>) {    var medicationName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(1) }
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

        FrequencySelector(
            frequency = frequency,
            onFrequencyChange = {
                frequency = it
                reminderTimes = List(it) { index ->
                    if (index < reminderTimes.size) reminderTimes[index] else LocalTime.now()
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        reminderTimes.forEachIndexed { index, time ->
            AndroidTimePicker(
                initialTime = time,
                onTimeSelected = { newTime ->
                    reminderTimes = reminderTimes.toMutableList().also { it[index] = newTime }
                }
            )
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
            CalendarDialog(
                onDismissRequest = { showStartDatePicker = false },
                onDateSelected = {
                    startDate = it
                    showStartDatePicker = false
                },
                initialDate = startDate,
                reminders = reminders
            )
        }

        if (showEndDatePicker) {
            CalendarDialog(
                onDismissRequest = { showEndDatePicker = false },
                onDateSelected = {
                    endDate = it
                    showEndDatePicker = false
                },
                initialDate = endDate ?: LocalDate.now(),
                reminders = reminders
            )
        }

        WeekdaySelector(
            selectedDays = reminderDays,
            onDaysChanged = { reminderDays = it }
        )
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Reminder")
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
        Button(onClick = { if (frequency > 1) onFrequencyChange(frequency - 1) }) {
            Text("-")
        }
        Text(
            text = frequency.toString(),
            modifier = Modifier.padding(horizontal = 8.dp)
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
            WeekdayButton(
                day = day,
                isSelected = isSelected,
                onClick = {
                    val newSet = if (isSelected) {
                        selectedDays - (index + 1)
                    } else {
                        selectedDays + (index + 1)
                    }
                    onDaysChanged(newSet)
                }
            )
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
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
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

    Button(onClick = {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                onTimeSelected(selectedTime)
            },
            selectedTime.hour,
            selectedTime.minute,
            false // Set to true if you want 24-hour view
        ).show()
    }) {
        Text("Select Time: ${selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
    }
}

@Composable
fun CalendarTab(viewModel: MedicationReminderViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var showDayView by remember { mutableStateOf(false) }
    var selectedIntake by remember { mutableStateOf<MedicationIntake?>(null) }

    val activeReminders by viewModel.getActiveReminders(selectedDate)
        .collectAsState(initial = emptyList())
    val intakes by viewModel.getIntakesForDate(selectedDate)
        .collectAsState(initial = emptyList())

    val indicatorColor = MaterialTheme.colorScheme.secondary

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
            onDateSelected = {
                selectedDate = it
                showDayView = true
            },
            selectedDate = selectedDate,
            reminders = activeReminders,
            indicatorColor = indicatorColor
        )

        if (showDayView) {
            DayView(
                date = selectedDate,
                intakes = intakes,
                onIntakeClick = { intake ->
                    selectedIntake = intake
                }
            )
        }
    }

    selectedIntake?.let { intake ->
        EventDetailsDialog(
            intake = intake,
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
    reminders: List<MedicationReminder>,
    indicatorColor: Color
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
                    modifier = Modifier.padding(4.dp)
                )
            }
            items(totalDays) { index ->
                if (index >= firstDayOfMonth - 1) {
                    val day = index - firstDayOfMonth + 2
                    val date = currentMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val hasReminders = reminders.any { reminder ->
                        reminder.startDate <= date && (reminder.endDate == null || reminder.endDate >= date)
                    }

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
                        if (hasReminders) {
                            Canvas(modifier = Modifier
                                .size(6.dp)
                                .align(Alignment.CenterHorizontally)) {
                                drawCircle(
                                    color = indicatorColor,
                                    radius = size.minDimension / 2
                                )
                            }
                        }
                    }
                } else {
                    Text("")
                }
            }
        }
    )
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

    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                    reminders = reminders,
                    indicatorColor = indicatorColor
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DayView(
    date: LocalDate,
    intakes: List<MedicationIntake>,
    onIntakeClick: (MedicationIntake) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        intakes.sortedBy { it.intakeDateTime }.forEach { intake ->
            MedicationEventItem(
                intake = intake,
                onClick = { onIntakeClick(intake) }
            )
        }
    }
}

@Composable
fun MedicationEventItem(
    intake: MedicationIntake,
    onClick: () -> Unit
) {
    val backgroundColor = if (intake.taken) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = intake.intakeDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = intake.medicationName,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EventDetailsDialog(
    intake: MedicationIntake,
    onDismiss: () -> Unit,
    onStatusChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(intake.medicationName) },
        text = {
            Column {
                Text("Time: ${intake.intakeDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                Text("Status: ${if (intake.taken) "Taken" else "Not Taken"}")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onStatusChange(true) },
                        enabled = !intake.taken
                    ) {
                        Text("Mark as Taken")
                    }
                    Button(
                        onClick = { onStatusChange(false) },
                        enabled = intake.taken
                    ) {
                        Text("Mark as Not Taken")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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