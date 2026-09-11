package com.davidp.simpleweeklyreminders.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.davidp.simpleweeklyreminders.data.settings.DateFormatPref
import com.davidp.simpleweeklyreminders.data.settings.SettingsRepository
import com.davidp.simpleweeklyreminders.data.settings.ThemeMode
import com.davidp.simpleweeklyreminders.data.settings.TimeFormatPref
import com.davidp.simpleweeklyreminders.data.settings.WeekStart
import com.davidp.simpleweeklyreminders.debug.DebugTools
import com.davidp.simpleweeklyreminders.ui.components.GroupSurface
import com.davidp.simpleweeklyreminders.ui.components.SectionLabel
import com.davidp.simpleweeklyreminders.ui.components.exactAlarmSettingsIntent
import com.davidp.simpleweeklyreminders.ui.components.notificationSettingsIntent
import com.davidp.simpleweeklyreminders.ui.components.rememberReminderPermissions
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.tonesFor
import kotlinx.coroutines.launch

// GitHub Pages rendering of privacy-policy.md in this repo. Blank hides the About row,
// so this must stay a URL that actually resolves — the same one goes in the Play
// Console listing. Editing the markdown updates this page on the next push.
private const val PRIVACY_POLICY_URL =
    "https://cozenom.github.io/Calendar-Reminder/privacy-policy"

/** Snooze lengths offered in Settings, in minutes. */
private val SNOOZE_PRESET_MINUTES = listOf(5, 10, 15, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    // Same DataStore singleton as everywhere else — constructing another repo just wraps it.
    val repo = remember { SettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var showThemePacks by remember { mutableStateOf(false) }

    if (showThemePacks) {
        BackHandler { showThemePacks = false }
        ThemePackScreen(
            selected = settings.themePack,
            dynamicColor = settings.dynamicColor,
            onSelectPack = { scope.launch { repo.setThemePack(it) } },
            onSelectDynamic = { scope.launch { repo.setDynamicColor(true) } },
            onBack = { showThemePacks = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
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
            Spacer(Modifier.height(8.dp))
            // Material You lives inside the pack list rather than as its own switch — from
            // the user's side it's one choice: what colours the app.
            ActionRow(
                label = "Theme pack",
                subtitle = if (settings.dynamicColor) "Material You"
                else tonesFor(settings.themePack).label,
                onClick = { showThemePacks = true },
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                label = "Per-reminder colours",
                subtitle = "Adds a colour picker to each reminder",
                checked = settings.perReminderColors,
                onCheckedChange = { scope.launch { repo.setPerReminderColors(it) } }
            )
        }

        SettingsSection("Time & date") {
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
            Spacer(Modifier.height(12.dp))
            SegmentedRow(
                label = "Date format",
                options = listOf(
                    DateFormatPref.SYSTEM to "Auto",
                    DateFormatPref.MONTH_FIRST to "Month 1st",
                    DateFormatPref.DAY_FIRST to "Day 1st"
                ),
                selected = settings.dateFormat,
                onSelect = { scope.launch { repo.setDateFormat(it) } }
            )
            Spacer(Modifier.height(12.dp))
            SegmentedRow(
                label = "Week starts on",
                options = listOf(
                    WeekStart.SUNDAY to "Sunday",
                    WeekStart.MONDAY to "Monday"
                ),
                selected = settings.weekStart,
                onSelect = { scope.launch { repo.setWeekStart(it) } }
            )
        }

        SettingsSection("Reminders") {
            SegmentedRow(
                label = "Snooze duration",
                options = SNOOZE_PRESET_MINUTES.map { it to "${it}m" },
                selected = settings.snoozeMinutes,
                onSelect = { scope.launch { repo.setSnoozeMinutes(it) } }
            )
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
                onClick = { context.startActivity(notificationSettingsIntent(context)) }
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

        // Compile-time false in release, so R8 drops the section and the stub it calls
        if (DebugTools.ENABLED) DeveloperSection(context)
    }
}

/** Debug builds only — seed/clear sample data for testing (see DebugTools in src/debug). */
@Composable
private fun DeveloperSection(context: Context) {
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    SettingsSection("Developer") {
        ActionRow(
            label = "Seed sample reminders",
            subtitle = "Adds ~18 reminders with past history",
            onClick = {
                scope.launch {
                    val count = DebugTools.seedSampleReminders(context)
                    Toast.makeText(context, "Added $count reminders", Toast.LENGTH_SHORT).show()
                }
            },
            trailingIcon = Icons.Filled.AddCircleOutline
        )
        ActionRow(
            label = "Fire test notifications",
            subtitle = "One per importance, in 10 seconds",
            onClick = {
                scope.launch {
                    DebugTools.fireTestNotifications(context)
                    Toast.makeText(context, "Firing in 10s", Toast.LENGTH_SHORT).show()
                }
            },
            trailingIcon = Icons.Filled.NotificationsActive
        )
        ActionRow(
            label = "Add reminder due in 1 min",
            subtitle = "Time to lock the phone and test the real alarm",
            onClick = {
                scope.launch {
                    DebugTools.addReminderDueInOneMinute(context)
                    Toast.makeText(context, "Due in 1 min", Toast.LENGTH_SHORT).show()
                }
            },
            trailingIcon = Icons.Filled.Alarm
        )
        ActionRow(
            label = "Clear all reminders",
            subtitle = "Deletes every reminder and its history",
            onClick = { showClearConfirm = true },
            trailingIcon = Icons.Filled.DeleteSweep
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all reminders") },
            text = { Text("Delete every reminder, archived ones and all history? This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        scope.launch {
                            DebugTools.clearAllReminders(context)
                            Toast.makeText(context, "Cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = MaterialTheme.appShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }, shape = MaterialTheme.appShapes.medium) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PermissionsSection(context: Context) {
    // Same state the Reminders-tab banner reads, re-read on every resume
    val permissions = rememberReminderPermissions()

    SettingsSection("Permissions") {
        PermissionRow(
            label = "Notifications",
            granted = permissions.notifications,
            onClick = { context.startActivity(notificationSettingsIntent(context)) }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(8.dp))
            PermissionRow(
                // Android's own name for it, so it matches the system page and the banner
                label = "Alarms & reminders",
                granted = permissions.exactAlarms,
                onClick = { context.startActivity(exactAlarmSettingsIntent(context)) }
            )
        }
    }
}

/** Uppercase group heading, then the section's rows in one rounded neutral container. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SectionLabel(title)
        GroupSurface(
            modifier = Modifier.fillMaxWidth(),
            // Full-width cards here, not inline groups, so a rounder corner than the default
            shape = MaterialTheme.appShapes.large,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            content = content
        )
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

/** [trailingIcon] distinguishes leaving the app (OpenInNew) from going deeper inside it. */
@Composable
private fun ActionRow(
    label: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.OpenInNew
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.appShapes.medium,
        contentPadding = PaddingValuesRow
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(trailingIcon, contentDescription = null)
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
