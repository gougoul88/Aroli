package com.aroli.storybox.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateAvailableDialog(
    latestVersion: String,
    releaseNotes: String?,
    downloadUrl: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Available") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Version $latestVersion", style = MaterialTheme.typography.labelLarge)

                if (releaseNotes != null) {
                    Text(
                        releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            if (downloadUrl != null) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    context.startActivity(intent)
                    onDismiss()
                }) {
                    Text("Download")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
}

@Composable
fun UpdateCheckingDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Checking Updates") },
        text = { Text("Checking for updates...") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun UpdateErrorDialog(
    error: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(error) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun NoUpdateDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No Update") },
        text = { Text("You already have the latest version.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}
