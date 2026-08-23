package com.davidp.simpleweeklyreminders.ui.calendar

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.coversDate
import com.davidp.simpleweeklyreminders.data.model.statusOf
import com.davidp.simpleweeklyreminders.data.settings.fullDatePattern
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

    // Pips and the day list slice one emission, so they can't disagree about a day
    LaunchedEffect(currentMonth, selectedDate) { viewModel.setCalendarWindow(currentMonth, selectedDate) }
    val calendarLogs by viewModel.calendarLogs.collectAsState()
    val allReminders by viewModel.reminders.collectAsState()

    // The done/missed/pending split needs a "now". Truncated to the minute so it stays a
    // stable remember key between recompositions instead of invalidating on every frame.
    val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

    // Logs don't carry their reminder's colour, so the grid looks it up by id. Built from the
    // reminders flow the tab already collects — no extra query.
    val colorKeys = remember(allReminders) {
        allReminders.orEmpty().associate { it.id to it.color }
    }

    val selectedDateLogs = remember(calendarLogs, selectedDate) {
        calendarLogs.filter { it.logDateTime.toLocalDate() == selectedDate }
    }
    val selectedDayStatus = remember(selectedDateLogs, now) {
        dayStatuses(selectedDateLogs, now)[selectedDate]
    }
    // Includes archived reminders on purpose — a past day's logs still need their icon
    // and importance, and the reminder behind them may have lapsed since.
    val remindersOnSelectedDate = remember(allReminders, selectedDate) {
        allReminders.orEmpty().filter { it.coversDate(selectedDate) }
    }

    Column(modifier = Modifier.fillMaxHeight()) {
        MonthHeader(
            month = currentMonth,
            onPrevious = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNext = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            onToday = {
                selectedDate = LocalDate.now()
                coroutineScope.launch { pagerState.animateScrollToPage(initialPage) }
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) { page ->
            val monthForPage = baseYearMonth.plusMonths((page - initialPage).toLong())
            // Folded once per page rather than filtered inside each of the 42 cells. Slices
            // the shared window instead of querying per page — the window already spans the
            // neighbouring months a swipe can reach.
            val statuses = remember(calendarLogs, monthForPage, now, colorKeys) {
                dayStatuses(
                    calendarLogs.filter { YearMonth.from(it.logDateTime) == monthForPage },
                    now,
                    colorKeys
                )
            }

            CalendarView(
                currentMonth = monthForPage,
                onDateSelected = { selectedDate = it },
                selectedDate = selectedDate,
                statuses = statuses
            )
        }

        Legend()

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val fullDatePattern = LocalAppSettings.current.dateFormat.fullDatePattern(LocalContext.current)
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern(fullDatePattern)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            selectedDayStatus?.let { status ->
                Text(
                    text = "${status.doneCount}/${status.total} done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedDateLogs.isEmpty()) {
            Text(
                "No reminders scheduled for this day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.padding(horizontal = 14.dp)) {
                items(selectedDateLogs, key = { it.id }) { log ->
                    val reminder = remindersOnSelectedDate.find { it.id == log.reminderId }
                    ReminderEventItem(
                        log = log,
                        status = statusOf(log, now),
                        iconKey = reminder?.icon,
                        colorKey = reminder?.color,
                        importance = reminder?.importance ?: Importance.HIGH,
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

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToday)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Reads the three segment treatments used in the grid above, drawn the same way. */
@Composable
private fun Legend() {
    val colors = MaterialTheme.reminderColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendKey("Done") { Modifier.background(colors.done, RoundedCornerShape(2.dp)) }
        LegendKey("Pending") { Modifier.background(colors.track, RoundedCornerShape(2.dp)) }
        LegendKey("Missed") { Modifier.border(1.dp, colors.missed, RoundedCornerShape(2.dp)) }
        Text(
            "← earlier · later →",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendKey(label: String, swatch: () -> Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 10.dp, height = 4.dp)
                .background(Color.Transparent)
                .then(swatch())
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
