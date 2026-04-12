package com.davidp.simpleweeklyreminders.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.davidp.simpleweeklyreminders.MainActivity
import com.davidp.simpleweeklyreminders.R
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.iconDrawableRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BootReceiver.ACTION_MISSED_DISMISSED) {
            saveDismissedTimestamp(context)
            return
        }

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

        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val log = database.reminderLogDao().getLogById(logId) ?: return@launch
            val reminder = database.reminderDao().getReminderByIdOnce(log.reminderId)

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

            val largeIcon: Bitmap? = reminder?.icon
                ?.let { iconDrawableRes(it) }
                ?.let { resId -> buildIconBitmap(context, resId) }

            val title = reminder?.title ?: log.title

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medication)
                .setContentTitle(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_check, "Completed", completedPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 250))
                .setOnlyAlertOnce(true)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            try {
                notificationManager.notify(logId, builder.build())
                Log.d("NotificationActionReceiver", "Notification shown for log $logId")
            } catch (e: Exception) {
                Log.e("NotificationActionReceiver", "Error showing notification: ${e.message}", e)
            }

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

    private fun saveDismissedTimestamp(context: Context) {
        context.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(BootReceiver.KEY_LAST_DISMISSED, java.time.LocalDateTime.now().toString()) }
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

    private fun buildIconBitmap(context: Context, resId: Int): Bitmap {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Draw colored circle background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = "#6650A4".toColorInt() // Material primary purple
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw icon in white, inset so it fits inside the circle
        val inset = size / 5
        val drawable = ContextCompat.getDrawable(context, resId) ?: return bitmap
        drawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        drawable.setBounds(inset, inset, size - inset, size - inset)
        drawable.draw(canvas)

        return bitmap
    }

    companion object {
        private const val CHANNEL_ID = "ReminderChannel"
    }
}
