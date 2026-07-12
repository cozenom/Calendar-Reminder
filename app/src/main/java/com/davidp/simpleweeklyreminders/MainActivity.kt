package com.davidp.simpleweeklyreminders

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
    // Tab 0 = Calendar (home, leftmost, start tab), tab 1 = Reminders
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = {
                    Icon(
                        if (selectedTab == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                        contentDescription = null
                    )
                },
                label = { Text("Calendar") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = {
                    Icon(
                        if (selectedTab == 1) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null
                    )
                },
                label = { Text("Reminders") }
            )
        }
    }, floatingActionButton = {
        if (selectedTab == 1) {
            FloatingActionButton(onClick = { showAddReminderDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    }) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.padding(paddingValues),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { direction * it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -direction * it / 4 } + fadeOut())
            },
            label = "tabSwitch"
        ) { tab ->
            when (tab) {
                0 -> CalendarTab(viewModel)
                else -> RemindersTab(viewModel)
            }
        }
    }

    // Back on Reminders returns to the Calendar (home) tab; back on Calendar
    // is unhandled so the system exits the app (keeps predictive back working)
    BackHandler(enabled = selectedTab == 1) {
        selectedTab = 0
    }

    if (showAddReminderDialog) {
        ReminderFormSheet(
            initial = null,
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminder ->
                viewModel.insert(reminder)
                showAddReminderDialog = false
            }
        )
    }
}

@Composable
fun RemindersTab(viewModel: ReminderViewModel) {
    // null = first DB emission hasn't arrived yet — show nothing rather than
    // flashing the empty state on every switch to this tab
    val reminders by viewModel.allReminders.collectAsState(initial = null)
    val today = LocalDate.now()
    val todayLogs by remember(today) { viewModel.getLogsForDate(today) }.collectAsState(initial = emptyList())

    val loadedReminders = reminders ?: return
    if (loadedReminders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                Text(
                    "No reminders yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap + to create your first reminder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ReminderList(
                reminders = loadedReminders,
                todayLogsByReminder = todayLogs.groupBy { it.reminderId },
                onDeleteReminder = { viewModel.delete(it) },
                viewModel = viewModel
            )
        }
    }
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
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(reminders) {
        if (!isDraggingActive) list = reminders
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        list = list.toMutableList().apply { add(to.index, removeAt(from.index)) }
        viewModel.updateRemindersOrder(list)
    }

    LazyColumn(
        state = lazyListState,
        // Fill the height so a dragged card isn't clipped at the bottom of a
        // short list (the list would otherwise end right below its last item)
        modifier = Modifier.fillMaxSize(),
        // Keep the FAB from covering the last card's buttons
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(list, key = { it.id }) { reminder ->
            ReorderableItem(
                reorderState,
                key = reminder.id,
                // Slide items into place on add/delete/reorder
                modifier = Modifier.animateItem()
            ) { _ ->
                ReminderItem(
                    reminder = reminder,
                    todayLogs = todayLogsByReminder[reminder.id] ?: emptyList(),
                    onDelete = { onDeleteReminder(reminder) },
                    viewModel = viewModel,
                    dragHandleModifier = Modifier.draggableHandle(
                        onDragStarted = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isDraggingActive = true
                        },
                        onDragStopped = { isDraggingActive = false }
                    )
                )
            }
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

    // remember the flows so recomposition doesn't recreate them and reset
    // collectAsState back to emptyList (visible as flicker)
    val activeReminders by remember(selectedDate) { viewModel.getActiveReminders(selectedDate) }.collectAsState(initial = emptyList())
    val selectedDateLogs by remember(selectedDate) { viewModel.getLogsForDate(selectedDate) }.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Calendar card: month navigation + swipeable grid in one container
        Surface(
            shape = MaterialTheme.appShapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
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

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val monthForPage = baseYearMonth.plusMonths((page - initialPage).toLong())
                    val logsForPage by remember(monthForPage) { viewModel.getLogsForMonth(monthForPage) }.collectAsState(initial = emptyList())

                    CalendarView(
                        currentMonth = monthForPage,
                        onDateSelected = { selectedDate = it },
                        selectedDate = selectedDate,
                        logs = logsForPage
                    )
                }
            }
        }

        // Legend: small bare row, no container box
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
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
                    val isToday = date == LocalDate.now()
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
                            .then(
                                if (isToday) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.appShapes.small
                                ) else Modifier
                            )
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.toString(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isToday) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
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
        modifier = Modifier.height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        reminderDotColors.take(maxDots).forEach { dotColor ->
            Box(modifier = Modifier.size(5.dp).background(dotColor, CircleShape))
        }
        if (reminderDotColors.size > maxDots) {
            // Wider pill hints there are more reminders than dots shown
            Box(
                modifier = Modifier
                    .size(width = 9.dp, height = 5.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
            )
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
