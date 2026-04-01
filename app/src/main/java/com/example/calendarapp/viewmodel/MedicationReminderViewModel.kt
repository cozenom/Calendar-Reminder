package com.example.calendarapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.Reminder
import com.example.calendarapp.data.model.ReminderLog
import com.example.calendarapp.data.notification.ReminderWorker
import com.example.calendarapp.data.repository.ReminderLogRepository
import com.example.calendarapp.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReminderRepository
    private val logRepository: ReminderLogRepository
    val allReminders: Flow<List<Reminder>>

    init {
        val database = AppDatabase.getDatabase(application)
        val reminderDao = database.reminderDao()
        val reminderLogDao = database.reminderLogDao()
        repository = ReminderRepository(reminderDao, reminderLogDao)
        allReminders = repository.allReminders
        logRepository = ReminderLogRepository(reminderLogDao)
    }

    fun insert(reminder: Reminder) = viewModelScope.launch {
        repository.insert(reminder)
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun update(reminder: Reminder) = viewModelScope.launch {
        repository.update(reminder)
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun delete(reminder: Reminder) = viewModelScope.launch {
        repository.delete(reminder)
        ReminderWorker.rescheduleNotifications(getApplication())
    }

    fun getActiveReminders(date: LocalDate): Flow<List<Reminder>> {
        return repository.getActiveReminders(date)
    }

    fun updateLogCompletedStatus(logId: Int, completed: Boolean) = viewModelScope.launch {
        repository.updateLogCompletedStatus(logId, completed)
    }

    fun getLogsForMonth(yearMonth: YearMonth): Flow<List<ReminderLog>> {
        return logRepository.getLogsForDateRange(
            yearMonth.atDay(1).atStartOfDay(),
            yearMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1)
        )
    }

    fun getLogsForDate(date: LocalDate): Flow<List<ReminderLog>> {
        return logRepository.getLogsForDateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusNanos(1)
        )
    }
}
