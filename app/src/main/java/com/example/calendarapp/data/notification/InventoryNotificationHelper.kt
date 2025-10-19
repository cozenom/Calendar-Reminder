package com.example.calendarapp.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.calendarapp.MainActivity
import com.example.calendarapp.R
import com.example.calendarapp.data.model.MedicationReminder
import com.example.calendarapp.data.model.PrescriptionRefill

object InventoryNotificationHelper {
    private const val INVENTORY_CHANNEL_ID = "InventoryAlertsChannel"
    private const val NOTIFICATION_ID_NO_REFILLS_BASE = 10000
    private const val NOTIFICATION_ID_LOW_INVENTORY_BASE = 20000
    private const val NOTIFICATION_ID_ZERO_INVENTORY_BASE = 30000

    const val ACTION_REFILLED = "com.example.calendarapp.ACTION_REFILLED"
    const val ACTION_DISMISS = "com.example.calendarapp.ACTION_DISMISS_INVENTORY"
    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    fun checkAndSendInventoryAlerts(
        context: Context,
        reminder: MedicationReminder,
        latestRefill: PrescriptionRefill?
    ) {
        // Check for zero refills
        if (latestRefill != null && latestRefill.refillsRemaining == 0) {
            sendInventoryNotification(
                context = context,
                notificationId = NOTIFICATION_ID_NO_REFILLS_BASE + reminder.id,
                title = "No Refills Remaining",
                message = "Contact your doctor for a new prescription for ${reminder.medicationName}",
                priority = NotificationCompat.PRIORITY_HIGH,
                ongoing = false,
                reminderId = reminder.id,
                showRefilledAction = false
            )
        }

        // Only show inventory alerts if prescription data exists
        if (latestRefill != null) {
            // Check for low inventory (7 days worth or less)
            val lowInventoryThreshold = 7 * reminder.dosagePerIntake
            if (reminder.currentInventory <= lowInventoryThreshold && reminder.currentInventory > 0) {
                val daysRemaining = reminder.currentInventory / reminder.dosagePerIntake
                sendInventoryNotification(
                    context = context,
                    notificationId = NOTIFICATION_ID_LOW_INVENTORY_BASE + reminder.id,
                    title = "Low Medication Inventory",
                    message = "${reminder.medicationName}: ${reminder.currentInventory} pills remaining (~$daysRemaining days). Time to pick up your refill.",
                    priority = NotificationCompat.PRIORITY_DEFAULT,
                    ongoing = false,
                    reminderId = reminder.id,
                    showRefilledAction = false
                )
            }

            // Check for zero inventory - persistent notification with Refilled action
            if (reminder.currentInventory == 0) {
                sendInventoryNotification(
                    context = context,
                    notificationId = NOTIFICATION_ID_ZERO_INVENTORY_BASE + reminder.id,
                    title = "Out of Medication",
                    message = "You are out of ${reminder.medicationName}. Pick up your refill immediately.",
                    priority = NotificationCompat.PRIORITY_HIGH,
                    ongoing = true,
                    reminderId = reminder.id,
                    showRefilledAction = true
                )
            } else {
                // Cancel zero inventory notification if inventory is no longer zero
                cancelNotification(context, NOTIFICATION_ID_ZERO_INVENTORY_BASE + reminder.id)
            }
        } else {
            // No prescription data - cancel any existing inventory notifications
            cancelNotification(context, NOTIFICATION_ID_ZERO_INVENTORY_BASE + reminder.id)
            cancelNotification(context, NOTIFICATION_ID_LOW_INVENTORY_BASE + reminder.id)
        }
    }

    private fun sendInventoryNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        priority: Int,
        ongoing: Boolean,
        reminderId: Int,
        showRefilledAction: Boolean
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createInventoryNotificationChannel(notificationManager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, INVENTORY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250))

        // Add "Refilled" and "Dismiss" actions for zero inventory notifications
        if (showRefilledAction) {
            // Make intent explicit for Android 12+ compatibility
            val refilledIntent = Intent(context, InventoryNotificationReceiver::class.java).apply {
                action = ACTION_REFILLED
                putExtra(EXTRA_REMINDER_ID, reminderId)
                component = ComponentName(context, InventoryNotificationReceiver::class.java)
            }
            val refilledPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                refilledIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_check, "Refilled", refilledPendingIntent)

            // Make intent explicit for Android 12+ compatibility
            val dismissIntent = Intent(context, InventoryNotificationReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                component = ComponentName(context, InventoryNotificationReceiver::class.java)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun createInventoryNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            INVENTORY_CHANNEL_ID,
            "Inventory Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for low inventory and refill reminders"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250)
            setBypassDnd(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
