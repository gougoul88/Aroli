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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.aroli.storybox.data.StoryItem
import com.aroli.storybox.R

/**
 * Kid-facing 3-zone layout: left/right thirds are big prev/next tap targets, middle third shows
 * cover art (or filename fallback) and toggles play/pause on tap. The PIN-gated parent menu is a
 * small always-visible icon in the top-right corner.
 * Folder support: folders display with different color (orange), tap to enter. A back button appears at the bottom when inside a folder.
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
    onOpenFolder: (StoryItem) -> Unit = {},
    onGoBack: () -> Unit = {},
    canNavigateBack: Boolean = false,
    showBatteryPercentage: Boolean = true,
    showTimeDisplay: Boolean = true,
    currentTime: String = "",
) {
    val current = stories.getOrNull(currentIndex)
    val isCurrentAFolder = current?.isFolder ?: false

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavZone(
                drawableResId = R.drawable.aroli_precedent_80x80,
                contentDescription = "Previous",
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
            )

            // Middle zone: displays content + optional back button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                if (canNavigateBack) {
                    // When inside a folder: split layout with content on top and back button taking bottom space
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Content area (top)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable(
                                    enabled = stories.isNotEmpty(),
                                    onClick = {
                                        if (isCurrentAFolder) {
                                            current?.let { onOpenFolder(it) }
                                        } else {
                                            onTogglePlayPause()
                                        }
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (current?.imageUri != null && !isCurrentAFolder) {
                                AsyncImage(
                                    model = current.imageUri,
                                    contentDescription = current.title,
                                    modifier = Modifier.fillMaxWidth(0.8f),
                                )
                            } else {
                                Text(
                                    text = current?.title ?: "No stories",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (isCurrentAFolder) Color(0xFFFFA500) else Color.Unspecified,  // Orange for folders
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

                        // Back button area (bottom, full width)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.25f)  // Take 25% of remaining height
                                .clickable(onClick = onGoBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.aroli_retour_arrow),
                                contentDescription = "Back",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                } else {
                    // Normal layout: just content, centered
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                enabled = stories.isNotEmpty(),
                                onClick = {
                                    if (isCurrentAFolder) {
                                        current?.let { onOpenFolder(it) }
                                    } else {
                                        onTogglePlayPause()
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (current?.imageUri != null && !isCurrentAFolder) {
                            AsyncImage(
                                model = current.imageUri,
                                contentDescription = current.title,
                                modifier = Modifier.fillMaxWidth(0.8f),
                            )
                        } else {
                            Text(
                                text = current?.title ?: "No stories",
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isCurrentAFolder) Color(0xFFFFA500) else Color.Unspecified,  // Orange for folders
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
            }

            NavZone(
                drawableResId = R.drawable.aroli_suivant_80x80,
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
            BatteryIndicator(showPercentage = showBatteryPercentage)
        }

        // Time display in top-center
        if (showTimeDisplay && currentTime.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }
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

@Composable
private fun NavZone(
    drawableResId: Int,
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
        Image(
            painter = painterResource(id = drawableResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(96.dp),
        )
    }
}
