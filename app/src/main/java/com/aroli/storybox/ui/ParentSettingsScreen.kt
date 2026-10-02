package com.aroli.storybox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aroli.storybox.data.ContentMode
import com.aroli.storybox.util.VersionInfo

/** "Parent Settings" screen (PIN-gated): mode, folder, AI/age/language filters, code, quit. */
@Composable
fun ParentSettingsScreen(
    mode: ContentMode,
    onModeChange: (ContentMode) -> Unit,
    onPickFolder: () -> Unit,
    onClearCache: () -> Unit,
    allowAiStories: Boolean,
    onAllowAiStoriesChange: (Boolean) -> Unit,
    userAge: Int?,
    onUserAgeChange: (Int?) -> Unit,
    storyLanguage: String,
    onStoryLanguageChange: (String) -> Unit,
    sleepTimeoutMinutes: Int,
    onSleepTimeoutChange: (Int) -> Unit,
    showBatteryPercentage: Boolean,
    onShowBatteryPercentageChange: (Boolean) -> Unit,
    nightModeEnabled: Boolean,
    onNightModeEnabledChange: (Boolean) -> Unit,
    nightModeStart: String,
    onNightModeStartChange: (String) -> Unit,
    nightModeEnd: String,
    onNightModeEndChange: (String) -> Unit,
    showTimeDisplay: Boolean,
    onShowTimeDisplayChange: (Boolean) -> Unit,
    syncPeriodDays: Int,
    onSyncPeriodDaysChange: (Int) -> Unit,
    onChangeCode: (String) -> Unit,
    onQuitApp: () -> Unit,
    onClose: () -> Unit,
    folderUri: String?,
    onCheckUpdates: () -> Unit,
) {
    var showChangeCodeDialog by remember { mutableStateOf(false) }
    var ageText by remember(userAge) { mutableStateOf(userAge?.toString() ?: "") }
    var sleepTimeoutText by remember(sleepTimeoutMinutes) { mutableStateOf(sleepTimeoutMinutes.toString()) }
    var nightModeStartText by remember(nightModeStart) { mutableStateOf(nightModeStart) }
    var nightModeEndText by remember(nightModeEnd) { mutableStateOf(nightModeEnd) }
    var syncPeriodDaysText by remember(syncPeriodDays) { mutableStateOf(syncPeriodDays.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header with title and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Parent Settings", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Content Mode Card - groups mode toggle, folder picker, and mode-specific options
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Mode toggle at the top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (mode == ContentMode.WEB) "Web Mode (GitHub Content)" else "Local Mode",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = mode == ContentMode.WEB,
                        onCheckedChange = { checked -> onModeChange(if (checked) ContentMode.WEB else ContentMode.LOCAL) },
                    )
                }

                // Local folder selection - only in LOCAL mode
                if (mode == ContentMode.LOCAL) {
                    Button(
                        onClick = onPickFolder,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Choose Local Folder")
                    }
                    if (folderUri != null) {
                        Text(
                            text = "📁 $folderUri",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }

                // AI stories filter - only in WEB mode
                if (mode == ContentMode.WEB) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = allowAiStories, onCheckedChange = onAllowAiStoriesChange)
                        Text(
                            "Allow AI-Generated Stories",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    // Cache Synchronization - only in WEB mode
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Cache Synchronization",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        OutlinedTextField(
                            value = syncPeriodDaysText,
                            onValueChange = { text ->
                                syncPeriodDaysText = text.filter(Char::isDigit).take(3)
                                onSyncPeriodDaysChange(syncPeriodDaysText.toIntOrNull() ?: 1)
                            },
                            label = { Text("Sync Period (days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "GitHub manifest cache will refresh after this many days of inactivity",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }

                    // Age filter - only in WEB mode
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { text ->
                            ageText = text.filter(Char::isDigit).take(2)
                            onUserAgeChange(ageText.toIntOrNull())
                        },
                        label = { Text("Child Age (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Language filter - only in WEB mode
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Story Language",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("fr" to "French", "en" to "English", "de" to "German").forEach { (langCode, langName) ->
                                Button(
                                    onClick = { onStoryLanguageChange(langCode) },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (storyLanguage == langCode)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surface,
                                    ),
                                ) {
                                    Text(
                                        langName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (storyLanguage == langCode)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timeout Configuration
        OutlinedTextField(
            value = sleepTimeoutText,
            onValueChange = { text ->
                sleepTimeoutText = text.filter(Char::isDigit).take(3)
                onSleepTimeoutChange(sleepTimeoutText.toIntOrNull() ?: 10)
            },
            label = { Text("Auto-Sleep Timeout (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Device enters sleep mode after this period of inactivity",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // Battery Display Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = showBatteryPercentage, onCheckedChange = onShowBatteryPercentageChange)
                    Text(
                        "Show Battery Percentage",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "Display the battery percentage next to the battery icon",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        // Time Display Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = showTimeDisplay, onCheckedChange = onShowTimeDisplayChange)
                    Text(
                        "Show Time Display",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "Display the current time at the top center of the screen",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        // Night Mode Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = nightModeEnabled, onCheckedChange = onNightModeEnabledChange)
                    Text(
                        "Enable Night Mode",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "Prevent story listening during specified hours",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                if (nightModeEnabled) {
                    // Night mode start time
                    OutlinedTextField(
                        value = nightModeStartText,
                        onValueChange = { newValue ->
                            // Only allow HH:MM format
                            if (newValue.length <= 5 && (newValue.isEmpty() || newValue.matches(Regex("\\d{0,2}:?\\d{0,2}")))) {
                                nightModeStartText = newValue
                                if (newValue.length == 5 && newValue.matches(Regex("\\d{2}:\\d{2}"))) {
                                    onNightModeStartChange(newValue)
                                }
                            }
                        },
                        label = { Text("Start Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Night mode end time
                    OutlinedTextField(
                        value = nightModeEndText,
                        onValueChange = { newValue ->
                            // Only allow HH:MM format
                            if (newValue.length <= 5 && (newValue.isEmpty() || newValue.matches(Regex("\\d{0,2}:?\\d{0,2}")))) {
                                nightModeEndText = newValue
                                if (newValue.length == 5 && newValue.matches(Regex("\\d{2}:\\d{2}"))) {
                                    onNightModeEndChange(newValue)
                                }
                            }
                        },
                        label = { Text("End Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Action buttons below the mode card
        Button(
            onClick = { showChangeCodeDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Change Parent Code")
        }

        Button(
            onClick = onCheckUpdates,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Check Updates")
        }

        Button(
            onClick = onClearCache,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear Cache")
        }

        Button(
            onClick = onQuitApp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Quit Application")
        }

        // Release notes section
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Release Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    VersionInfo.RELEASE_NOTES,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showChangeCodeDialog) {
        ChangeCodeDialog(
            onConfirm = { newCode ->
                onChangeCode(newCode)
                showChangeCodeDialog = false
            },
            onDismiss = { showChangeCodeDialog = false },
        )
    }
}

@Composable
private fun ChangeCodeDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var newCode by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Parent Code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newCode,
                    onValueChange = { newCode = it.filter(Char::isDigit).take(6) },
                    label = { Text("New Code (6 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = confirmCode,
                    onValueChange = { confirmCode = it.filter(Char::isDigit).take(6) },
                    label = { Text("Confirm Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                )
                if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    newCode.length != 6 -> error = "Code must contain 6 digits"
                    newCode != confirmCode -> error = "Codes do not match"
                    else -> onConfirm(newCode)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
