package com.example.calendarapp.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.MedicationIntake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.time.Duration
import kotlinx.coroutines.flow.first

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("MedicationReminderWorker", "Worker started on ${LocalDateTime.now()}")
        val database = AppDatabase.getDatabase(applicationContext)
        val intakeDao = database.medicationIntakeDao()

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        val targetDate = if (now.hour == 23 && now.minute >= 59) {
            Log.d("MedicationReminderWorker", "It's almost midnight, scheduling for tomorrow")
            tomorrow
        } else {
            Log.d("MedicationReminderWorker", "Scheduling for today")
            today
        }

        val targetStart = targetDate.atStartOfDay()
        val targetEnd = targetDate.plusDays(1).atStartOfDay().minusNanos(1)

        Log.d("MedicationReminderWorker", "Scheduling alarms for $targetDate")

        val targetIntakes = intakeDao.getIntakesForDateRange(targetStart, targetEnd).first()
        Log.d("MedicationReminderWorker", "Found ${targetIntakes.size} intakes for target date")

        cancelAllAlarms()
        scheduleNotificationsForIntakes(targetIntakes, targetDate)

        scheduleNextCheck()

        Log.d("MedicationReminderWorker", "Worker finished")
        Result.success()
    }

    private fun scheduleNotificationsForIntakes(
        intakes: List<MedicationIntake>,
        targetDate: LocalDate
    ) {
        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()

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

            val intakeDateTime = LocalDateTime.of(
                targetDate,
                intake.intakeDateTime.toLocalTime()
            )

            if (intakeDateTime.isAfter(now)) {
                val triggerTime =
                    intakeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                            Log.d(
                                "MedicationReminderWorker",
                                "Scheduled exact alarm for intake ${intake.id} at $intakeDateTime"
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                            Log.d(
                                "MedicationReminderWorker",
                                "Scheduled inexact alarm for intake ${intake.id} at $intakeDateTime"
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.d(
                            "MedicationReminderWorker",
                            "Scheduled exact alarm for intake ${intake.id} at $intakeDateTime"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(
                        "MedicationReminderWorker",
                        "Error scheduling alarm for intake ${intake.id}: ${e.message}",
                        e
                    )
                }
            } else {
                Log.d(
                    "MedicationReminderWorker",
                    "Skipped scheduling for intake ${intake.id} as it's in the past: $intakeDateTime"
                )
            }
        }
    }

    private fun scheduleNextCheck() {
        val now = LocalDateTime.now()
        val nextRun = now.with(LocalTime.of(23, 59, 59))
        val delay = Duration.between(now, nextRun)

        Log.d("MedicationReminderWorker", "Scheduling next check at $nextRun")

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                WORKER_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    private fun cancelAllAlarms() {
        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java)
        intent.action = ACTION_SHOW_NOTIFICATION

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

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.calendarapp.ACTION_SHOW_NOTIFICATION"
        const val ACTION_TAKEN = "com.example.calendarapp.ACTION_TAKEN"
        const val EXTRA_INTAKE_ID = "intake_id"
        private const val WORKER_NAME = "MedicationReminderWorker"

        fun schedule(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
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