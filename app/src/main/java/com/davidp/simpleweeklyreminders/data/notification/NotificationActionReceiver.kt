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
import com.davidp.simpleweeklyreminders.MainActivity
import com.davidp.simpleweeklyreminders.R
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.iconDrawableRes
import com.davidp.simpleweeklyreminders.data.settings.SettingsRepository
import com.davidp.simpleweeklyreminders.data.settings.timePattern
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

        // goAsync holds a wakelock until finish() — without it the coroutine can be
        // killed as soon as onReceive returns
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderWorker.ACTION_SHOW_NOTIFICATION -> showNotification(context, logId, isSnooze)
                    ReminderWorker.ACTION_COMPLETED -> markAsCompleted(context, logId)
                    ReminderWorker.ACTION_SNOOZE -> snooze(context, logId)
                    ReminderWorker.ACTION_DISMISS -> dismiss(context, logId)
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

        createNotificationChannels(context, notificationManager)

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
        // Matches Reminder.importance's default if the reminder row is somehow gone
        val importance = reminder?.importance ?: Importance.HIGH

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

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ReminderWorker.ACTION_DISMISS
            putExtra(ReminderWorker.EXTRA_LOG_ID, logId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            logId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val largeIcon: Bitmap? = reminder?.icon
            ?.let { iconDrawableRes(it) }
            ?.let { resId -> buildIconBitmap(context, resId) }

        val title = reminder?.title ?: log.title

        // Status bar shows the reminder's own icon; generic bell when it has none
        val smallIconRes = reminder?.icon?.let { iconDrawableRes(it) } ?: R.drawable.ic_notification

        val settings = SettingsRepository(context).read()
        val timePattern = settings.timeFormat.timePattern(context)
        val timeText = log.logDateTime.toLocalTime()
            .format(DateTimeFormatter.ofPattern(timePattern))
        val contentText =
            if (isSnooze) "Snoozed · scheduled for $timeText" else "Scheduled for $timeText"

        // Low/Medium: swipe clears this occurrence (log stays pending, not snoozed).
        // High: swipe snoozes, same as the Snooze button — stickiness protects
        // against losing something important to an accidental swipe. The Dismiss
        // button is always present for a deliberate "leave this honestly missed" choice.
        val swipePendingIntent = if (importance == Importance.HIGH) snoozePendingIntent else dismissPendingIntent

        val builder = NotificationCompat.Builder(context, channelIdFor(importance))
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(contentText)
            .setColor(ReminderWorker.accentColor(context))
            // Header timestamp = when this was scheduled, not when it popped up —
            // matters for snoozed re-fires and delayed inexact alarms
            .setWhen(log.logDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(false)
            .setDeleteIntent(swipePendingIntent)
            // Dismiss, Snooze, Done — increasing commitment left to right, same 3 actions
            // at every importance level. Snooze names its own duration so the length is
            // visible without opening Settings. Action icons render on Wear OS / some OEM
            // skins but not on stock Android phone notifications, so one shared icon
            // (rather than a distinct one per action) avoids maintaining art nobody sees
            // on the common path.
            .addAction(R.drawable.ic_notification, "Dismiss", dismissPendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Snooze ${settings.snoozeMinutes}m",
                snoozePendingIntent
            )
            .addAction(R.drawable.ic_notification, "Done", completedPendingIntent)
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

    /**
     * One channel per importance level, mapped 1:1 onto Android's own channel
     * importance (Low/Default/High) — heads-up display, sound eligibility, and
     * tray sort order all follow from this for free. Channel sound/importance is
     * locked in per ID the first time it's created on a real device, so this is
     * called on every notification post (createNotificationChannel is a no-op if
     * the channel already exists) rather than once at app start.
     */
    private fun createNotificationChannels(context: Context, notificationManager: NotificationManager) {
        val soundAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // A more insistent tone for High, borrowed from the alarm sound slot —
        // no bundled audio asset needed
        val urgentSound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: defaultSound

        val low = NotificationChannel(
            CHANNEL_ID_LOW,
            "Reminders (Low importance)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Low-importance reminders — quiet, swipe clears them"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        val medium = NotificationChannel(
            CHANNEL_ID_MEDIUM,
            "Reminders (Medium importance)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Medium-importance reminders"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
            setSound(defaultSound, soundAttributes)
        }
        val high = NotificationChannel(
            CHANNEL_ID_HIGH,
            "Reminders (High importance)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-importance reminders — sticky, swipe snoozes"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
            setSound(urgentSound, soundAttributes)
        }
        notificationManager.createNotificationChannel(low)
        notificationManager.createNotificationChannel(medium)
        notificationManager.createNotificationChannel(high)
    }

    private suspend fun markAsCompleted(context: Context, logId: Int) {
        val database = AppDatabase.getDatabase(context)
        val reminderLogDao = database.reminderLogDao()
        reminderLogDao.updateCompletedStatus(logId, true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(logId)

        // Acting on a notification counts as seeing it — advances the missed baseline
        BootReceiver.markSeenNow(context)
    }

    private suspend fun snooze(context: Context, logId: Int) {
        // Action-button taps don't auto-dismiss; harmless no-op after a swipe
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(logId)

        val database = AppDatabase.getDatabase(context)
        val log = database.reminderLogDao().getLogById(logId) ?: return

        val snoozeMinutes = SettingsRepository(context).read().snoozeMinutes
        val snoozedUntil = LocalDateTime.now().plusMinutes(snoozeMinutes.toLong())
        // DB first so the snooze survives reboot/force-stop; the alarm is re-derivable
        database.reminderLogDao().updateSnoozedUntil(logId, snoozedUntil)
        ReminderWorker.scheduleSnoozeAlarm(context, log.id, snoozedUntil)

        BootReceiver.markSeenNow(context)

        Log.d("NotificationActionReceiver", "Snoozed log $logId until $snoozedUntil")
    }

    /**
     * Deliberate "leave this honestly missed" action — clears the notification
     * without completing or rescheduling it. The log stays `completed = false`,
     * which already reads as "missed" once its time is past (see
     * ReminderLogDao.getMissedLogsList) — no separate status needed.
     */
    private suspend fun dismiss(context: Context, logId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(logId)

        BootReceiver.markSeenNow(context)

        Log.d("NotificationActionReceiver", "Dismissed log $logId")
    }

    private fun buildIconBitmap(context: Context, resId: Int): Bitmap {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Always the darker tone: this is a filled circle with a white glyph, so it carries
        // its own contrast and doesn't need to follow the shade like setColor() does.
        paint.color = ReminderWorker.ACCENT_COLOR.toColorInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Inset so the icon fits inside the circle
        val inset = size / 5
        val drawable = ContextCompat.getDrawable(context, resId) ?: return bitmap
        drawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        drawable.setBounds(inset, inset, size - inset, size - inset)
        drawable.draw(canvas)

        return bitmap
    }

    companion object {
        private const val CHANNEL_ID_LOW = "ReminderChannelLow"
        private const val CHANNEL_ID_MEDIUM = "ReminderChannelMedium"
        private const val CHANNEL_ID_HIGH = "ReminderChannelHigh"

        private fun channelIdFor(importance: Importance): String = when (importance) {
            Importance.LOW -> CHANNEL_ID_LOW
            Importance.MEDIUM -> CHANNEL_ID_MEDIUM
            Importance.HIGH -> CHANNEL_ID_HIGH
        }
    }
}
