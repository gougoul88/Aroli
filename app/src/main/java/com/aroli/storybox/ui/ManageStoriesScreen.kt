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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aroli.storybox.data.StoryItem

/**
 * Web-mode-only screen: lets parents pick exactly which GitHub-hosted stories are shown/downloaded,
 * instead of every published story auto-appearing. Grouped by the story's "folder" field.
 * Also allows setting a child age for filtering stories by age range.
 */
@Composable
fun ManageStoriesScreen(
    catalog: List<StoryItem>,
    initiallySelectedIds: Set<String>?,  // null = not configured yet - everything currently treated as selected
    isLoading: Boolean,
    userAge: Int?,
    onUserAgeChange: (Int?) -> Unit,
    onSave: (Set<String>) -> Unit,
    onClose: () -> Unit,
) {
    // Keep previous selection, but only for stories that still exist in catalog
    val catalogIds = catalog.map { it.id }.toSet()
    val previousSelection = (initiallySelectedIds ?: emptySet()).intersect(catalogIds)
    
    var selectedIds by remember(catalog, initiallySelectedIds) {
        mutableStateOf(previousSelection)  // New stories won't be in selection (unchecked by default)
    }
    var childAge by remember(userAge) { mutableStateOf(userAge?.toFloat() ?: 0f) }
    
    // Filter stories by age: show only those where ageMin <= childAge <= ageMax
    val ageFilteredCatalog = catalog.filter { story ->
        if (childAge == 0f) {
            // No age filter (0 means not set)
            true
        } else {
            val ageInt = childAge.toInt()
            val minOk = story.ageMin?.let { ageInt >= it } ?: true
            val maxOk = story.ageMax?.let { ageInt <= it } ?: true
            minOk && maxOk
        }
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
        
        // Child age filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                "Child Age: ${if (childAge == 0f) "Not set" else childAge.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Slider(
                value = childAge,
                onValueChange = { 
                    childAge = it
                    onUserAgeChange(if (it == 0f) null else it.toInt())
                },
                valueRange = 0f..18f,
                steps = 18,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Slide to set child age. Stories outside this age range will be hidden below.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        
        Text(
            "Choose which stories to show and download (${ageFilteredCatalog.filter { it.id in selectedIds }.size}/${ageFilteredCatalog.size} selected).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val grouped = ageFilteredCatalog.groupBy { it.folder ?: "" }.toSortedMap()
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(story.title, style = MaterialTheme.typography.bodyMedium)
                                val ageRange = when {
                                    story.ageMin != null && story.ageMax != null -> "Ages ${story.ageMin}-${story.ageMax}"
                                    story.ageMin != null -> "Ages ${story.ageMin}+"
                                    story.ageMax != null -> "Up to age ${story.ageMax}"
                                    else -> "All ages"
                                }
                                Text(
                                    ageRange,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                onClick = { selectedIds = ageFilteredCatalog.map { it.id }.toSet() },
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
