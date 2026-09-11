package com.davidp.simpleweeklyreminders.data.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms every alarm when something changes how the existing ones should fire.
 *
 * - Time zone / clock change: logs store floating wall-clock times ("09:00", no zone), but
 *   AlarmManager only takes absolute instants, converted once at arm time. A zone change moves
 *   local time out from under them — a 09:00 armed in PST would fire at 12:00 after flying to
 *   EST. DST needs nothing: it isn't a zone change, and each alarm is converted with its own
 *   date's zone rules.
 * - Exact-alarm permission granted: alarms armed while it was off went in as inexact
 *   (see ReminderWorker.setExactAlarm) and stay that way until re-armed. Returning from the
 *   system page doesn't restart the app, so nothing else would re-arm them.
 *
 * ReminderWorker already re-derives every alarm (and pending snooze) from the stored times,
 * so this just triggers it. Cheap: one alarm per reminder, the next occurrence only.
 * Separate from [BootReceiver] so the missed-reminders summary stays boot-only.
 */
class RescheduleReceiver : BroadcastReceiver() {
    // The permission constant is API 31; it's a compile-time string, and only S+ sends it
    @SuppressLint("InlinedApi")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> ReminderWorker.schedule(context)
        }
    }
}
