package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.settings.WeekStart
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings

/** ISO day numbers, Mon=1 .. Sun=7, paired with the single letter shown in the circle. */
private val WEEKDAYS = listOf(
    1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S"
)
private val FULL_NAMES = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

@Composable
fun WeekdaySelector(selectedDays: Set<Int>, onDaysChanged: (Set<Int>) -> Unit) {
    // Follows the same first-column choice as the calendar grid
    val ordered = when (LocalAppSettings.current.weekStart) {
        WeekStart.MONDAY -> WEEKDAYS
        WeekStart.SUNDAY -> listOf(WEEKDAYS.last()) + WEEKDAYS.dropLast(1)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ordered.forEach { (isoDay, letter) ->
            val isSelected = selectedDays.contains(isoDay)
            WeekdayButton(
                letter = letter,
                fullName = FULL_NAMES[isoDay - 1],
                isSelected = isSelected,
                onClick = {
                    val newSet = if (isSelected) selectedDays - isoDay else selectedDays + isoDay
                    // Keep at least one day selected — zero days would silently never fire
                    if (newSet.isNotEmpty()) onDaysChanged(newSet)
                },
                modifier = Modifier.weight(1f).aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun WeekdayButton(
    letter: String,
    fullName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .then(
                if (isSelected) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            .toggleable(value = isSelected, onValueChange = { onClick() }, role = Role.Checkbox)
            // The letters repeat (T/T, S/S), so screen readers get the full name instead
            .semantics { contentDescription = fullName },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
