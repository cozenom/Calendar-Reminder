package com.davidp.simpleweeklyreminders.ui.calendar

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.settings.WeekStart
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.appTypography
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import java.time.LocalDate
import java.time.YearMonth

private val STRIP_HEIGHT = 4.dp
private val STRIP_RADIUS = 2.dp
private val NOTCH_SIZE = 5.dp

@Composable
fun CalendarView(
    currentMonth: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    selectedDate: LocalDate?,
    statuses: Map<LocalDate, DayStatus>
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDow = currentMonth.atDay(1).dayOfWeek.value // Mon=1 .. Sun=7
    val weekStart = LocalAppSettings.current.weekStart
    // Blank cells before day 1, counted from the chosen first column.
    val leadingBlanks = leadingBlankCount(firstDow, weekStart)
    val totalCells = daysInMonth + leadingBlanks
    val weekdayLabels = when (weekStart) {
        WeekStart.MONDAY -> listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        WeekStart.SUNDAY -> listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    }
    val today = LocalDate.now()

    // Plain grid, not LazyVerticalGrid: ≤42 cells, all on screen, so laziness saves
    // nothing. Its item provider also defers state reads, leaving cells stale after
    // a completion toggle.
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label.uppercase(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.appTypography.sectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
            }
        }
        // Leading blanks pad the first week out to the month's starting weekday;
        // trailing blanks keep the last week's columns aligned with the rest.
        val weeks = (totalCells + 6) / 7
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val index = week * 7 + dayOfWeek
                    val day = index - leadingBlanks + 1
                    if (index < leadingBlanks || day > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                        continue
                    }
                    val date = currentMonth.atDay(day)
                    DayCell(
                        date = date,
                        status = statuses[date],
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isPast = date < today,
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus?,
    isSelected: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.reminderColors
    val dayColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isPast -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(MaterialTheme.appShapes.small)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = MaterialTheme.appShapes.small
            )
            .then(
                // Today keeps a ring so it stays findable once the selection moves off it
                if (isToday && !isSelected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.appShapes.small
                ) else Modifier
            )
            .semantics(mergeDescendants = true) { }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = dayColor
            )
            Spacer(modifier = Modifier.height(5.dp))
            DayReminderStrip(status = status)
        }

        // Corner wedge: a day that contains a miss is flagged without spending a colour on it,
        // so the state survives greyscale and colour-blind viewing.
        if (status?.hasMissed == true) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(NOTCH_SIZE)
            ) {
                val wedge = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(wedge, colors.missed)
            }
        }
    }
}

/**
 * One segment per scheduled occurrence, in time order, laid over a faint track.
 *
 * Done fills with the accent, missed is a hollow outlined segment, and pending is simply left
 * empty so the track shows through — fill, not hue, carries the meaning.
 */
@Composable
fun DayReminderStrip(status: DayStatus?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.reminderColors
    // Nothing scheduled: reserve the height but draw no bar, so blank days stay blank and
    // every row of cells still lines up.
    if (status == null || status.segments.isEmpty()) {
        Spacer(modifier = modifier.height(STRIP_HEIGHT))
        return
    }

    val label = buildString {
        append("${status.doneCount} of ${status.total} done")
        if (status.hasMissed) append(", some missed")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(STRIP_HEIGHT)
            .clip(RoundedCornerShape(STRIP_RADIUS))
            .background(colors.track)
            .clearAndSetSemantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        status.segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        when (segment) {
                            OccurrenceStatus.DONE -> Modifier.background(colors.done)
                            OccurrenceStatus.MISSED -> Modifier.border(1.dp, colors.missed)
                            OccurrenceStatus.PENDING -> Modifier // track shows through
                        }
                    )
            )
        }
    }
}
