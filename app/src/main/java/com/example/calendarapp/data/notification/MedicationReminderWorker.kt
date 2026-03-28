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
import com.example.calendarapp.data.model.ReminderLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val reminderLogDao = database.reminderLogDao()

        val now = LocalDateTime.now()
        val futureLogs = reminderLogDao.getFutureLogs(now)

        cancelAllAlarms()
        scheduleNotificationsForLogs(futureLogs)

        Result.success()
    }

    private fun cancelAllAlarms() {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

    private fun scheduleNotificationsForLogs(logs: List<ReminderLog>) {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        logs.forEach { log ->
            val notificationIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
                putExtra(EXTRA_LOG_ID, log.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                log.id,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = log.logDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.calendarapp.ACTION_SHOW_NOTIFICATION"
        const val ACTION_COMPLETED = "com.example.calendarapp.ACTION_COMPLETED"
        const val EXTRA_LOG_ID = "log_id"
        private const val WORKER_NAME = "ReminderWorker"

        fun schedule(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(0, TimeUnit.SECONDS)
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
