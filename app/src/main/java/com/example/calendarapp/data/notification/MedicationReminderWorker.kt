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
        val reminderDao = database.reminderDao()
        val reminderLogDao = database.reminderLogDao()

        val now = LocalDateTime.now()
        val reminders = reminderDao.getAllRemindersList()

        // Cancel existing alarm for each reminder, then schedule the next one
        reminders.forEach { reminder ->
            cancelAlarm(applicationContext, reminder.id)
            val nextLog = reminderLogDao.getNextLogForReminder(reminder.id, now)
            if (nextLog != null) {
                scheduleAlarm(applicationContext, nextLog)
            }
        }

        Result.success()
    }

    companion object {
        const val ACTION_SHOW_NOTIFICATION = "com.example.calendarapp.ACTION_SHOW_NOTIFICATION"
        const val ACTION_COMPLETED = "com.example.calendarapp.ACTION_COMPLETED"
        const val EXTRA_LOG_ID = "log_id"
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val WORKER_NAME = "ReminderWorker"

        fun scheduleAlarm(context: Context, log: ReminderLog) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
                putExtra(EXTRA_LOG_ID, log.id)
                putExtra(EXTRA_REMINDER_ID, log.reminderId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                log.reminderId, // keyed by reminderId so there's only ever one alarm per reminder
                intent,
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

        fun cancelAlarm(context: Context, reminderId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }

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
