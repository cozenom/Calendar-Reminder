package com.davidp.simpleweeklyreminders

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.davidp.simpleweeklyreminders.data.notification.BootReceiver
import com.davidp.simpleweeklyreminders.data.notification.ReminderWorker
import com.davidp.simpleweeklyreminders.data.settings.SettingsRepository
import com.davidp.simpleweeklyreminders.ui.archive.ArchiveScreen
import com.davidp.simpleweeklyreminders.ui.archive.newlyArchivedCount
import com.davidp.simpleweeklyreminders.ui.calendar.CalendarTab
import com.davidp.simpleweeklyreminders.ui.form.ReminderFormSheet
import com.davidp.simpleweeklyreminders.ui.reminders.RemindersTab
import com.davidp.simpleweeklyreminders.ui.settings.SettingsScreen
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModel
import com.davidp.simpleweeklyreminders.viewmodel.ReminderViewModelFactory
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ReminderViewModel
    private lateinit var alarmManager: AlarmManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

        requestRequiredPermissions()
        ReminderWorker.schedule(this)
        // User is looking at the app — reset the baseline for "missed reminders" reports
        BootReceiver.markSeenNow(this)

        viewModel = ViewModelProvider(
            this, ReminderViewModelFactory(application)
        )[ReminderViewModel::class.java]

        val settingsRepository = SettingsRepository(applicationContext)
        // Read once, blocking, before the first frame so the stored theme applies with
        // no flash to the default. Only this cold-start read blocks; the flow below is
        // reactive from then on.
        val initialSettings = runBlocking { settingsRepository.read() }

        setContent {
            val settings by settingsRepository.flow.collectAsState(initial = initialSettings)
            CalendarAppTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                themePack = settings.themePack
            ) {
                CompositionLocalProvider(LocalAppSettings provides settings) {
                    ReminderApp(viewModel)
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {}

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {}

                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderApp(viewModel: ReminderViewModel) {
    // Tab 0 = Calendar (home, leftmost, start tab), tab 1 = Reminders
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // One-shot per app session: if reminders auto-lapsed into the Archive since the
    // user last viewed it, surface a heads-up notice (the badge on the Archive icon
    // persists the same information until they actually open it).
    var hasShownArchiveNotice by rememberSaveable { mutableStateOf(false) }
    val archivedReminders by viewModel.archivedReminders.collectAsState()
    LaunchedEffect(archivedReminders) {
        if (hasShownArchiveNotice) return@LaunchedEffect
        val archived = archivedReminders ?: return@LaunchedEffect
        val newCount = newlyArchivedCount(archived, context)
        if (newCount > 0) {
            hasShownArchiveNotice = true
            snackbarHostState.showSnackbar(
                message = "$newCount reminder${if (newCount > 1) "s" else ""} archived since you last checked",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, topBar = {
        // Slim bar just for the settings entry; hidden on the full-screen overlays,
        // which carry their own back headers. Restyle pass will flesh this out.
        if (!showArchive && !showSettings) {
            TopAppBar(
                title = {
                    Text(
                        if (selectedTab == 0) "Calendar" else "Reminders",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }, bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0 && !showSettings,
                onClick = { selectedTab = 0; showArchive = false; showSettings = false },
                icon = {
                    Icon(
                        if (selectedTab == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                        contentDescription = null
                    )
                },
                label = { Text("Calendar") }
            )
            NavigationBarItem(
                selected = selectedTab == 1 && !showSettings,
                onClick = { selectedTab = 1; showSettings = false },
                icon = {
                    Icon(
                        if (selectedTab == 1) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = null
                    )
                },
                label = { Text("Reminders") }
            )
        }
    }, floatingActionButton = {
        if (selectedTab == 1 && !showArchive && !showSettings) {
            ExtendedFloatingActionButton(
                onClick = { showAddReminderDialog = true },
                shape = MaterialTheme.appShapes.medium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New", style = MaterialTheme.typography.labelLarge) }
            )
        }
    }) { paddingValues ->
        if (showSettings) {
            Box(modifier = Modifier.padding(paddingValues)) {
                SettingsScreen(onBack = { showSettings = false })
            }
        } else {
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.padding(paddingValues),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { direction * it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -direction * it / 4 } + fadeOut())
                },
                label = "tabSwitch"
            ) { tab ->
                when (tab) {
                    0 -> CalendarTab(viewModel)
                    else -> if (showArchive) {
                        ArchiveScreen(viewModel, onBack = { showArchive = false })
                    } else {
                        RemindersTab(viewModel, onOpenArchive = { showArchive = true }, snackbarHostState = snackbarHostState)
                    }
                }
            }
        }
    }

    // Back closes the Archive screen first, then returns Reminders to the
    // Calendar (home) tab; back on Calendar is unhandled so the system exits
    // the app (keeps predictive back working)
    BackHandler(enabled = showSettings) {
        showSettings = false
    }
    BackHandler(enabled = showArchive) {
        showArchive = false
    }
    BackHandler(enabled = selectedTab == 1 && !showArchive && !showSettings) {
        selectedTab = 0
    }

    if (showAddReminderDialog) {
        ReminderFormSheet(
            initial = null,
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminder ->
                viewModel.insert(reminder)
                showAddReminderDialog = false
            }
        )
    }
}
