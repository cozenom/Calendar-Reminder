package com.example.calendarapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.repository.MedicationIntakeRepository
import com.example.calendarapp.data.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicationReminderRepository
    private val intakeRepository: MedicationIntakeRepository
    val allReminders: Flow<List<MedicationReminder>>

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.medicationReminderDao()
        val intakeDao = database.medicationIntakeDao()
        repository = MedicationReminderRepository(dao)
        intakeRepository = MedicationIntakeRepository(intakeDao)
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

    fun updateIntakeTakenStatus(intakeId: Int, taken: Boolean) = viewModelScope.launch {
        intakeRepository.updateTakenStatus(intakeId, taken)
    }

    fun getMissedIntakes(dateTime: LocalDateTime): Flow<List<MedicationIntake>> {
        return intakeRepository.getMissedIntakes(dateTime)
    }

    fun getIntakesForDate(date: LocalDate): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusNanos(1)
        )
    }
}