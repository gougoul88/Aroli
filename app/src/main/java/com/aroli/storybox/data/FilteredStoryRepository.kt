package com.aroli.storybox.data

/** Wraps another repository, hiding stories that don't match the parent-configured AI/age/language filters. */
class FilteredStoryRepository(
    private val delegate: StoryRepository,
    private val allowAiStories: Boolean,
    private val userAge: Int?,
    private val storyLanguage: String = "fr",  // ISO 639-1 language code
) : StoryRepository {

    override suspend fun list(): List<StoryItem> = delegate.list().filter { story ->
        val aiOk = allowAiStories || !story.isAiGenerated
        val ageOk = userAge == null ||
            ((story.ageMin == null || userAge >= story.ageMin) && (story.ageMax == null || userAge <= story.ageMax))
        val languageOk = story.language == storyLanguage
        aiOk && ageOk && languageOk
    }

    override suspend fun resolvePlayableUri(item: StoryItem): String = delegate.resolvePlayableUri(item)
}
