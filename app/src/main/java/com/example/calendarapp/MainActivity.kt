package com.example.calendarapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.viewmodel.MedicationReminderViewModel
import com.example.calendarapp.viewmodel.MedicationReminderViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MedicationReminderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this,
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

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your Medications", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        ReminderList(
            reminders = reminders,
            onDeleteReminder = { viewModel.delete(it) },
            onEditReminder = { viewModel.update(it) }
        )
    }
}

@Composable
fun CalendarTab(viewModel: MedicationReminderViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val activeReminders by viewModel.getActiveReminders(selectedDate).collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Medication Calendar", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        DatePicker(
            date = selectedDate,
            onDateSelected = { it?.let { selectedDate = it } },
            label = "Select Date"
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(activeReminders) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    onDelete = { viewModel.delete(reminder) },
                    onEdit = { viewModel.update(it) }
                )
            }
        }
    }
}

@Composable
fun AddMedicationTab(viewModel: MedicationReminderViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add New Medication", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        AddReminderForm(onAddReminder = { viewModel.insert(it) })
    }
}

@Composable
fun ReminderList(
    reminders: List<MedicationReminder>,
    onDeleteReminder: (MedicationReminder) -> Unit,
    onEditReminder: (MedicationReminder) -> Unit
) {
    LazyColumn {
        items(reminders) { reminder ->
            ReminderItem(
                reminder = reminder,
                onDelete = { onDeleteReminder(reminder) },
                onEdit = { onEditReminder(it) }
            )
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


@Composable
fun AddReminderForm(onAddReminder: (MedicationReminder) -> Unit) {
    var medicationName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(1) }
    var reminderTimes by remember { mutableStateOf(listOf(LocalTime.now())) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var hasEndDate by remember { mutableStateOf(false) }
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
            ClockTimePicker(
                time = time,
                onTimeSelected = { newTime ->
                    reminderTimes = reminderTimes.toMutableList().also { it[index] = newTime }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        DatePicker(
            date = startDate,
            onDateSelected = { startDate = it },
            label = "Start Date"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = hasEndDate,
                onCheckedChange = { hasEndDate = it }
            )
            Text("Set End Date")
        }

        if (hasEndDate) {
            DatePicker(
                date = endDate,
                onDateSelected = { endDate = it },
                label = "End Date"
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

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
                        endDate = if (hasEndDate) endDate else null,
                        reminderDays = reminderDays
                    )
                    onAddReminder(reminder)
                    // Reset form fields
                    medicationName = ""
                    frequency = 1
                    reminderTimes = listOf(LocalTime.now())
                    startDate = LocalDate.now()
                    endDate = LocalDate.now()
                    hasEndDate = false
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
fun ReminderItem(
    reminder: MedicationReminder,
    onDelete: () -> Unit,
    onEdit: (MedicationReminder) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(reminder.medicationName) }
    var editedTimes by remember { mutableStateOf(reminder.reminderTimes) }
    var editedFrequency by remember { mutableStateOf(reminder.frequency) }
    var editedStartDate by remember { mutableStateOf(reminder.startDate) }
    var editedEndDate by remember { mutableStateOf(reminder.endDate ?: LocalDate.now()) }
    var hasEndDate by remember { mutableStateOf(reminder.endDate != null) }
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
                    ClockTimePicker(
                        time = time,
                        onTimeSelected = { newTime ->
                            editedTimes = editedTimes.toMutableList().also { it[index] = newTime }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                DatePicker(
                    date = editedStartDate,
                    onDateSelected = { editedStartDate = it },
                    label = "Start Date"
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasEndDate,
                        onCheckedChange = { hasEndDate = it }
                    )
                    Text("Set End Date")
                }

                if (hasEndDate) {
                    DatePicker(
                        date = editedEndDate,
                        onDateSelected = { editedEndDate = it },
                        label = "End Date"
                    )
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
                            endDate = if (hasEndDate) editedEndDate else null,
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
                Text(reminder.medicationName, style = MaterialTheme.typography.headlineSmall)
                reminder.reminderTimes.forEachIndexed { index, time ->
                    Text("Time ${index + 1}: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
                Text("Frequency: ${reminder.frequency} times daily")
                Text("Start Date: ${reminder.startDate}")
                reminder.endDate?.let { Text("End Date: $it") }
                Text("Days: ${reminder.reminderDays.sorted().joinToString(", ") { getDayName(it) }}")
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
fun ClockTimePicker(time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) {
        Text("Select Time: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
    }

    if (showDialog) {
        ClockTimePickerDialog(
            onDismissRequest = { showDialog = false },
            onConfirm = {
                onTimeSelected(it)
                showDialog = false
            },
            initialTime = time
        )
    }
}

@Composable
fun ClockTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ClockFace(
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    onHourSelected = { selectedHour = it },
                    onMinuteSelected = { selectedMinute = it }
                )
                Text(
                    "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(LocalTime.of(selectedHour, selectedMinute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClockFace(
    selectedHour: Int,
    selectedMinute: Int,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit
) {
    var isHourSelection by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .size(250.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val touchPoint = change.position - center
                    val angle = (atan2(touchPoint.y, touchPoint.x) * 180f / PI).toFloat()
                    val normalizedAngle = (angle + 360f) % 360f

                    if (isHourSelection) {
                        val hour = ((normalizedAngle + 15f) / 30f).toInt() % 12
                        onHourSelected(if (hour == 0) 12 else hour)
                    } else {
                        val minute = ((normalizedAngle + 3f) / 6f).toInt() % 60
                        onMinuteSelected(minute)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - 20.dp.toPx()

            // Draw clock face
            drawCircle(
                color = Color.LightGray,
                center = center,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw clock numbers
            val numberRadius = radius - 30.dp.toPx()
            for (i in 1..12) {
                val angle = i * 30f * (PI / 180f).toFloat()
                val x = center.x + cos(angle) * numberRadius
                val y = center.y + sin(angle) * numberRadius
                drawCircle(Color.Gray, 5.dp.toPx(), Offset(x, y))
            }

            // Draw selected time indicator
            val selectedAngle = if (isHourSelection) {
                (selectedHour % 12) * 30f
            } else {
                selectedMinute * 6f
            }
            val radians = selectedAngle * (PI / 180f).toFloat()
            val lineEnd = Offset(
                x = center.x + cos(radians) * radius,
                y = center.y + sin(radians) * radius
            )
            drawLine(Color.Blue, center, lineEnd, strokeWidth = 4.dp.toPx())
        }

        Button(
            onClick = { isHourSelection = !isHourSelection },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(if (isHourSelection) "Switch to Minutes" else "Switch to Hours")
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
    Column {
        Text("Select days:", style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { index, day ->
                val isSelected = selectedDays.contains(index + 1)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .toggleable(
                            value = isSelected,
                            onValueChange = {
                                val newSet = if (it) {
                                    selectedDays + (index + 1)
                                } else {
                                    selectedDays - (index + 1)
                                }
                                onDaysChanged(newSet)
                            }
                        )
                        .padding(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        Text(day)
                    }
                }
            }
        }
    }
}
@Composable
fun TimePicker(time: LocalTime, onTimeSelected: (LocalTime) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) {
        Text("Select Time: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
    }

    if (showDialog) {
        TimePickerDialog(
            onDismissRequest = { showDialog = false },
            onConfirm = {
                onTimeSelected(it)
                showDialog = false
            },
            initialTime = time
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    initialTime: LocalTime
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column {
                NumberPicker(
                    value = selectedHour,
                    onValueChange = { selectedHour = it },
                    range = 0..23
                )
                NumberPicker(
                    value = selectedMinute,
                    onValueChange = { selectedMinute = it },
                    range = 0..59
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(LocalTime.of(selectedHour, selectedMinute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Text("-")
        }
        Text(value.toString().padStart(2, '0'))
        Button(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Text("+")
        }
    }
}

@Composable
fun DatePicker(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    label: String
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }) {
        Text("$label: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            onDateSelected = {
                onDateSelected(it)
                showDialog = false
            },
            initialDate = date
        )
    }
}

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate
) {
    var selectedDate by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Date") },
        text = {
            Column {
                Row {
                    Button(onClick = { selectedDate = selectedDate.minusYears(1) }) {
                        Text("◀")
                    }
                    Text(selectedDate.year.toString(), modifier = Modifier.weight(1f))
                    Button(onClick = { selectedDate = selectedDate.plusYears(1) }) {
                        Text("▶")
                    }
                }
                Row {
                    Button(onClick = { selectedDate = selectedDate.minusMonths(1) }) {
                        Text("◀")
                    }
                    Text(selectedDate.month.toString(), modifier = Modifier.weight(1f))
                    Button(onClick = { selectedDate = selectedDate.plusMonths(1) }) {
                        Text("▶")
                    }
                }
                Row {
                    Button(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                        Text("◀")
                    }
                    Text(selectedDate.dayOfMonth.toString(), modifier = Modifier.weight(1f))
                    Button(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                        Text("▶")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDate) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WeekdayPicker(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
) {
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row {
        weekdays.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index + 1)
            Button(
                onClick = {
                    val newSet = if (isSelected) {
                        selectedDays - (index + 1)
                    } else {
                        selectedDays + (index + 1)
                    }
                    onDaysChanged(newSet)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(day)
            }
        }
    }
}