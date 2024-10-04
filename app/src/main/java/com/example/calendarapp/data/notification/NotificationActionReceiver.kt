package com.example.calendarapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.calendarapp.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intakeId = intent.getIntExtra(MedicationReminderWorker.EXTRA_INTAKE_ID, -1)
        if (intakeId == -1) return

        when (intent.action) {
            MedicationReminderWorker.ACTION_TAKEN -> markAsTaken(context, intakeId)
            MedicationReminderWorker.ACTION_SNOOZE -> snoozeReminder(context, intakeId)
        }
    }

    private fun markAsTaken(context: Context, intakeId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getDatabase(context)
            val intakeDao = database.medicationIntakeDao()
            intakeDao.updateTakenStatus(intakeId, true)
        }
    }

    private fun snoozeReminder(context: Context, intakeId: Int) {
        val snoozeWork = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(workDataOf(MedicationReminderWorker.EXTRA_INTAKE_ID to intakeId))
            .build()

        WorkManager.getInstance(context).enqueue(snoozeWork)
    }
}