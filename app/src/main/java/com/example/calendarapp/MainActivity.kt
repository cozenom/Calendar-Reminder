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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MedicationReminderViewModel
    private lateinit var alarmManager: AlarmManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
                // Permission request result is handled here if needed
            }

        requestRequiredPermissions()
        MedicationReminderWorker.schedule(this)

        viewModel = ViewModelProvider(
            this, MedicationReminderViewModelFactory(application)
        )[MedicationReminderViewModel::class.java]

        setContent {
            CalendarAppTheme {
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
                    data = "package:$packageName".toUri()
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
            }
        )
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
            viewModel = viewModel
        )
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAddReminder: (reminder: MedicationReminder, pillsPerRefill: Int, totalRefills: Int) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Medication") }, text = {
        AddReminderForm(
            onAddReminder = onAddReminder
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
    onDeleteReminder: (MedicationReminder) -> Unit,
    viewModel: MedicationReminderViewModel
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
    reminder: MedicationReminder,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
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
                    Material3TimePicker(initialTime = time, onTimeSelected = { newTime ->
                        editedTimes = editedTimes.toMutableList().also { it[index] = newTime }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Date: ${editedStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (editedEndDate != null) "End: ${editedEndDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                            else "Set End Date",
                            maxLines = 1
                        )
                    }
                    if (editedEndDate != null) {
                        TextButton(
                            onClick = { editedEndDate = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                WeekdaySelector(
                    selectedDays = editedReminderDays, onDaysChanged = { editedReminderDays = it })
                Spacer(modifier = Modifier.height(16.dp))

                // Prescription tracking section - editable
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        enabled = isEditFormValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Inventory status display
                if (reminder.inventoryTrackingEnabled) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        "Prescription Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calculate days of supply remaining
                    val dailyConsumption = reminder.frequency * reminder.dosagePerIntake
                    val daysRemaining = if (dailyConsumption > 0) {
                        reminder.currentInventory / dailyConsumption
                    } else 0

                    // Inventory count with 3-tier color coding based on days remaining
                    val inventoryContainerColor = when {
                        reminder.currentInventory == 0 -> Color(0xFFFFCDD2) // Darker red for empty
                        daysRemaining <= 3 -> Color(0xFFFFEBEE) // Light red for urgent (≤3 days)
                        daysRemaining < 7 -> Color(0xFFFFF3E0) // Light orange for warning (<7 days)
                        else -> Color(0xFFE8F5E9) // Light green for good stock (>7 days)
                    }
                    val inventoryContentColor = when {
                        reminder.currentInventory == 0 -> Color(0xFFB71C1C) // Dark red text for empty
                        daysRemaining <= 3 -> Color(0xFFC62828) // Dark red text for urgent
                        daysRemaining < 7 -> Color(0xFFE65100) // Dark orange text for warning
                        else -> Color(0xFF2E7D32) // Dark green text for good stock
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = inventoryContainerColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Medication Remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = inventoryContentColor
                                )
                                Text(
                                    "${reminder.currentInventory}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = inventoryContentColor
                                )
                            }
                            Text(
                                "${reminder.dosagePerIntake} per dose",
                                style = MaterialTheme.typography.bodyMedium,
                                color = inventoryContentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Show refills info if available
                    latestRefill?.let {
                        Text(
                            "Refills remaining: ${it.refillsRemaining} / ${it.totalRefillsAuthorized}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (it.refillsRemaining == 0) Color.Red else Color.Black,
                            fontWeight = if (it.refillsRemaining == 0) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    }

                    // Warning messages based on days remaining
                    if (daysRemaining <= 3 && reminder.currentInventory > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE), // Light red
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = Color(0xFFC62828) // Dark red
                                )
                                Text(
                                    "Only $daysRemaining days left! Refill urgently needed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = Color(0xFFC62828) // Dark red
                                )
                            }
                        }
                    } else if (daysRemaining < 7 && reminder.currentInventory > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0), // Light orange
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = Color(0xFFE65100) // Dark orange
                                )
                                Text(
                                    "$daysRemaining days remaining. Time to pick up refill.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = Color(0xFFE65100) // Dark orange
                                )
                            }
                        }
                    } else if (reminder.currentInventory == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFCDD2), // Darker red
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = Color(0xFFB71C1C) // Dark red
                                )
                                Text(
                                    "No medication remaining!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = Color(0xFFB71C1C) // Dark red
                                )
                            }
                        }
                    }

                    // Refill management buttons
                    Spacer(modifier = Modifier.height(8.dp))

                    if ((latestRefill?.refillsRemaining ?: 0) > 0) {
                        Button(
                            onClick = { showRecordRefillDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Record Refill Pickup")
                        }
                    } else if (latestRefill?.refillsRemaining == 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFCDD2), // Darker red
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "⚠",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(end = 8.dp),
                                        color = Color(0xFFB71C1C) // Dark red
                                    )
                                    Text(
                                        "No refills left. Get new prescription from doctor.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = Color(0xFFB71C1C) // Dark red
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showNewPrescriptionDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add New Prescription")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
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
            onDateSelected = {
                editedStartDate = it
                showStartDatePicker = false
            },
            initialDate = editedStartDate
        )
    }
    if (showEndDatePicker) {
        CalendarDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = {
                editedEndDate = it
                showEndDatePicker = false
            },
            initialDate = editedEndDate ?: LocalDate.now()
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
    onAddReminder: (reminder: MedicationReminder, pillsPerRefill: Int, totalRefills: Int) -> Unit
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
            Material3TimePicker(initialTime = time, onTimeSelected = { newTime ->
                reminderTimes = reminderTimes.toMutableList().also { it[index] = newTime }
            })
            Spacer(modifier = Modifier.height(8.dp))
        }

        androidx.compose.material3.OutlinedButton(
            onClick = { showStartDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Start Date: ${startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (endDate != null) "End: ${endDate?.format(DateTimeFormatter.ofPattern("MMM dd"))}"
                    else "Set End Date",
                    maxLines = 1
                )
            }
            if (endDate != null) {
                TextButton(
                    onClick = { endDate = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (showStartDatePicker) {
            CalendarDialog(onDismissRequest = { showStartDatePicker = false }, onDateSelected = {
                startDate = it
                showStartDatePicker = false
            }, initialDate = startDate
            )
        }

        if (showEndDatePicker) {
            CalendarDialog(onDismissRequest = { showEndDatePicker = false }, onDateSelected = {
                endDate = it
                showEndDatePicker = false
            }, initialDate = endDate ?: LocalDate.now()
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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
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
            modifier = Modifier.width(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "-",
                fontSize = 20.sp,
                color = Color.White
            )
        }
        Text(
            text = frequency.toString(),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Button(
            onClick = { onFrequencyChange(frequency + 1) },
            modifier = Modifier.width(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "+",
                fontSize = 20.sp,
                color = Color.White
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Material3TimePicker(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Time: ${selectedTime.format(DateTimeFormatter.ofPattern(timeFormat))}")
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )

                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                selectedTime = LocalTime.of(
                                    timePickerState.hour,
                                    timePickerState.minute
                                )
                                onTimeSelected(selectedTime)
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTab(viewModel: MedicationReminderViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedIntake by remember { mutableStateOf<MedicationIntake?>(null) }

    // Calculate initial page: 600 represents current month (allowing 50 years in both directions)
    val initialPage = 600
    val baseYearMonth = YearMonth.now()
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 1200 } // 100 years worth of months
    )
    val coroutineScope = rememberCoroutineScope()

    // Calculate current month based on pager position
    val currentMonth = remember(pagerState.currentPage) {
        baseYearMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    val activeReminders by viewModel.getActiveReminders(selectedDate)
        .collectAsState(initial = emptyList())
    val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())
    val selectedDateIntakes by viewModel.getIntakesForDate(selectedDate)
        .collectAsState(initial = emptyList())
    val allRefills by viewModel.getAllRefills().collectAsState(initial = emptyList())

    // Calculate estimated refill due dates (based on custom refill period from last pickup)
    val estimatedRefillDates = remember(allReminders, allRefills) {
        allReminders.filter { it.inventoryTrackingEnabled }
            .flatMap { reminder ->
                // Find the latest refill for this reminder
                val latestRefillForReminder = allRefills
                    .filter { it.reminderId == reminder.id }
                    .maxWithOrNull(compareBy({ it.pickupDate }, { it.id }))

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

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Medication Calendar", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Month navigation
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blue dot - Actual refills
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Blue, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Pickup",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Orange dot - Estimated refills
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFFA500), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Est. Refill",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Green/Red dots - Intakes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFF44336), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Doses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Swipeable calendar with HorizontalPager
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) { page ->
                val monthForPage = baseYearMonth.plusMonths((page - initialPage).toLong())
                val intakesForPage by viewModel.getIntakesForMonth(monthForPage)
                    .collectAsState(initial = emptyList())
                val refillsForPage by viewModel.getRefillsForMonth(monthForPage)
                    .collectAsState(initial = emptyList())

                CalendarView(
                    currentMonth = monthForPage,
                    onDateSelected = { selectedDate = it },
                    selectedDate = selectedDate,
                    intakes = intakesForPage,
                    refills = refillsForPage,
                    estimatedRefillDates = estimatedRefillDates
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show selected date's intakes
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            medicationIntakes.forEachIndexed { _, intake ->
                                MedicationEventItem(intake = intake, onClick = { selectedIntake = intake })
                            }
                        }
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
    intakes: List<MedicationIntake>,
    refills: List<PrescriptionRefill>,
    estimatedRefillDates: List<LocalDate>
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
                    val dayIntakes = intakes.filter { it.intakeDateTime.toLocalDate() == date }
                    val dayRefills = refills.filter { it.pickupDate == date }
                    val hasRefill = dayRefills.isNotEmpty()
                    val hasEstimatedRefill = estimatedRefillDates.contains(date)

                    Column(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDateSelected(date) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.toString(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
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
    initialDate: LocalDate
) {
    var selectedDate by remember { mutableStateOf(initialDate) }

    // Calculate initial page: 600 represents current month (allowing 50 years in both directions)
    val initialPage = 600
    val baseYearMonth = YearMonth.from(initialDate)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 1200 } // 100 years worth of months
    )
    val coroutineScope = rememberCoroutineScope()

    // Calculate current month based on pager position
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
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        },
        text = {
            // Swipeable calendar with HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val monthForPage = baseYearMonth.plusMonths((page - initialPage).toLong())

                CalendarView(
                    currentMonth = monthForPage,
                    onDateSelected = {
                        selectedDate = it
                        onDateSelected(it)
                    },
                    selectedDate = selectedDate,
                    intakes = emptyList(),
                    refills = emptyList(),
                    estimatedRefillDates = emptyList()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MedicationEventItem(
    intake: MedicationIntake, onClick: () -> Unit
) {
    val containerColor = if (intake.taken) {
        Color(0xFFE8F5E9) // Light green
    } else {
        Color(0xFFFFEBEE) // Light red
    }
    val contentColor = if (intake.taken) {
        Color(0xFF2E7D32) // Dark green text
    } else {
        Color(0xFFC62828) // Dark red text
    }
    val statusIcon = if (intake.taken) "✓" else "✗"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusIcon,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = intake.intakeDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.width(60.dp),
                color = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = intake.medicationName,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onStatusChange(true) },
                    enabled = !intake.taken,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✓ Taken")
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { onStatusChange(false) },
                    enabled = intake.taken,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✗ Not Taken")
                }
            }
        }
    }, confirmButton = {
        TextButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp)
        ) {
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

                androidx.compose.material3.OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pickup Date: ${pickupDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRecordRefill(pickupDate) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Record Pickup")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
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
            initialDate = pickupDate
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
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Prescription")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
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