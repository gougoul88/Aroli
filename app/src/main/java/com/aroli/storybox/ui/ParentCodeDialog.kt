package com.aroli.storybox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Numeric-code gate shown before entering Parent Settings (replaces the old hidden-tap-gesture entry). */
@Composable
fun ParentCodeDialog(
    expectedCode: String,
    appVersion: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showLostCodeDialog by remember { mutableStateOf(false) }
    val backupCode = "568749"  // Backup security code
    val githubUrl = "https://github.com/gougoul88/Aroli"

    if (showLostCodeDialog) {
        AlertDialog(
            onDismissRequest = { showLostCodeDialog = false },
            title = {
                Text("Code Recovery")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "If you forgot your parent code, please visit the link below for the recovery code.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    // Selectable URL for copying
                    SelectionContainer {
                        Text(
                            githubUrl,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .background(
                                    color = Color(0x1A808080),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLostCodeDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Parent Code")
                Text(
                    text = "v$appVersion",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter(Char::isDigit).take(expectedCode.length)
                        error = false
                    },
                    label = { Text(if (error) "Incorrect code" else "Enter code") },
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                )
                
                // Lost code link
                TextButton(
                    onClick = {
                        showLostCodeDialog = true
                    }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Code lost ?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (input == expectedCode || input == backupCode) {
                    onSuccess()
                } else {
                    error = true
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
