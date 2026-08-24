package com.davidp.simpleweeklyreminders.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.davidp.simpleweeklyreminders.MainActivity
import com.davidp.simpleweeklyreminders.R
import com.davidp.simpleweeklyreminders.data.database.AppDatabase
import com.davidp.simpleweeklyreminders.data.model.OccurrenceStatus
import com.davidp.simpleweeklyreminders.data.model.statusOf
import com.davidp.simpleweeklyreminders.data.settings.SettingsRepository
import com.davidp.simpleweeklyreminders.data.settings.timePattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderWorker.schedule(context)

            // goAsync holds a wakelock until finish() (see NotificationActionReceiver)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    showMissedNotification(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun showMissedNotification(context: Context) {
        // Opt-out: user turned the missed-reminder summary off in Settings
        val settings = SettingsRepository(context).read()
        if (!settings.missedSummaryEnabled) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // No baseline yet (first run before the app was ever opened): report nothing
        val since = prefs.getString(KEY_LAST_SEEN, null)
            ?.let { LocalDateTime.parse(it) }
            ?: return

        val now = LocalDateTime.now()
        val database = AppDatabase.getDatabase(context)
        // Same statusOf() the calendar uses, so a snoozed reminder isn't counted here while
        // the calendar still shows it as pending.
        val missedLogs = database.reminderLogDao()
            .getElapsedIncompleteLogs(since, now)
            .filter { statusOf(it, now) == OccurrenceStatus.MISSED }
        if (missedLogs.isEmpty()) return

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
        val title = "Missed Reminder${if (count > 1) "s" else ""}"
        val summaryLine = "You missed $count reminder${if (count > 1) "s" else ""} since you last checked"

        // Naming what was missed is the whole point — a bare count tells you to go and look,
        // which is the thing you already knew.
        val timePattern = settings.timeFormat.timePattern(context)
        val lines = missedLogs
            .sortedBy { it.logDateTime }
            .map { "${it.title} · ${it.logDateTime.format(DateTimeFormatter.ofPattern(timePattern))}" }
        val inbox = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .also { style -> lines.take(MAX_MISSED_LINES).forEach(style::addLine) }
            .also { style ->
                // The tray silently truncates past ~6 lines, so account for the rest
                val hidden = lines.size - MAX_MISSED_LINES
                style.setSummaryText(if (hidden > 0) "+$hidden more" else summaryLine)
            }

        val notification = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(inbox)
            .setColor(ReminderWorker.accentColor(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(MISSED_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val MISSED_CHANNEL_ID = "MissedRemindersChannel"
        private const val MISSED_NOTIFICATION_ID = 9999
        /** InboxStyle renders at most ~6 lines; the rest become a "+N more" summary. */
        private const val MAX_MISSED_LINES = 6
        const val PREFS_NAME = "missed_notification_prefs"
        // Baseline for "missed" reports: bumped on every app open, when the missed
        // notification is dismissed, and when a reminder notification is completed
        // or snoozed (see NotificationActionReceiver) — whichever happened last wins
        const val KEY_LAST_SEEN = "last_seen_at"
        const val ACTION_MISSED_DISMISSED = "com.davidp.simpleweeklyreminders.ACTION_MISSED_DISMISSED"

        fun markSeenNow(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit(commit = true) { putString(KEY_LAST_SEEN, LocalDateTime.now().toString()) }
        }
    }
}
