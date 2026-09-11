package com.davidp.simpleweeklyreminders.ui.reminders

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.ui.components.PermissionNotice
import com.davidp.simpleweeklyreminders.ui.components.exactAlarmSettingsIntent
import com.davidp.simpleweeklyreminders.ui.components.notice
import com.davidp.simpleweeklyreminders.ui.components.notificationSettingsIntent
import com.davidp.simpleweeklyreminders.ui.components.rememberReminderPermissions
import com.davidp.simpleweeklyreminders.ui.theme.appShapes

/**
 * Top-of-list notice while a permission reminders depend on is off. Replaces the settings
 * page that used to open on every launch: the app still works (reminders can be tracked by
 * hand), so this informs rather than blocks. No dismiss — it goes away once fixed. Shows
 * one problem at a time; see [notice] for the order.
 */
@Composable
fun PermissionBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val notice = rememberReminderPermissions().notice() ?: return

    val (icon, message, action) = when (notice) {
        PermissionNotice.NOTIFICATIONS_OFF ->
            Triple(Icons.Outlined.NotificationsOff, "Notifications are off. Reminders won't alert you.", "Turn on")
        PermissionNotice.EXACT_ALARMS_OFF ->
            Triple(Icons.Outlined.AlarmOff, "Alarms & reminders is off. Reminders may arrive late.", "Allow")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.large)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = {
                val intent = when (notice) {
                    PermissionNotice.NOTIFICATIONS_OFF -> notificationSettingsIntent(context)
                    // Only reachable on S+: below it exact alarms always read as granted
                    PermissionNotice.EXACT_ALARMS_OFF ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) exactAlarmSettingsIntent(context) else null
                }
                intent?.let(context::startActivity)
            },
            shape = MaterialTheme.appShapes.medium
        ) {
            Text(action)
        }
    }
}
