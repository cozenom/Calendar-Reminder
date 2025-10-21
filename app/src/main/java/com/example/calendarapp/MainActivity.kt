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
import android.text.format.DateFormat
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.model.PrescriptionRefill
import com.example.calendarapp.data.notification.MedicationReminderWorker
import com.example.calendarapp.viewmodel.MedicationReminderViewModel
import com.example.calendarapp.viewmodel.MedicationReminderViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MedicationReminderViewModel
    private lateinit var alarmManager: AlarmManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

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
    var selectedTab by remember { mutableStateOf(0) }
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
            onAddReminder = { reminder, pillsPerRefill, totalRefills ->
                viewModel.insertWithPrescription(reminder, pillsPerRefill, totalRefills)
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
            viewModel = viewModel
        )
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAddReminder: (reminder: MedicationReminder, pillsPerRefill: Int, totalRefills: Int) -> Unit,
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
    viewModel: MedicationReminderViewModel
) {
    LazyColumn {
        items(reminders) { reminder ->
            ReminderItem(
                reminder = reminder,
                intakes = intakes.filter { it.reminderId == reminder.id },
                onDelete = { onDeleteReminder(reminder) },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ReminderItem(
    reminder: MedicationReminder,
    intakes: List<MedicationIntake>,
    onDelete: () -> Unit,
    viewModel: MedicationReminderViewModel
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
    var showRecordRefillDialog by remember { mutableStateOf(false) }
    var showNewPrescriptionDialog by remember { mutableStateOf(false) }
    var editedDosagePerIntake by remember { mutableStateOf(reminder.dosagePerIntake.toString()) }
    var editedCurrentInventory by remember { mutableStateOf(reminder.currentInventory.toString()) }
    var editedInventoryTrackingEnabled by remember { mutableStateOf(reminder.inventoryTrackingEnabled) }
    var editedRefillPeriodDays by remember { mutableStateOf(reminder.refillPeriodDays.toString()) }

    // Get latest refill info
    val latestRefill by viewModel.getLatestRefillFlow(reminder.id)
        .collectAsState(initial = null)

    // Initialize prescription fields with actual values from reminder
    var editedPillsPerRefill by remember { mutableStateOf(reminder.prescriptionPillsPerRefill.toString()) }
    var editedTotalRefills by remember { mutableStateOf(reminder.prescriptionTotalRefills.toString()) }

    val currentDate = LocalDate.now()
    val currentDayIntakes = intakes.filter { it.intakeDateTime.toLocalDate() == currentDate }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
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
                    selectedDays = editedReminderDays, onDaysChanged = { editedReminderDays = it })
                Spacer(modifier = Modifier.height(16.dp))

                // Prescription tracking section - editable
                androidx.compose.material3.Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Prescription Tracking")
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = editedInventoryTrackingEnabled,
                        onCheckedChange = { editedInventoryTrackingEnabled = it }
                    )
                }

                if (editedInventoryTrackingEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedDosagePerIntake,
                        onValueChange = { editedDosagePerIntake = it },
                        label = { Text("Medication per dose") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedCurrentInventory,
                        onValueChange = { editedCurrentInventory = it },
                        label = { Text("Current medication count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedPillsPerRefill,
                        onValueChange = { editedPillsPerRefill = it },
                        label = { Text("Medication count per refill") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedTotalRefills,
                        onValueChange = { editedTotalRefills = it },
                        label = { Text("Total refills authorized") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedRefillPeriodDays,
                        onValueChange = { editedRefillPeriodDays = it },
                        label = { Text("Refill period (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Show refills info if available
                    latestRefill?.let {
                        Text(
                            "Refills remaining: ${it.refillsRemaining} / ${it.totalRefillsAuthorized}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (it.refillsRemaining == 0) Color.Red else Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Refill management buttons
                    if ((latestRefill?.refillsRemaining ?: 0) > 0) {
                        Button(
                            onClick = { showRecordRefillDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Record Refill Pickup")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Validation
                val isEditFormValid = editedName.isNotBlank() &&
                    (!editedInventoryTrackingEnabled || (
                        editedDosagePerIntake.isNotEmpty() && editedDosagePerIntake.toIntOrNull() != null &&
                        editedCurrentInventory.isNotEmpty() && editedCurrentInventory.toIntOrNull() != null &&
                        editedPillsPerRefill.isNotEmpty() && editedPillsPerRefill.toIntOrNull() != null &&
                        editedTotalRefills.isNotEmpty() && editedTotalRefills.toIntOrNull() != null &&
                        editedRefillPeriodDays.isNotEmpty() && editedRefillPeriodDays.toIntOrNull() != null
                    ))

                Row {
                    Button(
                        onClick = {
                            val updatedReminder = reminder.copy(
                                medicationName = editedName,
                                reminderTimes = editedTimes,
                                frequency = editedFrequency,
                                startDate = editedStartDate,
                                endDate = editedEndDate,
                                reminderDays = editedReminderDays,
                                dosagePerIntake = editedDosagePerIntake.toIntOrNull() ?: 1,
                                currentInventory = editedCurrentInventory.toIntOrNull() ?: 0,
                                inventoryTrackingEnabled = editedInventoryTrackingEnabled,
                                refillPeriodDays = editedRefillPeriodDays.toIntOrNull() ?: 30,
                                prescriptionPillsPerRefill = editedPillsPerRefill.toIntOrNull() ?: 60,
                                prescriptionTotalRefills = editedTotalRefills.toIntOrNull() ?: 5
                            )
                            viewModel.updateWithPrescription(
                                updatedReminder,
                                editedPillsPerRefill.toIntOrNull() ?: 60,
                                editedTotalRefills.toIntOrNull() ?: 5
                            )
                            isEditing = false
                        },
                        enabled = isEditFormValid
                    ) {
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
                                .padding(8.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Intake at ${
                                    intake.intakeDateTime.format(
                                        DateTimeFormatter.ofPattern(
                                            "HH:mm"
                                        )
                                    )
                                }", modifier = Modifier.weight(1f), color = textColor
                            )
                            Text(
                                text = if (intake.taken) "Taken" else "Not Taken", color = textColor
                            )
                        }
                    }
                } else {
                    Text("No intakes scheduled for today")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Inventory status display
                if (reminder.inventoryTrackingEnabled) {
                    androidx.compose.material3.Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Prescription Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Inventory count with color coding
                    val inventoryColor = when {
                        reminder.currentInventory == 0 -> Color.Red
                        reminder.currentInventory <= 10 -> Color(255, 165, 0) // Orange
                        else -> Color(0, 128, 0) // Green
                    }

                    Text(
                        "Medication remaining: ${reminder.currentInventory}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = inventoryColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )

                    Text(
                        "Dosage: ${reminder.dosagePerIntake} per dose",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Show refills info if available
                    latestRefill?.let {
                        Text(
                            "Refills remaining: ${it.refillsRemaining} / ${it.totalRefillsAuthorized}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (it.refillsRemaining == 0) Color.Red else Color.Black,
                            fontWeight = if (it.refillsRemaining == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (reminder.currentInventory <= 10 && reminder.currentInventory > 0) {
                        Text(
                            "⚠ Low inventory! Time to pick up refill.",
                            color = Color(255, 165, 0),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else if (reminder.currentInventory == 0) {
                        Text(
                            "⚠ No medication remaining!",
                            color = Color.Red,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Refill management buttons
                    Spacer(modifier = Modifier.height(8.dp))

                    if ((latestRefill?.refillsRemaining ?: 0) > 0) {
                        Button(
                            onClick = { showRecordRefillDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Record Refill Pickup")
                        }
                    } else if (latestRefill?.refillsRemaining == 0) {
                        Button(
                            onClick = { showNewPrescriptionDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add New Prescription")
                        }
                        Text(
                            "⚠ No refills left. Get new prescription from doctor.",
                            color = Color.Red,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

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
            reminders = emptyList() // You may want to pass actual reminders here
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
            reminders = emptyList() // You may want to pass actual reminders here
        )
    }

    // Record Refill Dialog
    if (showRecordRefillDialog) {
        latestRefill?.let { refill ->
            RecordRefillDialog(
                reminder = reminder,
                latestRefill = refill,
                onDismiss = { showRecordRefillDialog = false },
                onRecordRefill = { pickupDate ->
                    viewModel.recordRefillPickup(reminder.id, reminder, refill, pickupDate)
                    showRecordRefillDialog = false
                }
            )
        }
    }

    // New Prescription Dialog
    if (showNewPrescriptionDialog) {
        NewPrescriptionDialog(
            reminder = reminder,
            onDismiss = { showNewPrescriptionDialog = false },
            onAddPrescription = { pillsPerRefill, totalRefills ->
                viewModel.recordNewPrescription(reminder.id, pillsPerRefill, totalRefills)
                showNewPrescriptionDialog = false
            }
        )
    }
}

@Composable
fun AddReminderForm(
    onAddReminder: (reminder: MedicationReminder, pillsPerRefill: Int, totalRefills: Int) -> Unit,
    reminders: List<MedicationReminder>
) {
    var medicationName by remember { mutableStateOf("Medication") }
    var frequency by remember { mutableStateOf(1) }
    var reminderTimes by remember { mutableStateOf(listOf(LocalTime.now())) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    // Prescription tracking fields
    var inventoryTrackingEnabled by remember { mutableStateOf(false) }
    var dosagePerIntake by remember { mutableStateOf("1") }
    var currentInventory by remember { mutableStateOf("") }
    var pillsPerRefill by remember { mutableStateOf("60") }
    var totalRefills by remember { mutableStateOf("5") }
    var refillPeriodDays by remember { mutableStateOf("30") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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

        // Prescription tracking section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Prescription Tracking")
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = inventoryTrackingEnabled,
                onCheckedChange = { inventoryTrackingEnabled = it }
            )
        }

        if (inventoryTrackingEnabled) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = dosagePerIntake,
                onValueChange = { dosagePerIntake = it },
                label = { Text("Medication per dose") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = currentInventory,
                onValueChange = { currentInventory = it },
                label = { Text("Current medication count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pillsPerRefill,
                onValueChange = { pillsPerRefill = it },
                label = { Text("Medication count per refill") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = totalRefills,
                onValueChange = { totalRefills = it },
                label = { Text("Total refills authorized") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = refillPeriodDays,
                onValueChange = { refillPeriodDays = it },
                label = { Text("Refill period (days)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Validation
        val isFormValid = medicationName.isNotBlank() &&
            (!inventoryTrackingEnabled || (
                dosagePerIntake.isNotEmpty() && dosagePerIntake.toIntOrNull() != null &&
                currentInventory.isNotEmpty() && currentInventory.toIntOrNull() != null &&
                pillsPerRefill.isNotEmpty() && pillsPerRefill.toIntOrNull() != null &&
                totalRefills.isNotEmpty() && totalRefills.toIntOrNull() != null &&
                refillPeriodDays.isNotEmpty() && refillPeriodDays.toIntOrNull() != null
            ))

        Button(
            onClick = {
                if (medicationName.isNotBlank()) {
                    val reminder = MedicationReminder(
                        medicationName = medicationName,
                        reminderTimes = reminderTimes,
                        frequency = frequency,
                        startDate = startDate,
                        endDate = endDate,
                        reminderDays = reminderDays,
                        dosagePerIntake = dosagePerIntake.toIntOrNull() ?: 1,
                        currentInventory = currentInventory.toIntOrNull() ?: 0,
                        inventoryTrackingEnabled = inventoryTrackingEnabled,
                        refillPeriodDays = refillPeriodDays.toIntOrNull() ?: 30,
                        prescriptionPillsPerRefill = pillsPerRefill.toIntOrNull() ?: 60,
                        prescriptionTotalRefills = totalRefills.toIntOrNull() ?: 5
                    )
                    onAddReminder(reminder, pillsPerRefill.toIntOrNull() ?: 60, totalRefills.toIntOrNull() ?: 5)
                    // Reset form fields
                    medicationName = ""
                    frequency = 1
                    reminderTimes = listOf(LocalTime.now())
                    startDate = LocalDate.now()
                    endDate = null
                    reminderDays = setOf(1, 2, 3, 4, 5, 6, 7)
                    inventoryTrackingEnabled = false
                    dosagePerIntake = "1"
                    currentInventory = ""
                    pillsPerRefill = "60"
                    totalRefills = "5"
                    refillPeriodDays = "30"
                }
            },
            enabled = isFormValid,
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
        Button(
            onClick = { if (frequency > 1) onFrequencyChange(frequency - 1) },
            modifier = Modifier.width(48.dp)
        ) {
            Text(
                text = "-",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Text(
            text = frequency.toString(),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Button(
            onClick = { onFrequencyChange(frequency + 1) },
            modifier = Modifier.width(48.dp)
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
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
    var selectedIntake by remember { mutableStateOf<MedicationIntake?>(null) }

    val activeReminders by viewModel.getActiveReminders(selectedDate)
        .collectAsState(initial = emptyList())
    val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())
    val intakes by viewModel.getIntakesForMonth(currentMonth).collectAsState(initial = emptyList())
    val selectedDateIntakes by viewModel.getIntakesForDate(selectedDate)
        .collectAsState(initial = emptyList())
    val refills by viewModel.getRefillsForMonth(currentMonth).collectAsState(initial = emptyList())
    val allRefills by viewModel.getAllRefills().collectAsState(initial = emptyList())

    // Calculate estimated refill due dates (based on custom refill period from last pickup)
    val estimatedRefillDates = remember(allReminders, allRefills) {
        allReminders.filter { it.inventoryTrackingEnabled }
            .flatMap { reminder ->
                // Find the latest refill for this reminder
                val latestRefillForReminder = allRefills
                    .filter { it.reminderId == reminder.id }
                    .maxByOrNull { it.pickupDate }

                // Generate estimated refill dates for all remaining refills from the last pickup
                if (latestRefillForReminder != null && latestRefillForReminder.refillsRemaining > 0) {
                    (1..latestRefillForReminder.refillsRemaining).map { multiplier ->
                        latestRefillForReminder.pickupDate.plusDays(
                            (reminder.refillPeriodDays * multiplier).toLong()
                        )
                    }
                } else {
                    emptyList()
                }
            }
    }

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
            onDateSelected = { selectedDate = it },
            selectedDate = selectedDate,
            reminders = activeReminders,
            intakes = intakes,
            refills = refills,
            estimatedRefillDates = estimatedRefillDates,
            indicatorColor = indicatorColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show selected date's intakes
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDateIntakes.isEmpty()) {
            Text("No medications scheduled for this day")
        } else {
            LazyColumn {
                items(selectedDateIntakes.groupBy { it.medicationName }.values.toList()) { medicationIntakes ->
                    medicationIntakes.forEachIndexed { index, intake ->
                        MedicationEventItem(intake = intake, onClick = { selectedIntake = intake })
                    }
                }
            }
        }
    }

    selectedIntake?.let { intake ->
        // Find the reminder for this intake
        val reminder = activeReminders.find { it.id == intake.reminderId }

        EventDetailsDialog(intake = intake,
            onDismiss = { selectedIntake = null },
            onStatusChange = { newStatus ->
                reminder?.let {
                    viewModel.updateIntakeTakenStatus(intake.id, newStatus, it)
                }
                selectedIntake = null
            })
    }
}

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    selectedDate: LocalDate?,
    reminders: List<MedicationReminder>,
    intakes: List<MedicationIntake>,
    refills: List<PrescriptionRefill>,
    estimatedRefillDates: List<LocalDate>,
    indicatorColor: Color
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value
    val totalDays = daysInMonth + firstDayOfMonth - 1

    // Generate a map of medication names to color offsets
    val medicationColorOffsets = remember(intakes) {
        intakes.map { it.medicationName }.distinct().mapIndexed { index, name ->
            name to generateColorOffset(index, intakes.map { it.medicationName }.distinct().size)
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
                    val dayIntakes = intakes.filter { it.intakeDateTime.toLocalDate() == date }
                    val dayRefills = refills.filter { it.pickupDate == date }
                    val hasRefill = dayRefills.isNotEmpty()
                    val hasEstimatedRefill = estimatedRefillDates.contains(date)

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.toString(),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasRefill) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color.Blue, CircleShape)
                                )
                            }
                            if (hasEstimatedRefill) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFFFFA500), CircleShape) // Orange color
                                )
                            }
                        }
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
    intakes: List<MedicationIntake>,
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
        displayedIntakes.forEach { intake ->
            val colorOffset = medicationColorOffsets[intake.medicationName] ?: 0f
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
                    reminders = reminders,
                    intakes = emptyList(),
                    refills = emptyList(),
                    estimatedRefillDates = emptyList(),
                    indicatorColor = indicatorColor
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
fun DayView(
    date: LocalDate, intakes: List<MedicationIntake>, onIntakeClick: (MedicationIntake) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        intakes.sortedBy { it.intakeDateTime }.forEach { intake ->
            MedicationEventItem(intake = intake, onClick = { onIntakeClick(intake) })
        }
    }
}

@Composable
fun MedicationEventItem(
    intake: MedicationIntake, onClick: () -> Unit
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
            text = intake.medicationName,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}

@Composable
fun EventDetailsDialog(
    intake: MedicationIntake, onDismiss: () -> Unit, onStatusChange: (Boolean) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(intake.medicationName) }, text = {
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
                    Text("Mark as Taken")
                }
                Button(
                    onClick = { onStatusChange(false) }, enabled = intake.taken
                ) {
                    Text("Mark as Not Taken")
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    })
}

@Composable
fun RecordRefillDialog(
    reminder: MedicationReminder,
    latestRefill: PrescriptionRefill,
    onDismiss: () -> Unit,
    onRecordRefill: (LocalDate) -> Unit
) {
    var pickupDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Refill Pickup") },
        text = {
            Column {
                Text(
                    "Medication: ${reminder.medicationName}",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Medication count will increase by: ${latestRefill.pillsPerRefill}")
                Text("Refills: ${latestRefill.refillsRemaining} → ${latestRefill.refillsRemaining - 1}")

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showDatePicker = true }) {
                    Text("Pickup Date: ${pickupDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRecordRefill(pickupDate) }) {
                Text("Record Pickup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        CalendarDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = {
                pickupDate = it
                showDatePicker = false
            },
            initialDate = pickupDate,
            reminders = emptyList()
        )
    }
}

@Composable
fun NewPrescriptionDialog(
    reminder: MedicationReminder,
    onDismiss: () -> Unit,
    onAddPrescription: (pillsPerRefill: Int, totalRefills: Int) -> Unit
) {
    var pillsPerRefill by remember { mutableStateOf("60") }
    var totalRefills by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Prescription") },
        text = {
            Column {
                Text(
                    "Medication: ${reminder.medicationName}",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pillsPerRefill,
                    onValueChange = { pillsPerRefill = it },
                    label = { Text("Medication count per refill") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = totalRefills,
                    onValueChange = { totalRefills = it },
                    label = { Text("Total refills authorized") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pills = pillsPerRefill.toIntOrNull() ?: 60
                    val refills = totalRefills.toIntOrNull() ?: 5
                    onAddPrescription(pills, refills)
                }
            ) {
                Text("Add Prescription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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