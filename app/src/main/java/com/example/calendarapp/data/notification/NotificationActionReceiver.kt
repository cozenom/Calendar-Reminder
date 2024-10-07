package com.example.calendarapp.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take your medication")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make the notification persistent
            .setAutoCancel(false) // Prevent auto-cancellation
            .addAction(R.drawable.ic_check, "Take", takenPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Explicitly set no sound, vibration, or lights
        builder.setSound(null)
        builder.setVibrate(null)
        builder.setLights(0, 0, 0)

        notificationManager.notify(intakeId, builder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medication reminders"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
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