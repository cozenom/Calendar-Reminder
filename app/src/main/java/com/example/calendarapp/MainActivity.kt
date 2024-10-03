package com.example.calendarapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicationReminderApp()
        }
    }
}

@Composable
fun MedicationReminderApp() {
    val viewModel: MedicationReminderViewModel = viewModel()
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Medication Reminders", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        ReminderList(reminders = reminders, onDeleteReminder = { viewModel.delete(it) })
        Spacer(modifier = Modifier.height(16.dp))
        AddReminderForm(onAddReminder = { viewModel.insert(it) })
    }
}

@Composable
fun ReminderList(reminders: List<MedicationReminder>, onDeleteReminder: (MedicationReminder) -> Unit) {
    if (reminders.isEmpty()) {
        Text("No reminders yet.")
    } else {
        Column {
            reminders.forEach { reminder ->
                Text("${reminder.medicationName} - ${if (reminder.isMorning) "Morning" else "Evening"}")
            }
        }
    }
}

@Composable
fun AddReminderForm(onAddReminder: (MedicationReminder) -> Unit) {
    var medicationName by remember { mutableStateOf("") }
    var isMorning by remember { mutableStateOf(true) }

    Column {
        OutlinedTextField(
            value = medicationName,
            onValueChange = { medicationName = it },
            label = { Text("Medication Name") }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isMorning,
                onCheckedChange = { isMorning = it }
            )
            Text("Morning")
        }
        Button(onClick = {
            if (medicationName.isNotBlank()) {
                onAddReminder(MedicationReminder(
                    medicationName = medicationName,
                    reminderTime = System.currentTimeMillis(), // This is a placeholder
                    isMorning = isMorning
                ))
                medicationName = ""
            }
        }) {
            Text("Add Reminder")
        }
    }
}