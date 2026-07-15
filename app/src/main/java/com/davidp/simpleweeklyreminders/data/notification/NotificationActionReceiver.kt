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
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.davidp.simpleweeklyreminders.MainActivity
import com.davidp.simpleweeklyreminders.R
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.iconDrawableRes
import com.davidp.simpleweeklyreminders.data.settings.SnoozeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BootReceiver.ACTION_MISSED_DISMISSED) {
            BootReceiver.markSeenNow(context)
            return
        }

        val logId = intent.getIntExtra(ReminderWorker.EXTRA_LOG_ID, -1)
        if (logId == -1) return
        val isSnooze = intent.getBooleanExtra(ReminderWorker.EXTRA_IS_SNOOZE, false)

        // goAsync keeps the process alive (with a wakelock) until finish() —
        // without it the coroutine can be killed as soon as onReceive returns
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderWorker.ACTION_SHOW_NOTIFICATION -> showNotification(context, logId, isSnooze)
                    ReminderWorker.ACTION_COMPLETED -> markAsCompleted(context, logId)
                    ReminderWorker.ACTION_SNOOZE -> snooze(context, logId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showNotification(context: Context, logId: Int, isSnooze: Boolean) {
        Log.d("NotificationActionReceiver", "Showing notification for log $logId")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val database = AppDatabase.getDatabase(context)
        val log = database.reminderLogDao().getLogById(logId) ?: return

        // Keep the alarm chain going regardless of whether this log still needs a
        // notification. Skipped for snoozed re-shows: they fire after the original
        // log time, so "next log after logDateTime" could already be in the past
        // and re-arming it would fire immediately as a duplicate.
        if (!isSnooze) {
            val nextLog = database.reminderLogDao().getNextLogForReminder(log.reminderId, log.logDateTime)
            if (nextLog != null) {
                ReminderWorker.scheduleAlarm(context, nextLog)
            }
        }

        // Snooze delivered — clear it so a reboot/reschedule doesn't re-arm it
        if (log.snoozedUntil != null) {
            database.reminderLogDao().updateSnoozedUntil(logId, null)
        }

        // Completed early via the calendar — nothing to notify about
        if (log.completed) return

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

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ReminderWorker.ACTION_SNOOZE
            putExtra(ReminderWorker.EXTRA_LOG_ID, logId)
        }
        // Same requestCode as the Completed action is fine: PendingIntent identity
        // includes the Intent action, which differs
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            logId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val largeIcon: Bitmap? = reminder?.icon
            ?.let { iconDrawableRes(it) }
            ?.let { resId -> buildIconBitmap(context, resId) }

        val title = reminder?.title ?: log.title

        // Status bar shows the reminder's own icon; generic bell when it has none
        val smallIconRes = reminder?.icon?.let { iconDrawableRes(it) } ?: R.drawable.ic_notification

        val is24Hour = DateFormat.is24HourFormat(context)
        val timeText = log.logDateTime.toLocalTime()
            .format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a"))
        val contentText =
            if (isSnooze) "Snoozed · scheduled for $timeText" else "Scheduled for $timeText"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(contentText)
            .setColor(ReminderWorker.ACCENT_COLOR.toColorInt())
            // Header timestamp = when this was scheduled, not when it popped up —
            // matters for snoozed re-fires and delayed inexact alarms
            .setWhen(log.logDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            // Swipeable on purpose: dismissing never discards the reminder, it snoozes
            .setOngoing(false)
            .setAutoCancel(false)
            .setDeleteIntent(snoozePendingIntent)
            // Snooze left, Completed right — matches other calendar apps
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
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

    private suspend fun markAsCompleted(context: Context, logId: Int) {
        val database = AppDatabase.getDatabase(context)
        val reminderLogDao = database.reminderLogDao()
        reminderLogDao.updateCompletedStatus(logId, true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(logId)
    }

    private suspend fun snooze(context: Context, logId: Int) {
        // Action-button taps don't auto-dismiss; harmless no-op after a swipe
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(logId)

        val database = AppDatabase.getDatabase(context)
        val log = database.reminderLogDao().getLogById(logId) ?: return

        val snoozedUntil = LocalDateTime.now()
            .plusMinutes(SnoozeSettings.getSnoozeMinutes(context).toLong())
        // DB first so the snooze survives reboot/force-stop; the alarm is re-derivable
        database.reminderLogDao().updateSnoozedUntil(logId, snoozedUntil)
        ReminderWorker.scheduleSnoozeAlarm(context, log.id, snoozedUntil)

        Log.d("NotificationActionReceiver", "Snoozed log $logId until $snoozedUntil")
    }

    private fun buildIconBitmap(context: Context, resId: Int): Bitmap {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Draw colored circle background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = ReminderWorker.ACCENT_COLOR.toColorInt()
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
