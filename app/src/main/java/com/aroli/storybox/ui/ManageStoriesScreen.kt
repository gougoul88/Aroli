package com.aroli.storybox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.foundation.text.KeyboardOptions
import com.aroli.storybox.data.StoryItem

/**
 * Web-mode-only screen: lets parents pick exactly which GitHub-hosted stories are shown/downloaded,
 * instead of every published story auto-appearing. Grouped by the story's "folder" field.
 * Also allows setting a child age for filtering stories by age range, and selecting story language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStoriesScreen(
    catalog: List<StoryItem>,
    initiallySelectedIds: Set<String>?,  // null = not configured yet - everything currently treated as selected
    isLoading: Boolean,
    userAge: Int?,
    onUserAgeChange: (Int?) -> Unit,
    storyLanguage: String,
    onStoryLanguageChange: (String) -> Unit,
    onSave: (Set<String>) -> Unit,
    onClose: () -> Unit,
) {
    // Keep previous selection, but only for stories that still exist in catalog
    val catalogIds = catalog.map { it.id }.toSet()
    val previousSelection = (initiallySelectedIds ?: emptySet()).intersect(catalogIds)
    
    var selectedIds by remember(catalog, initiallySelectedIds) {
        mutableStateOf(previousSelection)  // New stories won't be in selection (unchecked by default)
    }
    var ageInput by remember(userAge) { mutableStateOf(userAge?.toString() ?: "") }
    val childAge = ageInput.toIntOrNull() ?: 0
    var showLanguageMenu by remember { mutableStateOf(false) }
    
    // Filter stories by age: show only those where ageMin <= childAge <= ageMax
    val ageFilteredCatalog = catalog.filter { story ->
        if (childAge == 0) {
            // No age filter (0 means not set)
            true
        } else {
            val minOk = story.ageMin?.let { childAge >= it } ?: true
            val maxOk = story.ageMax?.let { childAge <= it } ?: true
            minOk && maxOk
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Header with close button (fixed at top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Manage Stories", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            // Age and Language filters Card
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Age filter - number input
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { text ->
                            ageInput = text.filter(Char::isDigit).take(2)
                            onUserAgeChange(ageInput.toIntOrNull())
                        },
                        label = { Text("Child Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )

                    // Language filter - proper exposed dropdown
                    val languages = listOf("fr" to "Français", "en" to "English", "de" to "Deutsch")
                    val selectedLanguageLabel = languages.firstOrNull { it.first == storyLanguage }?.second ?: storyLanguage
                    ExposedDropdownMenuBox(
                        expanded = showLanguageMenu,
                        onExpandedChange = { showLanguageMenu = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = selectedLanguageLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Language") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLanguageMenu) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                        ) {
                            languages.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onStoryLanguageChange(code)
                                        showLanguageMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Info text
            Text(
                "Age filter: ${if (childAge == 0) "Off" else "$childAge years"} | ${ageFilteredCatalog.filter { it.id in selectedIds }.size}/${ageFilteredCatalog.size} selected",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            // Stories list - simple list (not LazyColumn, so scroll state is managed by parent Column)
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (ageFilteredCatalog.isEmpty()) {
                Text(
                    "No stories match this age range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                // Simple list of stories with checkboxes
                ageFilteredCatalog.forEach { story ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = story.id in selectedIds,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + story.id else selectedIds - story.id
                            },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                story.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val ageRange = when {
                                story.ageMin != null && story.ageMax != null -> "Ages ${story.ageMin}-${story.ageMax}"
                                story.ageMin != null -> "Ages ${story.ageMin}+"
                                story.ageMax != null -> "Up to age ${story.ageMax}"
                                else -> "All ages"
                            }
                            Text(
                                ageRange,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Spacer before buttons
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Fixed buttons at bottom - all in one line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(top = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Button(
                onClick = { onSave(selectedIds) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}
