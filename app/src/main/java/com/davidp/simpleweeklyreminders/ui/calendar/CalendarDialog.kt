package com.davidp.simpleweeklyreminders.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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
                    // Date picker, not a status view — no occurrence bars here
                    statuses = emptyMap()
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
