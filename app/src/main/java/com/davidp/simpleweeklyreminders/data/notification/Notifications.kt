package com.davidp.simpleweeklyreminders.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.davidp.simpleweeklyreminders.MainActivity

/**
 * Shared plumbing for the two notification receivers, which had grown identical copies of
 * both of these.
 */

/** Non-null cast, matching the platform contract — every Android device has this service. */
internal val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/**
 * Tap target for every notification: open the app on a fresh task.
 *
 * CLEAR_TASK so a tap lands on the app's start state rather than whatever screen happened to
 * be on the back stack when the alarm fired. [requestCode] only has to distinguish this
 * PendingIntent from others the same component owns.
 */
internal fun launchAppPendingIntent(context: Context, requestCode: Int = 0): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
