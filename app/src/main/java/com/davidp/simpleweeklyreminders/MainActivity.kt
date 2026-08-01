package com.davidp.simpleweeklyreminders

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.SortDirection
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.data.model.archivedSince
import com.davidp.simpleweeklyreminders.data.model.coversDate
import com.davidp.simpleweeklyreminders.data.model.defaultDirection
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.notification.BootReceiver
import com.davidp.simpleweeklyreminders.data.notification.ReminderWorker
import com.davidp.simpleweeklyreminders.data.settings.ArchiveSettings
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModelFactory
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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

/** Reminders that lapsed into the Archive after the user last viewed it. */
private fun newlyArchivedCount(archived: List<Reminder>, context: android.content.Context): Int {
    val lastViewed = ArchiveSettings.getLastViewed(context)
    return archived.count { it.archivedSince()?.isAfter(lastViewed) == true }
}

@Composable
fun ReminderApp(viewModel: ReminderViewModel) {
    // Tab 0 = Calendar (home, leftmost, start tab), tab 1 = Reminders
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // One-shot per app session: if reminders auto-lapsed into the Archive since the
    // user last viewed it, surface a heads-up notice (the badge on the Archive icon
    // persists the same information until they actually open it).
    var hasShownArchiveNotice by rememberSaveable { mutableStateOf(false) }
    val archivedReminders by viewModel.archivedReminders.collectAsState()
    LaunchedEffect(archivedReminders) {
        if (hasShownArchiveNotice) return@LaunchedEffect
        val archived = archivedReminders ?: return@LaunchedEffect
        val newCount = newlyArchivedCount(archived, context)
        if (newCount > 0) {
            hasShownArchiveNotice = true
            snackbarHostState.showSnackbar(
                message = "$newCount reminder${if (newCount > 1) "s" else ""} archived since you last checked",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; showArchive = false },
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
        if (selectedTab == 1 && !showArchive) {
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
                else -> if (showArchive) {
                    ArchiveScreen(viewModel, onBack = { showArchive = false })
                } else {
                    RemindersTab(viewModel, onOpenArchive = { showArchive = true }, snackbarHostState = snackbarHostState)
                }
            }
        }
    }

    // Back closes the Archive screen first, then returns Reminders to the
    // Calendar (home) tab; back on Calendar is unhandled so the system exits
    // the app (keeps predictive back working)
    BackHandler(enabled = showArchive) {
        showArchive = false
    }
    BackHandler(enabled = selectedTab == 1 && !showArchive) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersTab(viewModel: ReminderViewModel, onOpenArchive: () -> Unit, snackbarHostState: SnackbarHostState) {
    // null = first DB emission hasn't arrived yet — show nothing rather than
    // flashing the empty state on every switch to this tab
    val reminders by viewModel.allReminders.collectAsState()
    val archived by viewModel.archivedReminders.collectAsState()
    val today = LocalDate.now()
    // Re-points the shared today's-logs flow if the date rolls over while the app is open
    LaunchedEffect(today) { viewModel.setToday(today) }
    val todayLogs by viewModel.todayLogs.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadedReminders = reminders ?: return
    val badgeCount = newlyArchivedCount(archived ?: emptyList(), context)

    // Sort mode, direction, search text, and the importance filter reset on a real relaunch
    // (fresh process, so this composable starts over) but survive a background-and-resume
    // recreation via rememberSaveable, so switching apps for a second doesn't silently drop them.
    var sortMode by rememberSaveable { mutableStateOf(SortMode.MANUAL) }
    var sortDirection by rememberSaveable { mutableStateOf(SortMode.MANUAL.defaultDirection()) }
    var selectedImportances by rememberSaveable { mutableStateOf(setOf(Importance.LOW, Importance.MEDIUM, Importance.HIGH)) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSortFilterSheet by remember { mutableStateOf(false) }
    val isFilterActive = sortMode != SortMode.MANUAL || selectedImportances.size < 3 || searchQuery.isNotBlank()

    val visibleReminders = remember(loadedReminders, sortMode, sortDirection, selectedImportances, searchQuery) {
        loadedReminders
            .filter { selectedImportances.contains(it.importance) }
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
            .sortedFor(sortMode, sortDirection)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Reminders",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick way to drop the search without reopening the sheet — tapping the
                // chip itself (rather than the x) reopens the sheet to edit it instead.
                if (searchQuery.isNotBlank()) {
                    InputChip(
                        selected = false,
                        onClick = { showSortFilterSheet = true },
                        label = {
                            Text("“$searchQuery”", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .widthIn(max = 140.dp)
                    )
                }
                IconButton(onClick = { showSortFilterSheet = true }) {
                    BadgedBox(badge = {
                        if (isFilterActive) Badge()
                    }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Sort & filter")
                    }
                }
                IconButton(onClick = onOpenArchive) {
                    BadgedBox(badge = {
                        if (badgeCount > 0) Badge { Text(badgeCount.toString()) }
                    }) {
                        Icon(Icons.Outlined.Archive, contentDescription = "Archive")
                    }
                }
            }
        }

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
        } else if (visibleReminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                    Text(
                        "No reminders match your filters",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            ReminderList(
                reminders = visibleReminders,
                todayLogsByReminder = todayLogs.groupBy { it.reminderId },
                onArchiveReminder = { reminder ->
                    viewModel.archive(reminder)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "\"${reminder.title}\" archived",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restore(reminder)
                        }
                    }
                },
                viewModel = viewModel,
                sortMode = sortMode
            )
        }
    }

    if (showSortFilterSheet) {
        SortFilterSheet(
            sortMode = sortMode,
            onSortModeChange = { mode ->
                sortMode = mode
                // Reset to that mode's natural direction rather than carrying over
                // whatever was toggled for the previously selected mode.
                sortDirection = mode.defaultDirection()
            },
            sortDirection = sortDirection,
            onSortDirectionChange = { sortDirection = it },
            selectedImportances = selectedImportances,
            onImportancesChange = { selectedImportances = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onDismiss = { showSortFilterSheet = false }
        )
    }
}

@Composable
fun ArchiveScreen(viewModel: ReminderViewModel, onBack: () -> Unit) {
    val archived by viewModel.archivedReminders.collectAsState()
    val loadedArchived = archived ?: return
    val context = LocalContext.current
    // Viewing this screen clears the "new since last checked" badge/notice.
    LaunchedEffect(Unit) { ArchiveSettings.markViewedNow(context) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
            }
            Text("Archive", style = MaterialTheme.typography.titleLarge)
        }

        if (loadedArchived.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                    Text(
                        "Nothing archived yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(loadedArchived, key = { it.id }) { reminder ->
                    ArchivedReminderItem(
                        reminder = reminder,
                        onRestore = { viewModel.restore(reminder) },
                        onDelete = { viewModel.delete(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArchivedReminderItem(reminder: Reminder, onRestore: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = MaterialTheme.appShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                reminder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            reminder.endDate?.let {
                Text(
                    "Ended ${it.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (reminder.reminderType != ReminderType.ONE_TIME) {
                    TextButton(onClick = onRestore, shape = MaterialTheme.appShapes.medium) {
                        Text("Restore")
                    }
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Delete Forever", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reminder") },
            text = { Text("Permanently delete \"${reminder.title}\" and its history? This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    shape = MaterialTheme.appShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, shape = MaterialTheme.appShapes.medium) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReminderList(
    reminders: List<Reminder>,
    todayLogsByReminder: Map<Int, List<ReminderLog>>,
    onArchiveReminder: (Reminder) -> Unit,
    viewModel: ReminderViewModel,
    sortMode: SortMode = SortMode.MANUAL
) {
    var list by remember { mutableStateOf(reminders) }
    var isDraggingActive by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // Drag-reorder only means something in Manual mode — under a computed sort the
    // list would just re-sort itself back on the next recomposition.
    val dragEnabled = sortMode == SortMode.MANUAL

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
                    onArchive = { onArchiveReminder(reminder) },
                    viewModel = viewModel,
                    dragEnabled = dragEnabled,
                    modifier = if (dragEnabled) Modifier.draggableHandle(
                        onDragStarted = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isDraggingActive = true
                        },
                        onDragStopped = { isDraggingActive = false }
                    ) else Modifier
                )
            }
        }
    }
}

/** Sort-order + importance-filter + title-search bottom sheet for the Reminders tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilterSheet(
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
    sortDirection: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    selectedImportances: Set<Importance>,
    onImportancesChange: (Set<Importance>) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = listOf(
        SortMode.MANUAL to "Manual (drag)",
        SortMode.NEXT_OCCURRENCE to "Next occurrence",
        SortMode.IMPORTANCE to "Importance",
        SortMode.DATE_ADDED to "Date added"
    )
    val importanceOptions = listOf(
        Importance.HIGH to "High",
        Importance.MEDIUM to "Medium",
        Importance.LOW to "Low"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search title") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Text("Sort by", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                sortOptions.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = sortMode == mode,
                                onClick = { onSortModeChange(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortMode == mode, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Direction has no meaning for drag order — disabled (not hidden) for Manual so
            // the sheet's height stays constant across every sort mode.
            val directionEnabled = sortMode != SortMode.MANUAL
            val (ascendingLabel, descendingLabel) = when (sortMode) {
                SortMode.IMPORTANCE -> "Low first" to "High first"
                SortMode.NEXT_OCCURRENCE -> "Soonest first" to "Latest first"
                SortMode.DATE_ADDED -> "Oldest first" to "Newest first"
                SortMode.MANUAL -> "Ascending" to "Descending"
            }
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = directionEnabled && sortDirection == SortDirection.ASCENDING,
                    onClick = { onSortDirectionChange(SortDirection.ASCENDING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    enabled = directionEnabled,
                    icon = {}
                ) {
                    Text(ascendingLabel)
                }
                SegmentedButton(
                    selected = directionEnabled && sortDirection == SortDirection.DESCENDING,
                    onClick = { onSortDirectionChange(SortDirection.DESCENDING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = directionEnabled,
                    icon = {}
                ) {
                    Text(descendingLabel)
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Text("Filter by importance", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                importanceOptions.forEach { (importance, label) ->
                    val selected = selectedImportances.contains(importance)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val updated = if (selected) selectedImportances - importance else selectedImportances + importance
                            // Never allow an empty selection — that would just show "no matches"
                            // for no clear reason, so treat it the same as "all selected".
                            onImportancesChange(
                                if (updated.isEmpty()) setOf(Importance.LOW, Importance.MEDIUM, Importance.HIGH) else updated
                            )
                        },
                        label = { Text(label) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceToggle(mode: ReminderType, onChanged: (ReminderType) -> Unit) {
    val options = listOf(
        ReminderType.SPECIFIC_DAYS to "Specific Weekdays",
        ReminderType.EVERY_N_DAYS to "Every N Days",
        ReminderType.ONE_TIME to "One-Time"
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.first { it.first == mode }.second

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChanged(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Tap-to-select segmented control — same interaction pattern as WeekdaySelector's
 * day buttons (filled when selected, outlined otherwise) rather than a radio
 * group, styled with each level's own color instead of the theme primary so the
 * selector previews all three colors even before a choice is made. No level is
 * pre-selected — [importance] is null until the user picks one, since HIGH is
 * only ever a migration-compatibility default (see Reminder.kt), not something
 * a brand-new reminder should silently inherit.
 */
@Composable
fun ImportanceSelector(importance: Importance?, onChanged: (Importance) -> Unit) {
    val reminderColors = MaterialTheme.reminderColors
    val options = listOf(
        Triple(Importance.LOW, "Low", reminderColors.importanceLow),
        Triple(Importance.MEDIUM, "Medium", reminderColors.importanceMedium),
        Triple(Importance.HIGH, "High", reminderColors.importanceHigh)
    )

    Column {
        Text(
            "Importance",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (level, label, color) ->
                ImportanceButton(
                    label = label,
                    color = color,
                    isSelected = importance == level,
                    onClick = { onChanged(level) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ImportanceButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) color else Color.Transparent
    // Picked from the fill's own luminance rather than a fixed onSurface/surface
    // token: the dark-theme tints (e.g. amber 300) are just as light as their
    // light-theme counterparts are dark, so a theme-fixed choice would read fine
    // in one theme and wash out in the other for the same color
    val contentColor = if (isSelected) {
        if (color.luminance() > 0.5f) Color.Black else Color.White
    } else color

    Box(
        modifier = modifier
            .clip(MaterialTheme.appShapes.medium)
            .background(backgroundColor)
            .border(width = 1.dp, color = color, shape = MaterialTheme.appShapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
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
    val haptics = LocalHapticFeedback.current

    val initialPage = 600
    val baseYearMonth = YearMonth.now()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1200 })
    val coroutineScope = rememberCoroutineScope()

    val currentMonth = remember(pagerState.currentPage) {
        baseYearMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    // Both the month grid's pips and the day list below read this one logs emission, so
    // they always agree about a day's completed state; separate flows per page and per
    // selected day were two independent observers of the same rows and could disagree.
    LaunchedEffect(currentMonth, selectedDate) { viewModel.setCalendarWindow(currentMonth, selectedDate) }
    val calendarLogs by viewModel.calendarLogs.collectAsState()
    val allReminders by viewModel.reminders.collectAsState()

    val selectedDateLogs = remember(calendarLogs, selectedDate) {
        calendarLogs.filter { it.logDateTime.toLocalDate() == selectedDate }
    }
    // Includes archived reminders on purpose — a past day's logs still need their icon
    // and importance, and the reminder behind them may have lapsed since.
    val remindersOnSelectedDate = remember(allReminders, selectedDate) {
        allReminders.orEmpty().filter { it.coversDate(selectedDate) }
    }

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
                    // Slice of the shared window rather than a query per composed page —
                    // the window spans the neighbouring months a swipe can reach
                    val logsForPage = remember(calendarLogs, monthForPage) {
                        calendarLogs.filter { YearMonth.from(it.logDateTime) == monthForPage }
                    }

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
                        items(selectedDateLogs, key = { it.id }) { log ->
                            ReminderEventItem(
                                log = log,
                                iconKey = remindersOnSelectedDate.find { it.id == log.reminderId }?.icon,
                                importance = remindersOnSelectedDate.find { it.id == log.reminderId }?.importance ?: Importance.HIGH,
                                onToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.updateLogCompletedStatus(log.id, !log.completed)
                                }
                            )
                        }
                    }
                }
            }
        }
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

    // A plain grid, not a LazyVerticalGrid: a month is at most 42 cells and they're
    // all on screen at once, so laziness saves no work. It also cost correctness —
    // a lazy layout reaches its items through an item provider rather than a direct
    // state read, which left the day cells drawing a stale log list after a
    // completion toggle (pips lagged behind the day list's checkboxes until some
    // unrelated action forced the cells to recompose).
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
            }
        }
        // Leading blanks pad the first week out to the month's starting weekday;
        // trailing blanks keep the last week's columns aligned with the rest.
        val weeks = (totalDays + 6) / 7
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val index = week * 7 + dayOfWeek
                    val day = index - firstDayOfMonth + 2
                    if (index < firstDayOfMonth - 1 || day > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                        continue
                    }
                    val date = currentMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()
                    val dayLogs = logs.filter { it.logDateTime.toLocalDate() == date }

                    Column(
                        modifier = Modifier
                            .weight(1f)
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
                        DayReminderStrip(logs = dayLogs)
                    }
                }
            }
        }
    }
}

@Composable
fun DayReminderStrip(logs: List<ReminderLog>) {
    val reminderColors = MaterialTheme.reminderColors
    // One tick per occurrence (logs are already ordered by time), so a reminder
    // with multiple times gets a mark per time instead of one merged segment.
    val segmentColors = logs.map { log ->
        if (log.completed) reminderColors.completedIndicator else reminderColors.pendingIndicator
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        segmentColors.forEach { segmentColor ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(segmentColor, RoundedCornerShape(1.dp))
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
fun ReminderEventItem(log: ReminderLog, iconKey: String?, importance: Importance, onToggle: () -> Unit) {
    val reminderColors = MaterialTheme.reminderColors
    val containerColor by animateColorAsState(
        if (log.completed) reminderColors.completedContainer else reminderColors.pendingContainer,
        label = "eventContainerColor"
    )
    val contentColor by animateColorAsState(
        if (log.completed) reminderColors.completedContent else reminderColors.pendingContent,
        label = "eventContentColor"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.appShapes.medium,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = log.title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                Spacer(modifier = Modifier.width(6.dp))
                ImportanceChevrons(importance = importance)
            }
            val snoozedUntil = log.snoozedUntil
            if (snoozedUntil != null && !log.completed) {
                Icon(
                    imageVector = Icons.Filled.Snooze,
                    contentDescription = "Snoozed",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = snoozedUntil.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Checkbox(
                checked = log.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = contentColor,
                    uncheckedColor = contentColor,
                    checkmarkColor = containerColor
                )
            )
        }
    }
}
