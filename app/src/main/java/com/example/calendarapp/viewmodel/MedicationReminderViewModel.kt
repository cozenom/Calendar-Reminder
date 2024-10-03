package com.example.calendarapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    fun update(reminder: MedicationReminder) = viewModelScope.launch {
        repository.update(reminder)
    }

    fun delete(reminder: MedicationReminder) = viewModelScope.launch {
        repository.delete(reminder)
    }

    fun getActiveReminders(date: LocalDate): Flow<List<MedicationReminder>> {
        return repository.getActiveReminders(date)
    }
}
