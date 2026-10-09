package com.aroli.storybox.data

/** Wraps another repository, hiding stories that don't match the parent-configured age/language filters.
 * AI-generated stories are managed via the Story Manager web tool - marking stories with the "ai" flag. */
class FilteredStoryRepository(
    internal val delegate: StoryRepository,  // Internal for PlayerViewModel to unwrap for navigation
    private val userAge: Int?,
    private val storyLanguage: String = "fr",  // ISO 639-1 language code
) : StoryRepository {

    override suspend fun list(): List<StoryItem> = delegate.list().filter { story ->
        val ageOk = userAge == null ||
            ((story.ageMin == null || userAge >= story.ageMin) && (story.ageMax == null || userAge <= story.ageMax))
        val languageOk = story.language == storyLanguage
        ageOk && languageOk
    }

    override suspend fun resolvePlayableUri(item: StoryItem): String = delegate.resolvePlayableUri(item)
}
