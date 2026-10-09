package com.aroli.storybox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aroli.storybox.data.StoryItem

/**
 * Web-mode-only screen: lets parents pick exactly which GitHub-hosted stories are shown/downloaded,
 * instead of every published story auto-appearing. Grouped by the story's "folder" field.
 */
@Composable
fun ManageStoriesScreen(
    catalog: List<StoryItem>,
    initiallySelectedIds: Set<String>?,  // null = not configured yet - everything currently treated as selected
    isLoading: Boolean,
    onSave: (Set<String>) -> Unit,
    onClose: () -> Unit,
) {
    var selectedIds by remember(catalog, initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds ?: catalog.map { it.id }.toSet())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Manage Stories", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Text(
            "Choose which stories to show and download. Uncheck ones already listened to or disliked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val grouped = catalog.groupBy { it.folder ?: "" }.toSortedMap()
            LazyColumn(modifier = Modifier.weight(1f)) {
                grouped.forEach { (folderPath, stories) ->
                    item(key = "header_$folderPath") {
                        Text(
                            text = folderPath.ifEmpty { "General" },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(stories, key = { it.id }) { story ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = story.id in selectedIds,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + story.id else selectedIds - story.id
                                },
                            )
                            Text(story.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { selectedIds = catalog.map { it.id }.toSet() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Select All")
            }
            Button(
                onClick = { selectedIds = emptySet() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Deselect All")
            }
        }

        Button(
            onClick = { onSave(selectedIds) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("Save")
        }
    }
}
