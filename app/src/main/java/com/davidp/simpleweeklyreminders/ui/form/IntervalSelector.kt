package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidp.simpleweeklyreminders.data.model.IntervalUnit
import com.davidp.simpleweeklyreminders.ui.theme.dimensions

/** "Repeat every [ - N + ] [unit ▾]" — the count stepper plus a unit dropdown. */
@Composable
fun IntervalSelector(
    interval: Int,
    unit: IntervalUnit,
    onIntervalChange: (Int) -> Unit,
    onUnitChange: (IntervalUnit) -> Unit
) {
    var inputText by remember { mutableStateOf(interval.toString()) }
    LaunchedEffect(interval) { inputText = interval.toString() }
    var showUnits by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.width(4.dp))
            // Box anchors the menu under the button
            Box {
                TextButton(onClick = { showUnits = true }) {
                    Text(unit.label(interval))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = showUnits, onDismissRequest = { showUnits = false }) {
                    IntervalUnit.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label(interval)) },
                            onClick = { onUnitChange(option); showUnits = false }
                        )
                    }
                }
            }
        }
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
