package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    // Manual only — a computed sort would re-sort the list back on the next recomposition
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
