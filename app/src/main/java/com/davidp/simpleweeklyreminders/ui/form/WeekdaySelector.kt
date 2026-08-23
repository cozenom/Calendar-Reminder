package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

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
