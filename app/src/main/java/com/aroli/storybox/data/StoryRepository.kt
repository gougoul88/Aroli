package com.aroli.storybox.data

/** A single playable story: id is stable across app restarts (used for per-profile bookmarks and caching). */
data class StoryItem(
    val id: String,
    val title: String,
    val audioUri: String,
    val imageUri: String? = null,
    val language: String = "fr",  // ISO 639-1 language code (e.g., "fr", "en")
    val isAiGenerated: Boolean = false,
    val publishedDate: String? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val isFolder: Boolean = false,  // True if this is a folder containing stories/subfolders
    val folder: String? = null,  // Web mode virtual folder path (e.g. "Classiques/Animaux"), null = root level
)

/**
 * Common abstraction over where stories come from (local SAF folder or GitHub-hosted content).
 * Implementations: LocalFolderRepository (Phase 2), GitHubContentRepository (Phase 3).
 */
interface StoryRepository {
    suspend fun list(): List<StoryItem>

    /** Resolves a playable URI for [item], downloading/caching it first if needed. */
    suspend fun resolvePlayableUri(item: StoryItem): String
}
