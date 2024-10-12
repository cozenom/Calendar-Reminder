package com.example.calendarapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.repository.MedicationIntakeRepository
import com.example.calendarapp.data.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.calendarapp.data.notification.MedicationReminderWorker
import com.example.calendarapp.data.notification.NotificationActionReceiver
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId


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
            val reminderId = reminderRepository.insertOrUpdateReminder(reminder)
            scheduleAlarmsForReminder(reminderId)
            MedicationReminderWorker.rescheduleNotifications(getApplication())
        }
    }

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            reminderRepository.delete(reminder)
            cancelAlarmsForReminder(reminder.id)
            MedicationReminderWorker.rescheduleNotifications(getApplication())
        }
    }

    private suspend fun scheduleAlarmsForReminder(reminderId: Int) {
        val reminder = reminderRepository.getReminderById(reminderId)
        val intakes = intakeRepository.getIntakesForReminder(reminderId)
        intakes.collect { intakeList ->
            intakeList.forEach { intake ->
                scheduleAlarmForIntake(intake)
            }
        }
    }

    private fun scheduleAlarmForIntake(intake: MedicationIntake) {
        val alarmManager =
            getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), NotificationActionReceiver::class.java).apply {
            action = MedicationReminderWorker.ACTION_SHOW_NOTIFICATION
            putExtra(MedicationReminderWorker.EXTRA_INTAKE_ID, intake.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            intake.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime =
            intake.intakeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun cancelAlarmsForReminder(reminderId: Int) {
        val alarmManager =
            getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), NotificationActionReceiver::class.java)
        intent.action = MedicationReminderWorker.ACTION_SHOW_NOTIFICATION

        // Cancel all potential pending intents for this reminder (up to a reasonable maximum, e.g., 1000)
        for (i in 0 until 1000) {
            val pendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                reminderId * 10000 + i, // Use a unique ID for each potential intake
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
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
    }

    fun update(reminder: MedicationReminder) = viewModelScope.launch {
        reminderRepository.update(reminder)
    }

    fun delete(reminder: MedicationReminder) = viewModelScope.launch {
        reminderRepository.delete(reminder)
    }


    fun getIntakesForDate(date: LocalDate): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusNanos(1)
        )
    }
}