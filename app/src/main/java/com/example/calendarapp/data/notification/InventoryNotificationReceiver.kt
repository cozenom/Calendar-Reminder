package com.example.calendarapp.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.calendarapp.data.database.AppDatabase
import com.example.calendarapp.data.model.PrescriptionRefill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class InventoryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")

        // Use goAsync to extend receiver lifetime for async operations
        val pendingResult = goAsync()

        when (intent.action) {
            InventoryNotificationHelper.ACTION_REFILLED -> {
                val reminderId =
                    intent.getIntExtra(InventoryNotificationHelper.EXTRA_REMINDER_ID, -1)
                Log.d(TAG, "ACTION_REFILLED for reminderId: $reminderId")
                if (reminderId != -1) {
                    handleRefilled(context, reminderId, pendingResult)
                } else {
                    Log.e(TAG, "Invalid reminderId received")
                    pendingResult.finish()
                }
            }

            InventoryNotificationHelper.ACTION_DISMISS -> {
                val notificationId =
                    intent.getIntExtra(InventoryNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
                Log.d(TAG, "ACTION_DISMISS for notificationId: $notificationId")
                if (notificationId != -1) {
                    handleDismiss(context, notificationId)
                    pendingResult.finish()
                } else {
                    Log.e(TAG, "Invalid notificationId received")
                    pendingResult.finish()
                }
            }

            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
                pendingResult.finish()
            }
        }
    }

    private fun handleRefilled(context: Context, reminderId: Int, pendingResult: PendingResult) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val reminderDao = database.medicationReminderDao()
                val refillDao = database.prescriptionRefillDao()

                // Get the reminder and latest refill
                val reminder = reminderDao.getReminderById(reminderId).firstOrNull()
                val latestRefill = refillDao.getLatestRefill(reminderId)

                if (reminder == null) {
                    Log.e(TAG, "Reminder not found for id: $reminderId")
                    pendingResult.finish()
                    return@launch
                }

                Log.d(
                    TAG,
                    "Processing refill for ${reminder.medicationName}, latestRefill: ${latestRefill != null}"
                )

                val pillsToAdd: Int
                val newRefill: PrescriptionRefill?

                if (latestRefill != null) {
                    // Has prescription data - use it
                    pillsToAdd = latestRefill.pillsPerRefill
                    newRefill = PrescriptionRefill(
                        reminderId = reminderId,
                        pickupDate = LocalDate.now(),
                        pillsPerRefill = latestRefill.pillsPerRefill,
                        totalRefillsAuthorized = latestRefill.totalRefillsAuthorized,
                        refillsRemaining = maxOf(0, latestRefill.refillsRemaining - 1)
                    )
                    refillDao.insert(newRefill)
                    Log.d(
                        TAG,
                        "Created refill record: $pillsToAdd pills, ${newRefill.refillsRemaining} refills remaining"
                    )
                } else {
                    // No prescription data - use default amount
                    Log.w(
                        TAG,
                        "No prescription data found, using default refill amount: $DEFAULT_REFILL_AMOUNT"
                    )
                    pillsToAdd = DEFAULT_REFILL_AMOUNT
                    newRefill = null

                    // Show toast on main thread to inform user
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Added $DEFAULT_REFILL_AMOUNT pills. Please add prescription details in the app.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Add pills to inventory
                val newInventory = reminder.currentInventory + pillsToAdd
                reminderDao.updateInventory(reminderId, newInventory)
                Log.d(TAG, "Updated inventory: ${reminder.currentInventory} → $newInventory")

                // Cancel all inventory notifications for this medication
                val notificationIdZero = 30000 + reminderId // NOTIFICATION_ID_ZERO_INVENTORY_BASE
                val notificationIdLow = 20000 + reminderId // NOTIFICATION_ID_LOW_INVENTORY_BASE
                val notificationIdNoRefills = 10000 + reminderId // NOTIFICATION_ID_NO_REFILLS_BASE
                InventoryNotificationHelper.cancelNotification(context, notificationIdZero)
                InventoryNotificationHelper.cancelNotification(context, notificationIdLow)
                InventoryNotificationHelper.cancelNotification(context, notificationIdNoRefills)
                Log.d(TAG, "Cancelled inventory notifications")

                // Check if we need to show other alerts (e.g., no refills remaining)
                InventoryNotificationHelper.checkAndSendInventoryAlerts(
                    context,
                    reminder.copy(currentInventory = newInventory),
                    newRefill
                )

                // Reschedule notifications after inventory change
                MedicationReminderWorker.rescheduleNotifications(context)
                Log.d(TAG, "Refill completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error handling refill", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleDismiss(context: Context, notificationId: Int) {
        // Simply cancel the notification
        InventoryNotificationHelper.cancelNotification(context, notificationId)
    }

    companion object {
        private const val TAG = "InventoryNotifReceiver"
        private const val DEFAULT_REFILL_AMOUNT =
            60 // Default pills per refill when no prescription data
    }
}
