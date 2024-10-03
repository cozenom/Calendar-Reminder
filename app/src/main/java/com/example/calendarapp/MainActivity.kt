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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.viewmodel.MedicationReminderViewModel
import com.example.calendarapp.viewmodel.MedicationReminderViewModelFactory
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    val viewModel: MedicationReminderViewModel = viewModel()
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Medication Reminders", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        ReminderList(
            reminders = reminders,
            onDeleteReminder = { viewModel.delete(it) },
            onEditReminder = { viewModel.update(it) }
        )
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

@Composable
fun ReminderItem(
    reminder: MedicationReminder,
    onDelete: () -> Unit,
    onEdit: (MedicationReminder) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(reminder.medicationName) }
    var editedTime by remember { mutableStateOf(reminder.reminderTime) }
    var editedIsMorning by remember { mutableStateOf(reminder.isMorning) }

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
                    label = { Text("Medication Name") }
                )
                TimePicker(
                    time = editedTime,
                    onTimeSelected = { editedTime = it }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = editedIsMorning,
                        onCheckedChange = { editedIsMorning = it }
                    )
                    Text("Morning")
                }
                Row {
                    Button(onClick = {
                        onEdit(reminder.copy(medicationName = editedName, reminderTime = editedTime, isMorning = editedIsMorning))
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
                Text("Time: ${reminder.reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                Text(if (reminder.isMorning) "Morning" else "Evening")
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
fun AddReminderForm(onAddReminder: (MedicationReminder) -> Unit) {
    var medicationName by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf(LocalTime.now()) }
    var isMorning by remember { mutableStateOf(true) }

    Column {
        OutlinedTextField(
            value = medicationName,
            onValueChange = { medicationName = it },
            label = { Text("Medication Name") }
        )
        TimePicker(
            time = reminderTime,
            onTimeSelected = { reminderTime = it }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isMorning,
                onCheckedChange = { isMorning = it }
            )
            Text("Morning")
        }
        Button(onClick = {
            if (medicationName.isNotBlank()) {
                onAddReminder(
                    MedicationReminder(
                    medicationName = medicationName,
                    reminderTime = reminderTime,
                    isMorning = isMorning
                )
                )
                medicationName = ""
                reminderTime = LocalTime.now()
                isMorning = true
            }
        }) {
            Text("Add Reminder")
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