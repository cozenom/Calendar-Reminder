package com.example.calendarapp.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val intakeDao = database.medicationIntakeDao()

        val now = LocalDateTime.now()
        val futureIntakes = intakeDao.getFutureIntakes(now)

        cancelAllAlarms()
        scheduleNotificationsForIntakes(futureIntakes)

        // Schedule the next check
        scheduleNextCheck()

        Result.success()
    }

    private fun cancelAllAlarms() {
        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java)
        intent.action = ACTION_SHOW_NOTIFICATION

        // Cancel all potential pending intents (up to a reasonable maximum, e.g., 1000)
        for (i in 0 until 1000) {
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun scheduleNotificationsForIntakes(intakes: List<MedicationIntake>) {
        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        intakes.forEach { intake ->
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
    }

    private fun scheduleNextCheck() {
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                WORKER_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.calendarapp.ACTION_SHOW_NOTIFICATION"
        const val ACTION_TAKEN = "com.example.calendarapp.ACTION_TAKEN"
        const val EXTRA_INTAKE_ID = "intake_id"
        private const val WORKER_NAME = "MedicationReminderWorker"

        fun schedule(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORKER_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }

        fun rescheduleNotifications(context: Context) {
            schedule(context)
        }
    }
}