package com.example.calendarapp.data.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val intakeDao = database.medicationIntakeDao()

        val now = LocalDateTime.now()
        val endOfDay = now.toLocalDate().atTime(LocalTime.MAX)
        val upcomingIntakes = intakeDao.getUpcomingIntakes(now, endOfDay)

        upcomingIntakes.forEach { intake ->
            scheduleNotificationForIntake(intake)
        }

        // Reschedule missed notifications
        val missedIntakes = intakeDao.getMissedIntakesSync(now)
        missedIntakes.forEach { intake ->
            scheduleNotificationForIntake(intake, true)
        }

        // Schedule the next day's check
        scheduleNextDayCheck()

        return Result.success()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNotificationForIntake(intake: MedicationIntake, isMissed: Boolean = false) {
        val notificationIntent =
            Intent(applicationContext, NotificationActionReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
                putExtra(EXTRA_INTAKE_ID, intake.id)
            }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            intake.id,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = if (isMissed) {
            System.currentTimeMillis() + 1000 // Schedule missed notifications to show immediately
        } else {
            intake.intakeDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
        }

        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    private fun scheduleNextDayCheck() {
        val nextDay = LocalDate.now().plusDays(1).atStartOfDay()
        val delay = Duration.between(LocalDateTime.now(), nextDay).toMillis()

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.calendarapp.ACTION_SHOW_NOTIFICATION"
        const val ACTION_TAKEN = "com.example.calendarapp.ACTION_TAKEN"
        const val EXTRA_INTAKE_ID = "intake_id"

        fun schedule(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}