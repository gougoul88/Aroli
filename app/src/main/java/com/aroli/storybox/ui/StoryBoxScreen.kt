package com.aroli.storybox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aroli.storybox.data.StoryItem

/**
 * Kid-facing 3-zone layout: left/right thirds are big prev/next tap targets, middle third shows
 * cover art (or filename fallback) and toggles play/pause on tap. The PIN-gated parent menu is a
 * small always-visible icon in the top-right corner.
 */
@Composable
fun StoryBoxScreen(
    stories: List<StoryItem>,
    currentIndex: Int,
    unavailableMessage: String? = null,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenParentMenu: () -> Unit,
) {
    val current = stories.getOrNull(currentIndex)

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavZone(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (current?.imageUri != null) {
                        AsyncImage(
                            model = current.imageUri,
                            contentDescription = current.title,
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                    } else {
                        Text(
                            text = current?.title ?: "No stories",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    if (unavailableMessage != null) {
                        Text(
                            text = unavailableMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red,
                        )
                    }
                }
            }

            NavZone(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                onClick = onNext,
                modifier = Modifier.weight(1f),
            )
        }

        // Battery indicator in top-left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BatteryIndicator()
        }

        // Settings icon in top-right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Parent menu",
                tint = Color.Gray,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onOpenParentMenu),
            )
        }
    }
}

@Composable
private fun NavZone(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(96.dp),
        )
    }
}
