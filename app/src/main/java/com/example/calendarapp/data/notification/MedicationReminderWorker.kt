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
import java.time.Duration
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val intakeDao = database.medicationIntakeDao()

        val tomorrow = LocalDate.now().plusDays(1)
        val dayAfterTomorrow = tomorrow.plusDays(1)

        // Get intakes for tomorrow only
        val tomorrowIntakes = intakeDao.getIntakesForDateRange(
            tomorrow.atStartOfDay(),
            dayAfterTomorrow.atStartOfDay()
        ).first()

        cancelAllAlarms()
        scheduleNotificationsForIntakes(tomorrowIntakes)

        // Schedule the next check for tomorrow at 23:59
        scheduleNextCheck()

        Result.success()
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
        val nextRun = LocalDateTime.now().withHour(23).withMinute(59).withSecond(0)
        val delay = Duration.between(LocalDateTime.now(), nextRun)

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                WORKER_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    private fun cancelAllAlarms() {
        // Implementation remains the same
    }

    companion object {
        private const val WORKER_NAME = "MedicationReminderWorker"
        const val ACTION_SHOW_NOTIFICATION = "com.example.calendarapp.ACTION_SHOW_NOTIFICATION"
        const val EXTRA_INTAKE_ID = "intake_id"

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
    }
}