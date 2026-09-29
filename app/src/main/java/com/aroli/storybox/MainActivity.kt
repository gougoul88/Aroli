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
import com.aroli.storybox.player.PlayerViewModel
import com.aroli.storybox.ui.NoUpdateDialog
import com.aroli.storybox.ui.ParentCodeDialog
import com.aroli.storybox.ui.ParentSettingsScreen
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

// Night-time eye comfort: black background, gray arrows/text (plan.md kid-facing kiosk screen).
private val NightColorScheme = darkColorScheme(
    background = Color.Black,
    onBackground = Color.Gray,
    surface = Color.Black,
    onSurface = Color.Gray,
    primary = Color.Gray,
    onPrimary = Color.Black,
)

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private lateinit var appSettings: AppSettings

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

        // Kid-facing kiosk screen: ignore system back gesture (Phase 5 adds screen pinning on top of this).
        onBackPressedDispatcher.addCallback(this) { /* no-op */ }
        setContent {
            MaterialTheme(colorScheme = NightColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val mode by appSettings.mode.collectAsStateWithLifecycle(initialValue = ContentMode.WEB)
                    val folderUri by appSettings.folderUri.collectAsStateWithLifecycle(initialValue = null)
                    val allowAiStories by appSettings.allowAiStories.collectAsStateWithLifecycle(initialValue = true)
                    val userAge by appSettings.userAge.collectAsStateWithLifecycle(initialValue = null)
                    val storyLanguage by appSettings.storyLanguage.collectAsStateWithLifecycle(initialValue = "fr")
                    val sleepTimeoutMinutes by appSettings.sleepTimeoutMinutes.collectAsStateWithLifecycle(initialValue = 10)
                    val parentCode by appSettings.parentCode.collectAsStateWithLifecycle(initialValue = DEFAULT_PARENT_CODE)
                    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

                    var showSettings by remember { mutableStateOf(false) }
                    var showCodeDialog by remember { mutableStateOf(false) }
                    var forceReloadKey by remember { mutableStateOf(0) }
                    var showSleepMode by remember { mutableStateOf(false) }
                    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
                    var sleepCheckTimer by remember { mutableStateOf(0L) }  // Track sleep check cycles
                    var wasPlayingBeforeSleep by remember { mutableStateOf(false) }  // Track playback state before sleep

                    // Update check timer on state changes (triggers recomposition of sleep effect)
                    LaunchedEffect(showSettings, showCodeDialog) {
                        sleepCheckTimer = System.currentTimeMillis()
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
                    LaunchedEffect(mode, folderUri, allowAiStories, userAge, storyLanguage, forceReloadKey) {
                        val base = if (mode == ContentMode.LOCAL && folderUri != null) {
                            LocalFolderRepository(applicationContext, Uri.parse(folderUri))
                        } else {
                            GitHubContentRepository(applicationContext)
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
                                GitHubContentRepository(applicationContext)
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
                    LaunchedEffect(sleepTimeoutMinutes, sleepCheckTimer) {
                        if (!showSettings && !showCodeDialog && !showSleepMode) {
                            // Only check for auto-sleep when in story playback mode
                            val checkInterval = 1000L  // Check every second
                            while (!showSettings && !showCodeDialog && !showSleepMode) {
                                val elapsedMillis = System.currentTimeMillis() - lastInteractionTime
                                val elapsedMinutes = elapsedMillis / (1000 * 60)
                                if (elapsedMinutes >= sleepTimeoutMinutes) {
                                    // Save playback state before entering sleep
                                    wasPlayingBeforeSleep = uiState.isPlaying
                                    // Only pause if currently playing (avoid accidentally starting playback)
                                    if (wasPlayingBeforeSleep) {
                                        playerViewModel.togglePlayPause()
                                    }
                                    showSleepMode = true
                                    break
                                }
                                kotlinx.coroutines.delay(checkInterval)
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
                            )

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
            // Not eligible for lock task mode on this device/build - ignored per plan.md Further Considerations.
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
