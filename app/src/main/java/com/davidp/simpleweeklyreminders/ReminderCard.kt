package com.davidp.simpleweeklyreminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.Reminder
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.ReminderType
import com.davidp.simpleweeklyreminders.data.model.SortDirection
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.data.model.defaultDirection
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.model.isScheduledOn
import com.davidp.simpleweeklyreminders.data.settings.timePattern
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val WEEKDAY_ABBREVIATIONS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Comfortable width for a "12:00 PM" chip with its check icon at default font size. */
private val TIME_CHIP_MIN_WIDTH = 90.dp
private val TIME_CHIP_SPACING = 8.dp

/** Human-readable recurrence line, e.g. "Mon, Wed, Fri", "Every 2 days", "Weekdays". */
fun scheduleSummary(reminder: Reminder): String {
    val base = when (reminder.reminderType) {
        ReminderType.EVERY_N_DAYS ->
            if (reminder.dayInterval == 1) "Every day" else "Every ${reminder.dayInterval} days"
        ReminderType.ONE_TIME -> "One-time"
        ReminderType.SPECIFIC_DAYS -> when {
            reminder.reminderDays.size == 7 -> "Every day"
            reminder.reminderDays == setOf(1, 2, 3, 4, 5) -> "Weekdays"
            reminder.reminderDays == setOf(6, 7) -> "Weekends"
            else -> reminder.reminderDays.sorted().joinToString(", ") { WEEKDAY_ABBREVIATIONS[it - 1] }
        }
    }

    val today = LocalDate.now()
    val qualifiers = buildList {
        if (reminder.startDate > today) {
            add("starts ${reminder.startDate.format(DateTimeFormatter.ofPattern("MMM d"))}")
        }
        reminder.endDate?.let { add("until ${it.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}") }
    }
    return (listOf(base) + qualifiers).joinToString(" · ")
}

/** The next date-time this reminder is scheduled to fire, or null if none (paused/ended). */
fun nextOccurrence(reminder: Reminder, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
    if (!reminder.isActive || reminder.reminderTimes.isEmpty()) return null

    val sortedTimes = reminder.reminderTimes.sorted()
    var date = maxOf(reminder.startDate, now.toLocalDate())
    val endDate = reminder.endDate ?: date.plusYears(1)

    while (date <= endDate) {
        if (reminder.isScheduledOn(date)) {
            for (time in sortedTimes) {
                val dateTime = LocalDateTime.of(date, time)
                if (dateTime > now) return dateTime
            }
        }
        date = date.plusDays(1)
    }
    return null
}

/**
 * Orders reminders for the given [SortMode] and [direction]. MANUAL ignores direction — the
 * list is already in `sortOrder ASC, createdAt ASC` order from the DB query, and drag order
 * has no "reverse". IMPORTANCE and NEXT_OCCURRENCE use a stable sort so MANUAL order still
 * breaks ties (e.g. same importance, or both paused).
 */
fun List<Reminder>.sortedFor(
    mode: SortMode,
    direction: SortDirection = mode.defaultDirection(),
    now: LocalDateTime = LocalDateTime.now()
): List<Reminder> {
    if (mode == SortMode.MANUAL) return this

    val ascending = when (mode) {
        SortMode.DATE_ADDED -> sortedBy { it.createdAt }
        SortMode.IMPORTANCE -> sortedBy { it.importance.ordinal }
        SortMode.NEXT_OCCURRENCE -> sortedWith(compareBy(nullsLast()) { nextOccurrence(it, now) })
        SortMode.MANUAL -> this
    }
    if (direction == SortDirection.ASCENDING) return ascending

    // Descending: reverse, but keep "no next occurrence" (paused/lapsed) pinned at the end
    // rather than flipping to the front — those reminders aren't meaningfully "latest",
    // they're just not applicable to this sort.
    return if (mode == SortMode.NEXT_OCCURRENCE) {
        val (withOccurrence, without) = ascending.partition { nextOccurrence(it, now) != null }
        withOccurrence.reversed() + without
    } else {
        ascending.reversed()
    }
}

private fun formatNextOccurrence(dateTime: LocalDateTime, now: LocalDateTime, timePattern: String): String {
    val time = dateTime.format(DateTimeFormatter.ofPattern(timePattern))
    val daysAway = ChronoUnit.DAYS.between(now.toLocalDate(), dateTime.toLocalDate())
    val day = when {
        daysAway == 0L -> "Today"
        daysAway == 1L -> "Tomorrow"
        daysAway < 7L -> dateTime.format(DateTimeFormatter.ofPattern("EEEE"))
        else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    return "$day at $time"
}

/**
 * Shared importance marker (list row, calendar day list): rank chevrons, one stripe per
 * level. Custom-drawn rather than a repurposed arrow icon so the stripes interlock like
 * rank insignia. Count is the primary signal, color reinforces it (see [ReminderColors]).
 */
@Composable
fun ImportanceChevrons(
    importance: Importance,
    modifier: Modifier = Modifier,
    chevronWidth: Dp = 16.dp,
    chevronHeight: Dp = 6.dp,
    strokeWidth: Dp = 2.dp
) {
    val color = with(MaterialTheme.reminderColors) {
        when (importance) {
            Importance.LOW -> importanceLow
            Importance.MEDIUM -> importanceMedium
            Importance.HIGH -> importanceHigh
        }
    }
    val count = when (importance) {
        Importance.LOW -> 1
        Importance.MEDIUM -> 2
        Importance.HIGH -> 3
    }
    val label = when (importance) {
        Importance.LOW -> "Low importance"
        Importance.MEDIUM -> "Medium importance"
        Importance.HIGH -> "High importance"
    }

    Column(
        modifier = modifier.semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(-(strokeWidth))
    ) {
        repeat(count) {
            Canvas(modifier = Modifier.size(width = chevronWidth, height = chevronHeight)) {
                val strokePx = strokeWidth.toPx()
                val path = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    todayLogs: List<ReminderLog>,
    onArchive: () -> Unit,
    viewModel: ReminderViewModel,
    dragEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.appShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dragEnabled) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to reorder",
                        modifier = modifier
                            .size(20.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                Icon(
                    imageVector = iconFromKey(reminder.icon).icon,
                    contentDescription = null,
                    tint = if (reminder.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Chevrons lead the title so they align in one column down the list
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ImportanceChevrons(importance = reminder.importance)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (reminder.isActive) scheduleSummary(reminder) else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { viewModel.update(reminder.copy(isActive = it)) }
                )
            }

            val timePattern = LocalAppSettings.current.timeFormat.timePattern(LocalContext.current)

            Spacer(modifier = Modifier.height(12.dp))
            val today = LocalDate.now()
            val scheduledToday = reminder.isActive && reminder.isScheduledOn(today)
            // Grid of equal-width chips. How many fit per row is computed from the
            // available width and the user's font scale instead of assuming a screen
            // size, so large-font settings gracefully drop to fewer, wider chips.
            BoxWithConstraints {
                val minChipWidth = TIME_CHIP_MIN_WIDTH * LocalDensity.current.fontScale
                val chipsPerRow = ((maxWidth + TIME_CHIP_SPACING) / (minChipWidth + TIME_CHIP_SPACING))
                    .toInt()
                    .coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(TIME_CHIP_SPACING)) {
                    reminder.reminderTimes.distinct().sorted().chunked(chipsPerRow).forEach { rowTimes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TIME_CHIP_SPACING)
                        ) {
                            rowTimes.forEach { time ->
                                val log = todayLogs.firstOrNull { it.logDateTime.toLocalTime() == time }
                                TimeChip(
                                    label = time.format(DateTimeFormatter.ofPattern(timePattern)),
                                    completed = log?.completed == true,
                                    snoozed = log?.snoozedUntil != null,
                                    actionable = log != null || scheduledToday,
                                    onClick = {
                                        if (log != null) {
                                            viewModel.updateLogCompletedStatus(log.id, !log.completed)
                                        } else {
                                            // No log for this slot today (time had already passed when
                                            // the reminder was created/edited) — record it on demand
                                            viewModel.logAdHocCompletion(reminder, LocalDateTime.of(today, time))
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Keep a partial last row's chips the same size as full rows
                            repeat(chipsPerRow - rowTimes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (reminder.isActive) {
                val now = LocalDateTime.now()
                nextOccurrence(reminder, now)?.let { next ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Next: ${formatNextOccurrence(next, now, timePattern)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            reminder.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.clickable { showNotes = !showNotes },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showNotes) "Hide notes" else "Show notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showNotes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (showNotes) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showEditSheet = true },
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onArchive,
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Archive")
                }
            }
        }
    }

    if (showEditSheet) {
        ReminderFormSheet(
            initial = reminder,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                viewModel.update(updated)
                showEditSheet = false
            }
        )
    }
}

/**
 * One scheduled time as a chip. Tapping toggles (or records) today's
 * completion; chips are inert only when the reminder isn't scheduled today.
 * A pending snooze shows a snooze glyph until the notification re-fires.
 */
@Composable
private fun TimeChip(
    label: String,
    completed: Boolean,
    snoozed: Boolean,
    actionable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reminderColors = MaterialTheme.reminderColors

    Surface(
        modifier = modifier,
        shape = MaterialTheme.appShapes.small,
        color = when {
            completed -> reminderColors.completedContainer
            actionable -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            completed -> reminderColors.completedContent
            actionable -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (actionable && !completed) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        onClick = onClick,
        enabled = actionable
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon space is always reserved (hidden via alpha when blank) so
            // ticking a reminder off doesn't change the chip's size.
            val showSnooze = snoozed && !completed
            Icon(
                imageVector = if (showSnooze) Icons.Filled.Snooze else Icons.Filled.Check,
                contentDescription = when {
                    completed -> "Completed"
                    showSnooze -> "Snoozed"
                    else -> null
                },
                tint = if (showSnooze) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
                modifier = Modifier
                    .size(14.dp)
                    .alpha(if (completed || showSnooze) 1f else 0f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
