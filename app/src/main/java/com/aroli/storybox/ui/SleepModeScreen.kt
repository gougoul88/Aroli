package com.aroli.storybox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** Sleep mode screen: black display with minimal UI, tap anywhere to wake */
@Composable
fun SleepModeScreen(onWake: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onWake() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Tap anywhere indicator text (small, at center)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onWake() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "💤\nTap to wake",
                    fontSize = 24.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
