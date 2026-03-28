package com.example.calendarapp.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.calendarapp.MainActivity
import com.example.calendarapp.R
import com.example.calendarapp.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val logId = intent.getIntExtra(ReminderWorker.EXTRA_LOG_ID, -1)
        if (logId == -1) return

        when (intent.action) {
            ReminderWorker.ACTION_SHOW_NOTIFICATION -> showNotification(context, logId)
            ReminderWorker.ACTION_COMPLETED -> markAsCompleted(context, logId)
        }
    }

    private fun showNotification(context: Context, logId: Int) {
        Log.d("NotificationActionReceiver", "Showing notification for log $logId")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ReminderWorker.ACTION_COMPLETED
            putExtra(ReminderWorker.EXTRA_LOG_ID, logId)
        }
        val completedPendingIntent = PendingIntent.getBroadcast(
            context,
            logId,
            completedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Reminder")
            .setContentText("Time for your reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_check, "Done", completedPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250))
            .setOnlyAlertOnce(true)

        try {
            notificationManager.notify(logId, builder.build())
            Log.d("NotificationActionReceiver", "Notification shown for log $logId")
        } catch (e: Exception) {
            Log.e("NotificationActionReceiver", "Error showing notification: ${e.message}", e)
        }

        // Chain: schedule the next occurrence for this reminder
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val log = database.reminderLogDao().getLogById(logId) ?: return@launch
            val nextLog = database.reminderLogDao().getNextLogForReminder(log.reminderId, log.logDateTime)
            if (nextLog != null) {
                ReminderWorker.scheduleAlarm(context, nextLog)
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            setBypassDnd(true)
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun markAsCompleted(context: Context, logId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val reminderLogDao = database.reminderLogDao()
            reminderLogDao.updateCompletedStatus(logId, true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(logId)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ReminderChannel"
    }
}
