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

// Fixed at build time - not user-editable (plan.md scope boundary: content lives in this project's own repo).
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
)

@Serializable
private data class Manifest(
    val appVersion: String? = null,
    val stories: List<ManifestStory> = emptyList(),
)

/** Fetches stories + audio/images straight from this project's GitHub repo's `content/` folder (plan.md Phase 3). */
class GitHubContentRepository(private val context: Context) : StoryRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val manifestCacheFile = File(context.cacheDir, "manifest.json")
    private val audioCacheDir = File(context.cacheDir, "audio").apply { mkdirs() }

    override suspend fun list(): List<StoryItem> = withContext(Dispatchers.IO) {
        val manifest = fetchManifest() ?: readCachedManifest() ?: Manifest()
        manifest.stories.map { story ->
            StoryItem(
                id = story.id,
                title = story.title,
                audioUri = "$RAW_CONTENT_BASE/audio/${story.audioFile}",
                imageUri = story.imageFile?.let { "$RAW_CONTENT_BASE/images/$it" },
                language = story.language,
                isAiGenerated = story.ai,
                publishedDate = story.publishedDate,
                ageMin = story.ageMin,
                ageMax = story.ageMax,
            )
        }
    }

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
