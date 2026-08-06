package com.davidp.simpleweeklyreminders

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.davidp.simpleweeklyreminders.data.settings.SettingsRepository
import com.davidp.simpleweeklyreminders.data.settings.ThemeMode
import com.davidp.simpleweeklyreminders.data.settings.TimeFormatPref
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import kotlinx.coroutines.launch

// Set to the hosted privacy-policy URL before launch; blank hides the About row.
private const val PRIVACY_POLICY_URL = ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    // Same DataStore singleton as everywhere else — constructing another repo just wraps it.
    val repo = remember { SettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        SettingsSection("Appearance") {
            SegmentedRow(
                label = "Theme",
                options = listOf(
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark",
                    ThemeMode.SYSTEM to "System"
                ),
                selected = settings.themeMode,
                onSelect = { scope.launch { repo.setThemeMode(it) } }
            )
            // Material You is Android 12+ only; hide the toggle where it does nothing.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    label = "Dynamic color",
                    subtitle = "Match the system wallpaper palette",
                    checked = settings.dynamicColor,
                    onCheckedChange = { scope.launch { repo.setDynamicColor(it) } }
                )
            }
        }

        SettingsSection("Time") {
            SegmentedRow(
                label = "Clock format",
                options = listOf(
                    TimeFormatPref.SYSTEM to "Auto",
                    TimeFormatPref.H12 to "12h",
                    TimeFormatPref.H24 to "24h"
                ),
                selected = settings.timeFormat,
                onSelect = { scope.launch { repo.setTimeFormat(it) } }
            )
        }

        SettingsSection("Reminders") {
            Text("Snooze duration", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            val presets = listOf(5, 10, 15, 30, 45, 60)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                presets.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        selected = settings.snoozeMinutes == minutes,
                        onClick = { scope.launch { repo.setSnoozeMinutes(minutes) } },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = presets.size),
                        icon = {}
                    ) {
                        Text("${minutes}m")
                    }
                }
            }
            SwitchRow(
                label = "Missed-reminder summary",
                subtitle = "Notify on restart about reminders missed while off",
                checked = settings.missedSummaryEnabled,
                onCheckedChange = { scope.launch { repo.setMissedSummaryEnabled(it) } }
            )
        }

        SettingsSection("Notifications") {
            ActionRow(
                label = "Sound & vibration",
                subtitle = "Per-importance channels — opens system settings",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            )
        }

        PermissionsSection(context)

        SettingsSection("About") {
            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull().orEmpty()
            }
            Text("Version $versionName", style = MaterialTheme.typography.bodyMedium)
            if (PRIVACY_POLICY_URL.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                ActionRow(
                    label = "Privacy policy",
                    subtitle = null,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) }
                )
            }
        }
    }
}

@Composable
private fun PermissionsSection(context: Context) {
    // Permission state can change when the user returns from a system settings screen;
    // bump a key on every resume so the rows re-read it.
    var resumeKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifGranted = remember(resumeKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    val exactAlarmGranted = remember(resumeKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true
    }

    SettingsSection("Permissions") {
        PermissionRow(
            label = "Notifications",
            granted = notifGranted,
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(8.dp))
            PermissionRow(
                label = "Exact alarms",
                granted = exactAlarmGranted,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            "package:${context.packageName}".toUri()
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/** Labeled single-choice segmented control shared by the enum settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Text(label, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, text) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {}
            ) {
                Text(text)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(label: String, subtitle: String?, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.appShapes.medium,
        contentPadding = PaddingValuesRow
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.Filled.OpenInNew, contentDescription = null)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (granted) "Granted" else "Not granted",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error
            )
        }
        if (!granted) {
            TextButton(onClick = onClick, shape = MaterialTheme.appShapes.medium) {
                Text("Grant")
            }
        }
    }
}

private val PaddingValuesRow = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
