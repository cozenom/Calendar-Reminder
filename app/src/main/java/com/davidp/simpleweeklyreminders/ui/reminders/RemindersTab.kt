package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.data.model.defaultDirection
import com.davidp.simpleweeklyreminders.data.model.sortedFor
import com.davidp.simpleweeklyreminders.ui.archive.newlyArchivedCount
import com.davidp.simpleweeklyreminders.ui.theme.dimensions
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    var hidePaused by rememberSaveable { mutableStateOf(false) }
    var showSortFilterSheet by remember { mutableStateOf(false) }
    // The sheet is one surface with two entry points — the search icon opens it with the
    // field already focused, the sort icon opens it plainly.
    var searchOnOpen by remember { mutableStateOf(false) }
    val isFilterActive = sortMode != SortMode.MANUAL || selectedImportances.size < 3 ||
        searchQuery.isNotBlank() || hidePaused

    val visibleReminders = remember(
        loadedReminders, sortMode, sortDirection, selectedImportances, searchQuery, hidePaused
    ) {
        loadedReminders
            .filter { selectedImportances.contains(it.importance) }
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
            .filter { !hidePaused || it.isActive }
            .sortedFor(sortMode, sortDirection)
    }

    val doneToday = todayLogs.count { it.completed }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title lives in the top app bar; this row carries the count and the actions.
            Text(
                text = buildString {
                    append("${visibleReminders.size} reminder${if (visibleReminders.size == 1) "" else "s"}")
                    if (todayLogs.isNotEmpty()) append(" · $doneToday of ${todayLogs.size} done today")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { searchOnOpen = true; showSortFilterSheet = true }) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search reminders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { searchOnOpen = false; showSortFilterSheet = true }) {
                    BadgedBox(badge = {
                        if (isFilterActive) Badge()
                    }) {
                        Icon(
                            Icons.Outlined.SwapVert,
                            contentDescription = "Sort & filter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onOpenArchive) {
                    BadgedBox(badge = {
                        if (badgeCount > 0) Badge { Text(badgeCount.toString()) }
                    }) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick way to drop the search without reopening the sheet — tapping the
        // chip itself (rather than the x) reopens the sheet to edit it instead.
        if (searchQuery.isNotBlank()) {
            InputChip(
                selected = false,
                onClick = { searchOnOpen = true; showSortFilterSheet = true },
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
                    .padding(start = 20.dp, top = 4.dp)
                    .widthIn(max = 200.dp)
            )
        }

        if (loadedReminders.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Notifications,
                title = "Nothing to remember yet",
                body = "Add the things you'd rather not keep in your head — watering, vitamins, feeding the cat."
            )
        } else if (visibleReminders.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.SwapVert,
                title = "No reminders match your filters",
                body = "Try widening the importance filter or clearing the search."
            )
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
            hidePaused = hidePaused,
            onHidePausedChange = { hidePaused = it },
            matchCount = visibleReminders.size,
            focusSearchOnOpen = searchOnOpen,
            onDismiss = { showSortFilterSheet = false }
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
