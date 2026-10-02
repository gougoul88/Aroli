package com.aroli.storybox

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.aroli.storybox.data.AppSettings
import com.aroli.storybox.data.ContentMode
import com.aroli.storybox.data.DEFAULT_PARENT_CODE
import com.aroli.storybox.data.FilteredStoryRepository
import com.aroli.storybox.data.GitHubContentRepository
import com.aroli.storybox.data.LocalFolderRepository
import com.aroli.storybox.data.StoryItem
import com.aroli.storybox.player.PlayerViewModel
import com.aroli.storybox.ui.NoUpdateDialog
import com.aroli.storybox.ui.NightModeScreen
import com.aroli.storybox.ui.ParentCodeDialog
import com.aroli.storybox.ui.ParentSettingsScreen
import com.aroli.storybox.ui.LoadingScreen
import com.aroli.storybox.ui.SleepModeScreen
import com.aroli.storybox.ui.StoryBoxScreen
import com.aroli.storybox.ui.UpdateAvailableDialog
import com.aroli.storybox.ui.UpdateCheckingDialog
import com.aroli.storybox.ui.UpdateErrorDialog
import com.aroli.storybox.util.UpdateChecker
import com.aroli.storybox.util.VersionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime

// Night-time eye comfort: black background, gray arrows/text.
private val NightColorScheme = darkColorScheme(
    background = Color.Black,
    onBackground = Color.Gray,
    surface = Color.Black,
    onSurface = Color.Gray,
    primary = Color.Gray,
    onPrimary = Color.Black,
)

/** Check if current time falls within night mode range */
private fun isNightModeActive(enabled: Boolean, startTime: String, endTime: String): Boolean {
    if (!enabled) return false
    
    try {
        val currentTime = LocalTime.now()
        val start = LocalTime.parse(startTime)
        val end = LocalTime.parse(endTime)
        
        // Handle wrapping time (e.g., 21:00 to 07:00)
        return if (start.isBefore(end)) {
            currentTime.isAfter(start) && currentTime.isBefore(end)
        } else {
            currentTime.isAfter(start) || currentTime.isBefore(end)
        }
    } catch (e: Exception) {
        return false
    }
}

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private lateinit var appSettings: AppSettings
    private lateinit var initialRepository: FilteredStoryRepository
    private var initialLastIndex: Int = 0

    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            lifecycleScope.launch {
                appSettings.setFolderUri(uri.toString())
                appSettings.setMode(ContentMode.LOCAL)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSettings = AppSettings(applicationContext)

        // Preload all initial settings synchronously to avoid "No stories" flicker on startup
        var initialMode = ContentMode.WEB
        var initialFolderUri: String? = null
        var initialAllowAiStories = true
        var initialUserAge: Int? = null
        var initialStoryLanguage = "fr"
        var lastIndexOnStartup = 0
        var initialSyncPeriodDays = 1
        runBlocking {
            initialMode = appSettings.mode.first()
            initialFolderUri = appSettings.folderUri.first()
            initialAllowAiStories = appSettings.allowAiStories.first()
            initialUserAge = appSettings.userAge.first()
            initialStoryLanguage = appSettings.storyLanguage.first()
            lastIndexOnStartup = appSettings.lastIndex.first()
            initialSyncPeriodDays = appSettings.syncPeriodDays.first()
        }
        
        // Pre-load the repository with initial settings before setContent
        val baseRepository = if (initialMode == ContentMode.LOCAL && initialFolderUri != null) {
            LocalFolderRepository(applicationContext, Uri.parse(initialFolderUri))
        } else {
            val ghRepo = GitHubContentRepository(applicationContext)
            ghRepo.setSyncPeriodDays(initialSyncPeriodDays)
            ghRepo
        }
        
        val initialRepository = FilteredStoryRepository(
            baseRepository,
            initialAllowAiStories,
            initialUserAge,
            initialStoryLanguage
        )
        
        // Store in class members for access in setContent lambda
        this.initialRepository = initialRepository
        this.initialLastIndex = lastIndexOnStartup

        // Kid-facing kiosk screen: ignore system back gesture (Phase 5 adds screen pinning on top of this).
        onBackPressedDispatcher.addCallback(this) { /* no-op */ }
        setContent {
            MaterialTheme(colorScheme = NightColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val mode by appSettings.mode.collectAsStateWithLifecycle(initialValue = initialMode)
                    val folderUri by appSettings.folderUri.collectAsStateWithLifecycle(initialValue = initialFolderUri)
                    val allowAiStories by appSettings.allowAiStories.collectAsStateWithLifecycle(initialValue = initialAllowAiStories)
                    val userAge by appSettings.userAge.collectAsStateWithLifecycle(initialValue = initialUserAge)
                    val storyLanguage by appSettings.storyLanguage.collectAsStateWithLifecycle(initialValue = initialStoryLanguage)
                    val sleepTimeoutMinutes by appSettings.sleepTimeoutMinutes.collectAsStateWithLifecycle(initialValue = 10)
                    val showBatteryPercentage by appSettings.showBatteryPercentage.collectAsStateWithLifecycle(initialValue = true)
                    val nightModeEnabled by appSettings.nightModeEnabled.collectAsStateWithLifecycle(initialValue = false)
                    val nightModeStart by appSettings.nightModeStart.collectAsStateWithLifecycle(initialValue = "21:00")
                    val nightModeEnd by appSettings.nightModeEnd.collectAsStateWithLifecycle(initialValue = "07:00")
                    val showTimeDisplay by appSettings.showTimeDisplay.collectAsStateWithLifecycle(initialValue = true)
                    val syncPeriodDays by appSettings.syncPeriodDays.collectAsStateWithLifecycle(initialValue = 1)
                    val parentCode by appSettings.parentCode.collectAsStateWithLifecycle(initialValue = DEFAULT_PARENT_CODE)
                    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

                    var showSettings by remember { mutableStateOf(false) }
                    var showCodeDialog by remember { mutableStateOf(false) }
                    var forceReloadKey by remember { mutableStateOf(0) }
                    var showSleepMode by remember { mutableStateOf(false) }
                    var showNightMode by remember { mutableStateOf(isNightModeActive(nightModeEnabled, nightModeStart, nightModeEnd)) }
                    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
                    var sleepCheckTimer by remember { mutableStateOf(0L) }  // Track sleep check cycles
                    var wasPlayingBeforeSleep by remember { mutableStateOf(false) }  // Track playback state before sleep
                    var currentTime by remember { mutableStateOf("") }  // Current time display string

                    // Load initial repository on first composition to display stories (shows LoadingScreen while loading)
                    LaunchedEffect(Unit) {
                        playerViewModel.load(initialRepository, initialLastIndex)
                    }

                    // Update time display every minute (or every second if preferred)
                    LaunchedEffect(showTimeDisplay) {
                        while (showTimeDisplay) {
                            val now = java.time.LocalTime.now()
                            currentTime = String.format("%02d:%02d", now.hour, now.minute)
                            delay(60000)  // Update every minute
                        }
                    }

                    // Update check timer on state changes (triggers recomposition of sleep effect)
                    LaunchedEffect(showSettings, showCodeDialog) {
                        sleepCheckTimer = System.currentTimeMillis()
                    }

                    // Check night mode status periodically (every minute)
                    LaunchedEffect(nightModeEnabled, nightModeStart, nightModeEnd) {
                        while (true) {
                            showNightMode = isNightModeActive(nightModeEnabled, nightModeStart, nightModeEnd)
                            delay(60000)  // Check every minute
                        }
                    }

                    // Update checker states
                    var showUpdateChecking by remember { mutableStateOf(false) }
                    var showUpdateAvailable by remember { mutableStateOf(false) }
                    var showNoUpdate by remember { mutableStateOf(false) }
                    var showUpdateError by remember { mutableStateOf<String?>(null) }
                    var latestVersion by remember { mutableStateOf<String?>(null) }
                    var releaseNotes by remember { mutableStateOf<String?>(null) }
                    var downloadUrl by remember { mutableStateOf<String?>(null) }

                    // (Re)loads the active repository whenever mode/folder/filters change, restoring the last bookmark.
                    LaunchedEffect(mode, folderUri, allowAiStories, userAge, storyLanguage, syncPeriodDays, forceReloadKey) {
                        val base = if (mode == ContentMode.LOCAL && folderUri != null) {
                            LocalFolderRepository(applicationContext, Uri.parse(folderUri))
                        } else {
                            val ghRepo = GitHubContentRepository(applicationContext)
                            ghRepo.setSyncPeriodDays(syncPeriodDays)
                            ghRepo
                        }
                        val repository = FilteredStoryRepository(base, allowAiStories, userAge, storyLanguage)
                        val startIndex = appSettings.lastIndex.first()
                        playerViewModel.load(repository, startIndex)
                    }

                    // When closing Parent Settings, force reload to apply any filter changes that occurred while it was open.
                    LaunchedEffect(showSettings) {
                        if (!showSettings) {
                            val base = if (mode == ContentMode.LOCAL && folderUri != null) {
                                LocalFolderRepository(applicationContext, Uri.parse(folderUri))
                            } else {
                                val ghRepo = GitHubContentRepository(applicationContext)
                                ghRepo.setSyncPeriodDays(syncPeriodDays)
                                ghRepo
                            }
                            val repository = FilteredStoryRepository(base, allowAiStories, userAge, storyLanguage)
                            playerViewModel.load(repository, uiState.currentIndex)
                        }
                    }

                    // Persists the current position on every navigation (survives reboot, Phase 5 step 14).
                    LaunchedEffect(uiState.currentIndex) {
                        if (uiState.stories.isNotEmpty()) {
                            appSettings.setLastIndex(uiState.currentIndex)
                        }
                    }

                    // Auto-sleep timer: enters sleep mode if no interaction for configured timeout
                    // Uses delay() instead of polling loop to avoid battery drain
                    LaunchedEffect(sleepTimeoutMinutes, sleepCheckTimer, lastInteractionTime) {
                        if (!showSettings && !showCodeDialog && !showSleepMode) {
                            val sleepDelayMillis = (sleepTimeoutMinutes * 60 * 1000L)
                            val elapsedMillis = System.currentTimeMillis() - lastInteractionTime
                            val timeUntilSleep = sleepDelayMillis - elapsedMillis
                            
                            if (timeUntilSleep > 0) {
                                // Sleep until timeout, then check state and trigger sleep mode
                                kotlinx.coroutines.delay(timeUntilSleep)
                                
                                // Verify we're still in playback mode before entering sleep
                                if (!showSettings && !showCodeDialog && !showSleepMode) {
                                    wasPlayingBeforeSleep = uiState.isPlaying
                                    if (wasPlayingBeforeSleep) {
                                        playerViewModel.togglePlayPause()
                                    }
                                    showSleepMode = true
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showSleepMode) {
                            SleepModeScreen(onWake = {
                                showSleepMode = false
                                lastInteractionTime = System.currentTimeMillis()
                                // Restore playback state if it was playing before sleep
                                if (wasPlayingBeforeSleep) {
                                    playerViewModel.togglePlayPause()
                                }
                            })
                        } else if (showSettings) {
                            ParentSettingsScreen(
                                mode = mode,
                                onModeChange = { newMode -> lifecycleScope.launch { appSettings.setMode(newMode) } },
                                onPickFolder = { pickFolderLauncher.launch(null) },
                                onClearCache = {
                                    appSettings.clearCache()
                                    forceReloadKey += 1  // Trigger repository reload with fresh data
                                },
                                allowAiStories = allowAiStories,
                                onAllowAiStoriesChange = { allow -> lifecycleScope.launch { appSettings.setAllowAiStories(allow) } },
                                userAge = userAge,
                                onUserAgeChange = { age -> lifecycleScope.launch { appSettings.setUserAge(age) } },
                                storyLanguage = storyLanguage,
                                onStoryLanguageChange = { lang -> lifecycleScope.launch { appSettings.setStoryLanguage(lang) } },
                                sleepTimeoutMinutes = sleepTimeoutMinutes,
                                onSleepTimeoutChange = { minutes -> lifecycleScope.launch { appSettings.setSleepTimeoutMinutes(minutes) } },
                                showBatteryPercentage = showBatteryPercentage,
                                onShowBatteryPercentageChange = { show -> lifecycleScope.launch { appSettings.setShowBatteryPercentage(show) } },
                                nightModeEnabled = nightModeEnabled,
                                onNightModeEnabledChange = { enabled -> lifecycleScope.launch { appSettings.setNightModeEnabled(enabled) } },
                                nightModeStart = nightModeStart,
                                onNightModeStartChange = { time -> lifecycleScope.launch { appSettings.setNightModeStart(time) } },
                                nightModeEnd = nightModeEnd,
                                onNightModeEndChange = { time -> lifecycleScope.launch { appSettings.setNightModeEnd(time) } },
                                showTimeDisplay = showTimeDisplay,
                                onShowTimeDisplayChange = { show -> lifecycleScope.launch { appSettings.setShowTimeDisplay(show) } },
                                syncPeriodDays = syncPeriodDays,
                                onSyncPeriodDaysChange = { days -> lifecycleScope.launch { appSettings.setSyncPeriodDays(days) } },
                                onChangeCode = { code -> lifecycleScope.launch { appSettings.setParentCode(code) } },
                                onQuitApp = {
                                    lifecycleScope.launch {
                                        try {
                                            stopLockTask()
                                        } catch (e: IllegalArgumentException) {
                                            // Was not pinned - nothing to undo.
                                        }
                                        delay(200)  // Brief delay to ensure stopLockTask completes before finishing.
                                        finishAndRemoveTask()
                                    }
                                },
                                onClose = { 
                                    showSettings = false
                                    // Recalculate night mode immediately after closing settings
                                    showNightMode = isNightModeActive(nightModeEnabled, nightModeStart, nightModeEnd)
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                folderUri = folderUri,
                                onCheckUpdates = {
                                    showUpdateChecking = true
                                    lifecycleScope.launch {
                                        try {
                                            val updateInfo = UpdateChecker.checkForUpdates(this@MainActivity)
                                            showUpdateChecking = false

                                            when {
                                                updateInfo.error != null -> {
                                                    showUpdateError = updateInfo.error
                                                }
                                                updateInfo.isUpdateAvailable -> {
                                                    latestVersion = updateInfo.latestVersion
                                                    releaseNotes = updateInfo.releaseNotes
                                                    downloadUrl = updateInfo.downloadUrl
                                                    showUpdateAvailable = true
                                                }
                                                else -> {
                                                    showNoUpdate = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            showUpdateChecking = false
                                            showUpdateError = "Error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                                            e.printStackTrace()
                                        }
                                    }
                                },
                            )
                        } else if (showNightMode) {
                            NightModeScreen(
                                showBatteryPercentage = showBatteryPercentage,
                                onOpenParentMenu = { 
                                    lastInteractionTime = System.currentTimeMillis()
                                    showCodeDialog = true 
                                },
                            )
                        } else if (uiState.isLoading) {
                            // Show loading screen while stories are being loaded
                            LoadingScreen()
                        } else {
                            StoryBoxScreen(
                                stories = uiState.stories,
                                currentIndex = uiState.currentIndex,
                                unavailableMessage = uiState.unavailableMessage,
                                onPrevious = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    playerViewModel.previous()
                                },
                                onNext = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    playerViewModel.next()
                                },
                                onTogglePlayPause = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    playerViewModel.togglePlayPause()
                                },
                                onOpenParentMenu = { 
                                    lastInteractionTime = System.currentTimeMillis()
                                    showCodeDialog = true 
                                },
                                showBatteryPercentage = showBatteryPercentage,
                                showTimeDisplay = showTimeDisplay,
                                currentTime = currentTime,
                            )
                        }

                        // Parent code dialog - always visible (accessible from night mode and story screen)
                        if (showCodeDialog) {
                            ParentCodeDialog(
                                expectedCode = parentCode,
                                appVersion = VersionInfo.getAppVersion(this@MainActivity),
                                onSuccess = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showCodeDialog = false
                                    showSettings = true
                                },
                                onDismiss = { 
                                    lastInteractionTime = System.currentTimeMillis()
                                    showCodeDialog = false 
                                },
                            )
                        }

                        // Update dialogs
                        if (showUpdateChecking) {
                            UpdateCheckingDialog(onDismiss = { showUpdateChecking = false })
                        }
                        if (showUpdateAvailable && latestVersion != null) {
                            UpdateAvailableDialog(
                                latestVersion = latestVersion!!,
                                releaseNotes = releaseNotes,
                                downloadUrl = downloadUrl,
                                onDismiss = { showUpdateAvailable = false },
                            )
                        }
                        if (showNoUpdate) {
                            NoUpdateDialog(onDismiss = { showNoUpdate = false })
                        }
                        if (showUpdateError != null) {
                            UpdateErrorDialog(
                                error = showUpdateError!!,
                                onDismiss = { showUpdateError = null },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyDeviceOwnerLockTaskConfigIfEnrolled()
        try {
            // Self-service screen pinning (no Device Owner needed) - accepted trade-off if unavailable/declined.
            startLockTask()
        } catch (e: IllegalArgumentException) {
            // Not eligible for lock task mode on this device/build - ignored.
        }
    }

    /**
     * If Aroli has been enrolled as Device Owner (dpm set-device-owner or QR provisioning - see AdminReceiver),
     * whitelist itself for lock task mode and hide the system unpin gesture entirely. No-op otherwise.
     */
    private fun applyDeviceOwnerLockTaskConfigIfEnrolled() {
        val dpm = getSystemService(DevicePolicyManager::class.java) ?: return
        if (!dpm.isDeviceOwnerApp(packageName)) return
        val admin = ComponentName(this, AdminReceiver::class.java)
        dpm.setLockTaskPackages(admin, arrayOf(packageName))
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
    }
}
