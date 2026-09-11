package com.davidp.simpleweeklyreminders.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms every alarm after a time-zone or manual clock change.
 *
 * Logs store floating wall-clock times ("09:00", no zone), but AlarmManager only takes
 * absolute instants, converted once at arm time. A zone change moves local time out from
 * under those instants — a 09:00 armed in PST would fire at 12:00 after flying to EST.
 * ReminderWorker already re-derives every alarm (and pending snooze) from the stored times,
 * so this just triggers it. Cheap: one alarm per reminder, the next occurrence only.
 *
 * DST needs nothing here: it isn't a zone change, and each alarm is converted with its own
 * date's zone rules.
 *
 * Separate from [BootReceiver] so the missed-reminders summary stays boot-only.
 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> ReminderWorker.schedule(context)
        }
    }
}
