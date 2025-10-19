package com.example.calendarapp.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule the MedicationReminderWorker to run immediately after boot
            MedicationReminderWorker.schedule(context)
        }
    }
}