package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.davidp.simpleweeklyreminders.data.model.ReminderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceToggle(mode: ReminderType, onChanged: (ReminderType) -> Unit) {
    val options = listOf(
        ReminderType.SPECIFIC_DAYS to "Specific Weekdays",
        ReminderType.EVERY_N_DAYS to "Every N Days",
        ReminderType.ONE_TIME to "One-Time"
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.first { it.first == mode }.second

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeat") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChanged(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
