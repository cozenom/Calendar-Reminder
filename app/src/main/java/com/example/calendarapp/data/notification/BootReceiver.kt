package com.example.calendarapp.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.calendarapp.MainActivity
import com.example.calendarapp.R
import com.example.calendarapp.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderWorker.schedule(context)
            showMissedNotification(context)
        }
    }

    private fun showMissedNotification(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val since = prefs.getString(KEY_LAST_DISMISSED, null)
                ?.let { LocalDateTime.parse(it) }
                ?: LocalDateTime.MIN

            val now = LocalDateTime.now()
            val database = AppDatabase.getDatabase(context)
            val missedLogs = database.reminderLogDao().getMissedLogsList(since, now)
            if (missedLogs.isEmpty()) return@launch

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                MISSED_CHANNEL_ID,
                "Missed Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_MISSED_DISMISSED
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                MISSED_NOTIFICATION_ID,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val count = missedLogs.size
            val notification = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle("Missed Reminder${if (count > 1) "s" else ""}")
                .setContentText("You missed $count reminder${if (count > 1) "s" else ""} while your phone was off")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(MISSED_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val MISSED_CHANNEL_ID = "MissedRemindersChannel"
        private const val MISSED_NOTIFICATION_ID = 9999
        const val PREFS_NAME = "missed_notification_prefs"
        const val KEY_LAST_DISMISSED = "last_dismissed_at"
        const val ACTION_MISSED_DISMISSED = "com.example.calendarapp.ACTION_MISSED_DISMISSED"
    }
}
