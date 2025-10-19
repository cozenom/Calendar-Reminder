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
import android.os.PowerManager
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
        val intakeId = intent.getIntExtra(MedicationReminderWorker.EXTRA_INTAKE_ID, -1)
        if (intakeId == -1) return

        when (intent.action) {
            MedicationReminderWorker.ACTION_SHOW_NOTIFICATION -> showNotification(context, intakeId)
            MedicationReminderWorker.ACTION_TAKEN -> markAsTaken(context, intakeId)
        }
    }

    private fun showNotification(context: Context, intakeId: Int) {
        Log.d("NotificationActionReceiver", "Showing notification for intake $intakeId")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager, context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = MedicationReminderWorker.ACTION_TAKEN
            putExtra(MedicationReminderWorker.EXTRA_INTAKE_ID, intakeId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            intakeId,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take your medication")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_check, "Take", takenPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250))
            .setOnlyAlertOnce(true)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CalendarApp:MedicationReminder"
        )
        wakeLock.acquire(10000) // Release after 10 seconds

        try {
            notificationManager.notify(intakeId, builder.build())
            Log.d("NotificationActionReceiver", "Notification shown for intake $intakeId")
        } catch (e: Exception) {
            Log.e("NotificationActionReceiver", "Error showing notification: ${e.message}", e)
        }
    }

    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        context: Context
    ) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medication Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for taking medications"
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

    private fun markAsTaken(context: Context, intakeId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val intakeDao = database.medicationIntakeDao()
            intakeDao.updateTakenStatus(intakeId, true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(intakeId)
        }
    }

    companion object {
        private const val CHANNEL_ID = "MedicationReminderChannel"
    }
}