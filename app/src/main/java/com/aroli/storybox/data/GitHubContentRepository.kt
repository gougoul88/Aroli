package com.aroli.storybox.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

// Fixed at build time - not user-editable (content lives in this project's own repo).
private const val GITHUB_OWNER = "gougoul88"
private const val GITHUB_REPO = "Aroli"
private const val GITHUB_BRANCH = "main"
private const val RAW_CONTENT_BASE = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/content"

@Serializable
private data class ManifestStory(
    val id: String,
    val title: String,
    val audioFile: String,
    val imageFile: String? = null,
    val language: String = "fr",  // ISO 639-1 language code
    val ai: Boolean = false,
    val publishedDate: String? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val folder: String? = null,  // Virtual folder path, e.g. "Classiques/Animaux". null = root level
)

@Serializable
private data class Manifest(
    val appVersion: String? = null,
    val stories: List<ManifestStory> = emptyList(),
)

/** Fetches stories + audio/images straight from this project's GitHub repo's `content/` folder. */
class GitHubContentRepository(private val context: Context) : StoryRepository, FolderNavigableRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val manifestCacheFile = File(context.cacheDir, "manifest.json")
    private val audioCacheDir = File(context.cacheDir, "audio").apply { mkdirs() }

    /** Parent-picked subset of story ids to show/download. Null = not configured yet, show everything. */
    private var selectedStoryIds: Set<String>? = null

    /** Current position in the virtual folder tree. Null = root. */
    private var currentFolderPath: String? = null
    private val navigationHistory = mutableListOf<String?>()

    /** Restricts list() to these story ids (parent's "Manage Stories" selection). Null = show all. */
    fun setSelectedStoryIds(ids: Set<String>?) {
        selectedStoryIds = ids
    }

    override suspend fun list(): List<StoryItem> = withContext(Dispatchers.IO) {
        val manifest = fetchManifestIfNeeded() ?: readCachedManifest() ?: Manifest()
        val allStories = manifest.stories.map(::toStoryItem)
        val visible = selectedStoryIds?.let { ids -> allStories.filter { it.id in ids } } ?: allStories
        buildFolderView(visible, currentFolderPath)
    }

    /** Full unfiltered catalog (ignores selection + current folder) - used by the "Manage Stories" screen. */
    suspend fun listCatalog(): List<StoryItem> = withContext(Dispatchers.IO) {
        val manifest = fetchManifestIfNeeded() ?: readCachedManifest() ?: Manifest()
        manifest.stories.map(::toStoryItem)
    }

    /** Builds the folder/story listing visible at [path] (null = root): direct stories + immediate subfolders. */
    private fun buildFolderView(stories: List<StoryItem>, path: String?): List<StoryItem> {
        val directStories = stories.filter { it.folder == path }
        val prefix = if (path == null) "" else "$path/"
        val subfolderNames = stories.mapNotNull { it.folder }
            .filter { it != path && it.startsWith(prefix) }
            .map { it.removePrefix(prefix).substringBefore('/') }
            .distinct()
            .sorted()
        val folderItems = subfolderNames.map { name ->
            StoryItem(
                id = "gh_folder:$prefix$name",
                title = name,
                audioUri = "",
                isFolder = true,
                folder = path,
            )
        }
        return folderItems + directStories.sortedBy { it.title }
    }

    private fun toStoryItem(story: ManifestStory) = StoryItem(
        id = story.id,
        title = story.title,
        audioUri = "$RAW_CONTENT_BASE/audio/${story.audioFile}",
        imageUri = story.imageFile?.let { "$RAW_CONTENT_BASE/images/$it" },
        language = story.language,
        isAiGenerated = story.ai,
        publishedDate = story.publishedDate,
        ageMin = story.ageMin,
        ageMax = story.ageMax,
        folder = story.folder,
    )

    /** Enter a virtual folder. Expects a StoryItem with isFolder=true. */
    override suspend fun navigateInto(folder: StoryItem) {
        if (!folder.isFolder) return
        navigationHistory.add(currentFolderPath)
        currentFolderPath = if (currentFolderPath == null) folder.title else "$currentFolderPath/${folder.title}"
    }

    /** Exit current virtual folder and go back to parent. */
    override suspend fun navigateBack() {
        if (navigationHistory.isNotEmpty()) {
            currentFolderPath = navigationHistory.removeAt(navigationHistory.size - 1)
        }
    }

    /** True if we are inside a subfolder (not at root). */
    override fun canNavigateBack(): Boolean = navigationHistory.isNotEmpty()

    override suspend fun resolvePlayableUri(item: StoryItem): String = withContext(Dispatchers.IO) {
        val cached = File(audioCacheDir, "${item.id}.mp3")
        if (cached.exists()) return@withContext Uri.fromFile(cached).toString()

        try {
            download(item.audioUri, cached)
            Uri.fromFile(cached).toString()
        } catch (e: IOException) {
            // Offline and not cached: surfaced by the caller as an "unavailable offline" state.
            throw IOException("Story '${item.title}' unavailable offline", e)
        }
    }

    private fun fetchManifest(): Manifest? {
        return try {
            val request = Request.Builder().url("$RAW_CONTENT_BASE/manifest.json").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                manifestCacheFile.writeText(body)
                json.decodeFromString<Manifest>(body)
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Fetch manifest only if not already cached. Returns null if cache exists (let caller use cached version). */
    private fun fetchManifestIfNeeded(): Manifest? {
        if (!manifestCacheFile.exists()) {
            return fetchManifest()  // Try to fetch if cache doesn't exist
        }
        return null  // Cache exists, let caller use cached version
    }

    /** Force refresh the manifest from GitHub, overwriting cache. Called when opening Manage Stories. */
    suspend fun forceRefreshManifest() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$RAW_CONTENT_BASE/manifest.json").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        manifestCacheFile.writeText(body)
                    }
                }
            }
        } catch (e: IOException) {
            // Offline or network error - cache will be used on next list() call
        }
    }

    private fun readCachedManifest(): Manifest? {
        if (!manifestCacheFile.exists()) return null
        return try {
            json.decodeFromString<Manifest>(manifestCacheFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    private fun download(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty body for $url")
            destination.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }
}
