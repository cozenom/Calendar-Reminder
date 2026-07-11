package com.davidp.simpleweeklyreminders

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
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.davidp.simpleweeklyreminders.data.model.DEFAULT_ICON_KEY
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.notification.BootReceiver
import com.davidp.simpleweeklyreminders.data.notification.ReminderWorker
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ReminderViewModel
    private lateinit var alarmManager: AlarmManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

        requestRequiredPermissions()
        ReminderWorker.schedule(this)
        // User is looking at the app — reset the baseline for "missed reminders" reports
        BootReceiver.markSeenNow(this)

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
    var selectedTab by remember { mutableIntStateOf(1) }
    val tabs = listOf("Reminders", "Calendar")
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.statusBarsPadding()) {
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

    BackHandler(enabled = selectedTab == 1) {
        selectedTab = 0
    }
    BackHandler(enabled = selectedTab == 0) {
        // Do nothing — prevent closing the app on the reminders list
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
    val today = LocalDate.now()
    val todayLogs by remember(today) { viewModel.getLogsForDate(today) }.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your Reminders", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
        ReminderList(
            reminders = reminders,
            todayLogsByReminder = todayLogs.groupBy { it.reminderId },
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
    todayLogsByReminder: Map<Int, List<ReminderLog>>,
    onDeleteReminder: (Reminder) -> Unit,
    viewModel: ReminderViewModel
) {
    var list by remember { mutableStateOf(reminders) }
    var isDraggingActive by remember { mutableStateOf(false) }

    LaunchedEffect(reminders) {
        if (!isDraggingActive) list = reminders
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        list = list.toMutableList().apply { add(to.index, removeAt(from.index)) }
        viewModel.updateRemindersOrder(list)
    }

    LazyColumn(state = lazyListState) {
        items(list, key = { it.id }) { reminder ->
            ReorderableItem(reorderState, key = reminder.id) { _ ->
                ReminderItem(
                    reminder = reminder,
                    todayLogs = todayLogsByReminder[reminder.id] ?: emptyList(),
                    onDelete = { onDeleteReminder(reminder) },
                    viewModel = viewModel,
                    dragHandleModifier = Modifier.draggableHandle(
                        onDragStarted = { isDraggingActive = true },
                        onDragStopped = { isDraggingActive = false }
                    )
                )
            }
        }
    }
}

@Composable
fun AddReminderForm(onAddReminder: (reminder: Reminder) -> Unit) {
    var title by remember { mutableStateOf("Reminder") }
    var frequency by remember { mutableIntStateOf(1) }
    var reminderTimes by remember { mutableStateOf(listOf(LocalTime.now().plusMinutes(2).truncatedTo(ChronoUnit.MINUTES))) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var useEveryNDays by remember { mutableStateOf(false) }
    var dayInterval by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(DEFAULT_ICON_KEY) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        FrequencySelector(frequency = frequency, onFrequencyChange = {
            frequency = it
            reminderTimes = List(it) { index ->
                if (index < reminderTimes.size) reminderTimes[index]
                else LocalTime.now().plusMinutes(2).truncatedTo(ChronoUnit.MINUTES)
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
            shape = MaterialTheme.appShapes.medium
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

        RecurrenceToggle(useEveryNDays = useEveryNDays, onChanged = { useEveryNDays = it })
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))
        if (useEveryNDays) {
            DayIntervalSelector(interval = dayInterval, onIntervalChange = { dayInterval = it })
        } else {
            WeekdaySelector(selectedDays = reminderDays, onDaysChanged = { reminderDays = it })
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

        var showIconPicker by remember { mutableStateOf(false) }
        Text("Icon", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconPickerRow(selectedKey = selectedIcon, onChangeTapped = { showIconPicker = true })
        Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

        if (showIconPicker) {
            IconPickerDialog(
                currentKey = selectedIcon,
                onIconSelected = { selectedIcon = it },
                onDismiss = { showIconPicker = false }
            )
        }

        Button(
            onClick = {
                val reminder = Reminder(
                    title = title.ifBlank { "Reminder" },
                    reminderTimes = reminderTimes,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    reminderDays = reminderDays,
                    notes = notes.ifBlank { null },
                    icon = selectedIcon,
                    dayInterval = if (useEveryNDays) dayInterval else null
                )
                onAddReminder(reminder)
                title = "Reminder"
                frequency = 1
                reminderTimes = listOf(LocalTime.now().plusMinutes(2).truncatedTo(ChronoUnit.MINUTES))
                startDate = LocalDate.now()
                endDate = null
                reminderDays = setOf(1, 2, 3, 4, 5, 6, 7)
                useEveryNDays = false
                dayInterval = 1
                notes = ""
                selectedIcon = DEFAULT_ICON_KEY
            },
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.appShapes.medium
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
fun RecurrenceToggle(useEveryNDays: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!useEveryNDays) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium
            ) { Text("Specific Days") }
        } else {
            androidx.compose.material3.OutlinedButton(
                onClick = { onChanged(false) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium
            ) { Text("Specific Days") }
        }
        if (useEveryNDays) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium
            ) { Text("Every N Days") }
        } else {
            androidx.compose.material3.OutlinedButton(
                onClick = { onChanged(true) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.appShapes.medium
            ) { Text("Every N Days") }
        }
    }
}

@Composable
fun DayIntervalSelector(interval: Int, onIntervalChange: (Int) -> Unit) {
    var inputText by remember { mutableStateOf(interval.toString()) }
    LaunchedEffect(interval) { inputText = interval.toString() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Repeat every",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { if (interval > 1) onIntervalChange(interval - 1) },
                modifier = Modifier.width(MaterialTheme.dimensions.frequencyButtonWidth),
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = "-", fontSize = 20.sp) }
            OutlinedTextField(
                value = inputText,
                onValueChange = { text ->
                    inputText = text
                    val parsed = text.toIntOrNull()
                    if (parsed != null && parsed >= 1) onIntervalChange(parsed)
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(72.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )
            Button(
                onClick = { onIntervalChange(interval + 1) },
                modifier = Modifier.width(MaterialTheme.dimensions.frequencyButtonWidth),
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = "+", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("days")
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekdays.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index + 1)
            WeekdayButton(
                day = day,
                isSelected = isSelected,
                onClick = {
                    val newSet = if (isSelected) selectedDays - (index + 1) else selectedDays + (index + 1)
                    // Keep at least one day selected — zero days would silently never fire
                    if (newSet.isNotEmpty()) onDaysChanged(newSet)
                },
                modifier = Modifier.weight(1f).aspectRatio(1f)
            )
        }
    }
}

@Composable
fun WeekdayButton(day: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
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
        shape = MaterialTheme.appShapes.medium
    ) {
        Text("Time: ${selectedTime.format(DateTimeFormatter.ofPattern(timeFormat))}")
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
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
            shape = MaterialTheme.appShapes.medium,
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
            shape = MaterialTheme.appShapes.medium,
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
            shape = MaterialTheme.appShapes.medium,
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
            shape = MaterialTheme.appShapes.medium,
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
                                ReminderEventItem(log = log, iconKey = activeReminders.find { it.id == log.reminderId }?.icon, onClick = { selectedLog = log })
                            }
                        }
                    }
                }
            }
        }
    }

    selectedLog?.let { log ->
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
                            .clip(MaterialTheme.appShapes.small)
                            .clickable { onDateSelected(date) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = MaterialTheme.appShapes.small
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
                        FlexibleDotRow(logs = dayLogs, maxDots = 4)
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
    // One dot per unique reminder: green if all occurrences done, red if any pending
    val reminderDotColors = logs.groupBy { it.reminderId }.values.map { group ->
        if (group.all { it.completed }) reminderColors.completedIndicator else reminderColors.pendingIndicator
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        reminderDotColors.take(maxDots).forEach { dotColor ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 1.dp)
                    .background(dotColor, CircleShape)
            )
        }
        if (reminderDotColors.size > maxDots) {
            Text("+${reminderDotColors.size - maxDots}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
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
            TextButton(onClick = onDismissRequest, shape = MaterialTheme.appShapes.medium) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReminderEventItem(log: ReminderLog, iconKey: String?, onClick: () -> Unit) {
    val reminderColors = MaterialTheme.reminderColors
    val containerColor = if (log.completed) reminderColors.completedContainer else reminderColors.pendingContainer
    val contentColor = if (log.completed) reminderColors.completedContent else reminderColors.pendingContent
    val statusIcon = if (log.completed) "✓" else "✗"

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.appShapes.medium,
        color = containerColor,
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = iconFromKey(iconKey).icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp).padding(end = 0.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = log.logDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.width(60.dp),
                color = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = log.title, style = MaterialTheme.typography.bodyLarge, color = contentColor, modifier = Modifier.weight(1f))
            Text(text = statusIcon, style = MaterialTheme.typography.titleMedium, color = contentColor)
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
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text(if (log.completed) "✗ Mark as Pending" else "✓ Mark as Done")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.appShapes.medium) { Text("Close") }
        }
    )
}

@Composable
fun IconPickerRow(selectedKey: String?, onChangeTapped: () -> Unit) {
    val option = iconFromKey(selectedKey)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )
        TextButton(onClick = onChangeTapped) { Text("Change") }
    }
}
