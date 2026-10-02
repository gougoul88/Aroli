package com.aroli.storybox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aroli.storybox.R

/**
 * Night mode screen: displays night mode image, battery indicator in top-left,
 * and parent menu access in top-right.
 */
@Composable
fun NightModeScreen(
    showBatteryPercentage: Boolean = true,
    onOpenParentMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // Night mode image in center
        Image(
            painter = painterResource(id = R.drawable.mode_nuit),
            contentDescription = "Night mode - stories disabled",
            modifier = Modifier
                .fillMaxSize(0.8f),
        )

        // Battery indicator in top-left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BatteryIndicator(showPercentage = showBatteryPercentage)
        }

        // Settings icon in top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.aroli_reglages_80x80),
                contentDescription = "Parent menu",
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onOpenParentMenu),
            )
        }
    }
}
