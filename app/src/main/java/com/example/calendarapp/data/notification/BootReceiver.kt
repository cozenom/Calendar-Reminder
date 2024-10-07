package com.example.calendarapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.calendarapp.data.notification.MedicationReminderWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule the MedicationReminderWorker to run immediately after boot
            MedicationReminderWorker.schedule(context)
        }
    }
}