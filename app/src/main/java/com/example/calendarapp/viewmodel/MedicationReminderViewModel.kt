package com.example.calendarapp.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.notification.MedicationReminderWorker
import com.example.calendarapp.data.notification.NotificationActionReceiver
import com.example.calendarapp.data.repository.MedicationIntakeRepository
import com.example.calendarapp.data.repository.MedicationReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MedicationReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicationReminderRepository
    private val intakeRepository: MedicationIntakeRepository
    private val alarmManager: AlarmManager
    val allReminders: Flow<List<MedicationReminder>>

    init {
        val database = AppDatabase.getDatabase(application)
        val reminderDao = database.medicationReminderDao()
        val intakeDao = database.medicationIntakeDao()
        repository = MedicationReminderRepository(reminderDao, intakeDao)
        allReminders = repository.allReminders
        intakeRepository = MedicationIntakeRepository(intakeDao)
        alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun insert(reminder: MedicationReminder) = viewModelScope.launch {
        repository.insert(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
    }

    fun update(reminder: MedicationReminder) = viewModelScope.launch {
        val oldReminder = repository.getReminderById(reminder.id).first()
        repository.update(reminder)
        if (oldReminder.reminderTimes != reminder.reminderTimes) {
            cancelNotificationsForReminder(oldReminder)
            scheduleNotificationsForReminder(reminder)
        }
    }

    fun delete(reminder: MedicationReminder) = viewModelScope.launch {
        repository.delete(reminder)
        MedicationReminderWorker.rescheduleNotifications(getApplication())
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

    fun getIntakesForMonth(yearMonth: YearMonth): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            yearMonth.atDay(1).atStartOfDay(),
            yearMonth.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1)
        )
    }

    fun getIntakesForDate(date: LocalDate): Flow<List<MedicationIntake>> {
        return intakeRepository.getIntakesForDateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay().minusNanos(1)
        )
    }

    private fun scheduleNotificationsForReminder(reminder: MedicationReminder) {
        val now = LocalDateTime.now()
        val endDate = reminder.endDate ?: now.plusYears(1).toLocalDate()

        var currentDate = maxOf(now.toLocalDate(), reminder.startDate)
        while (currentDate <= endDate) {
            if (reminder.reminderDays.contains(currentDate.dayOfWeek.value)) {
                for (time in reminder.reminderTimes) {
                    val intakeDateTime = LocalDateTime.of(currentDate, time)
                    if (intakeDateTime > now) {
                        scheduleNotification(reminder, intakeDateTime)
                    }
                }
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    private fun scheduleNotification(reminder: MedicationReminder, intakeDateTime: LocalDateTime) {
        val intent = Intent(getApplication(), NotificationActionReceiver::class.java).apply {
            action = MedicationReminderWorker.ACTION_SHOW_NOTIFICATION
            putExtra("reminderId", reminder.id)
            putExtra("intakeDateTime", intakeDateTime.toString())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            reminder.id * 10000 + intakeDateTime.toLocalTime().toSecondOfDay(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = intakeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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

    private fun cancelNotificationsForReminder(reminder: MedicationReminder) {
        val intent = Intent(getApplication(), NotificationActionReceiver::class.java)
        intent.action = MedicationReminderWorker.ACTION_SHOW_NOTIFICATION

        for (time in reminder.reminderTimes) {
            val pendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                reminder.id * 10000 + time.toSecondOfDay(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}