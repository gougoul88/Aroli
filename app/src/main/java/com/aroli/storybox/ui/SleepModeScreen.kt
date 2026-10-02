package com.aroli.storybox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aroli.storybox.R

/** Sleep mode screen: displays timeout image, tap anywhere to wake */
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
            // Timeout screen image (tap anywhere to wake)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onWake() },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.timeout),
                    contentDescription = "Sleep mode - tap to wake",
                    modifier = Modifier.size(300.dp),
                )
            }
        }
    }
}
