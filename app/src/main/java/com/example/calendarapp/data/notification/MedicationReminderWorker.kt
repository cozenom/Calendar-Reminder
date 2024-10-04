package com.example.calendarapp.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.calendarapp.MainActivity
import com.example.calendarapp.R
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.notification.NotificationActionReceiver
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {



    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val intakeDao = database.medicationIntakeDao()
        val reminderDao = database.medicationReminderDao()

        val now = LocalDateTime.now()
        val upcoming = intakeDao.getUpcomingIntakes(now, now.plusMinutes(15)).first()

        upcoming.forEach { intake ->
            val reminder = reminderDao.getReminderById(intake.reminderId).first()
            showNotification(intake.id, reminder.medicationName)
        }

        // Schedule the next check
        scheduleNextCheck()

        return Result.success()
    }
    private fun showNotification(intakeId: Int, medicationName: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medication Reminders",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val takenIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra(EXTRA_INTAKE_ID, intakeId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(applicationContext, intakeId, takenIntent, PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_INTAKE_ID, intakeId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(applicationContext, intakeId + 1000, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take $medicationName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Taken", takenPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)

        notificationManager.notify(intakeId, builder.build())
    }


    private fun scheduleNextCheck() {
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    companion object {
        private const val CHANNEL_ID = "MedicationReminderChannel"
        const val ACTION_TAKEN = "com.example.calendarapp.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.example.calendarapp.ACTION_SNOOZE"
        const val EXTRA_INTAKE_ID = "intake_id"

        fun schedule(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}