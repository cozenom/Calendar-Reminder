package com.example.calendarapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MedicationReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicationReminderRepository
    val allReminders: Flow<List<MedicationReminder>>

    init {
        val dao = AppDatabase.getDatabase(application).medicationReminderDao()
        repository = MedicationReminderRepository(dao)
        allReminders = repository.allReminders
    }

    fun insert(reminder: MedicationReminder) = viewModelScope.launch {
        repository.insert(reminder)
    }

    fun delete(reminder: MedicationReminder) = viewModelScope.launch {
        repository.delete(reminder)
    }
}