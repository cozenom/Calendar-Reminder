package com.example.calendarapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.repository.MedicationIntakeRepository
import com.example.calendarapp.data.repository.MedicationReminderRepository
import com.example.calendarapp.data.notification.MedicationReminderWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class MedicationReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val reminderRepository: MedicationReminderRepository
    private val intakeRepository: MedicationIntakeRepository
    val allReminders: Flow<List<MedicationReminder>>

    init {
        val database = AppDatabase.getDatabase(application)
        val reminderDao = database.medicationReminderDao()
        val intakeDao = database.medicationIntakeDao()
        reminderRepository = MedicationReminderRepository(reminderDao, intakeDao)
        allReminders = reminderRepository.allReminders
        intakeRepository = MedicationIntakeRepository(intakeDao)
    }

    fun createOrUpdateReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            reminderRepository.insertOrUpdateReminder(reminder)
            MedicationReminderWorker.rescheduleNotifications(getApplication())
        }
    }

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            reminderRepository.delete(reminder)
            MedicationReminderWorker.rescheduleNotifications(getApplication())
        }
    }

    fun getIntakesWithRemindersForMonth(yearMonth: YearMonth): Flow<List<Pair<MedicationIntake, MedicationReminder>>> {
        val startDateTime = yearMonth.atDay(1).atStartOfDay()
        val endDateTime = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1)
        return reminderRepository.getIntakesWithRemindersForDateRange(startDateTime, endDateTime)
    }

    fun getIntakesWithRemindersForDate(date: LocalDate): Flow<List<Pair<MedicationIntake, MedicationReminder>>> {
        val startDateTime = date.atStartOfDay()
        val endDateTime = date.plusDays(1).atStartOfDay().minusNanos(1)
        return reminderRepository.getIntakesWithRemindersForDateRange(startDateTime, endDateTime)
    }

    fun updateIntakeTakenStatus(intakeId: Int, taken: Boolean) {
        viewModelScope.launch {
            intakeRepository.updateTakenStatus(intakeId, taken)
        }
    }

    fun insert(reminder: MedicationReminder) = viewModelScope.launch {
        reminderRepository.insert(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun update(reminder: MedicationReminder) = viewModelScope.launch {
        reminderRepository.update(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun delete(reminder: MedicationReminder) = viewModelScope.launch {
        reminderRepository.delete(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun getIntakesForDate(date: LocalDate): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusNanos(1)
        )
    }
}