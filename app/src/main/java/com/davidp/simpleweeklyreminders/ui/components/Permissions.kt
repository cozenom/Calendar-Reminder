package com.davidp.simpleweeklyreminders.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** The two permissions reminders depend on. Both read as granted below the API that added them. */
data class ReminderPermissions(val notifications: Boolean, val exactAlarms: Boolean)

/** What the Reminders-tab banner asks the user to fix — one at a time, most important first. */
enum class PermissionNotice { NOTIFICATIONS_OFF, EXACT_ALARMS_OFF }

/**
 * Notifications outrank exact alarms: an on-time alarm with no notification to show is
 * pointless. Each lives on its own system page, so they're fixed one after the other anyway.
 */
fun ReminderPermissions.notice(): PermissionNotice? = when {
    !notifications -> PermissionNotice.NOTIFICATIONS_OFF
    !exactAlarms -> PermissionNotice.EXACT_ALARMS_OFF
    else -> null
}

fun Context.readReminderPermissions(): ReminderPermissions = ReminderPermissions(
    notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED,
    exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
)

/**
 * Current permission state, re-read on every resume — the user changes these on a system
 * settings page and comes back. The one source for the Settings rows and the Reminders-tab
 * banner, so the two can't disagree.
 */
@Composable
fun rememberReminderPermissions(): ReminderPermissions {
    val context = LocalContext.current
    var resumeKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return remember(resumeKey) { context.readReminderPermissions() }
}

/** This app's notification settings page. */
fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

/** This app's "Alarms & reminders" page. There's no popup for this permission — only this. */
@RequiresApi(Build.VERSION_CODES.S)
fun exactAlarmSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
