package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.IntervalUnit
import com.davidp.simpleweeklyreminders.ui.theme.dimensions

/**
 * Reads as a sentence: "Every [– N +] months", with the unit as a segmented row below.
 * Was a unit dropdown: all four units fit on screen, so hiding them cost a tap for nothing.
 */
@Composable
fun IntervalSelector(
    interval: Int,
    unit: IntervalUnit,
    onIntervalChange: (Int) -> Unit,
    onUnitChange: (IntervalUnit) -> Unit
) {
    var inputText by remember { mutableStateOf(interval.toString()) }
    LaunchedEffect(interval) { inputText = interval.toString() }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Every", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onIntervalChange(interval - 1) },
                    enabled = interval > 1
                ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
                // Still typeable: stepping to "every 30 days" one tap at a time would be a chore
                BasicTextField(
                    value = inputText,
                    onValueChange = { text ->
                        inputText = text.filter(Char::isDigit).take(3)
                        inputText.toIntOrNull()?.takeIf { it >= 1 }?.let(onIntervalChange)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.width(40.dp)
                )
                IconButton(onClick = { onIntervalChange(interval + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                }
            }
            Text(unit.label(interval), style = MaterialTheme.typography.titleMedium)
        }
        SegmentedToggle(
            options = IntervalUnit.entries.map { it to it.title },
            selected = unit,
            onChanged = onUnitChange
        )
    }
}

/** "day" / "days" etc., matching the count beside it. */
private fun IntervalUnit.label(count: Int): String {
    val singular = when (this) {
        IntervalUnit.DAYS -> "day"
        IntervalUnit.WEEKS -> "week"
        IntervalUnit.MONTHS -> "month"
        IntervalUnit.YEARS -> "year"
    }
    return if (count == 1) singular else "${singular}s"
}

/** Segment label: "Day", "Week"… */
private val IntervalUnit.title: String
    get() = label(1).replaceFirstChar(Char::uppercase)
