package com.aroli.storybox.data

/** Implemented by repositories that support folder navigation (real SAF folders or virtual web-mode folders). */
interface FolderNavigableRepository {
    suspend fun navigateInto(folder: StoryItem)
    suspend fun navigateBack()
    fun canNavigateBack(): Boolean
}
