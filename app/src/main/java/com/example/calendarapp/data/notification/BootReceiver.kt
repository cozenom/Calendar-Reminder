package com.example.calendarapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.calendarapp.data.notification.MedicationReminderWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule the MedicationReminderWorker to run immediately after boot
            val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}