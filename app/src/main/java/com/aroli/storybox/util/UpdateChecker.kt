package com.aroli.storybox.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class RemoteManifest(
    val appVersion: String? = null,
)

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String? = null,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val error: String? = null,
)

object UpdateChecker {
    private const val GITHUB_MANIFEST_URL = "https://raw.githubusercontent.com/gougoul88/Aroli/main/content/manifest.json"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_MANIFEST_URL)
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateInfo(false, error = "GitHub Error (${response.code})")
            }

            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                return@withContext UpdateInfo(false, error = "Empty response from GitHub")
            }

            val manifest = try {
                json.decodeFromString<RemoteManifest>(body)
            } catch (e: Exception) {
                return@withContext UpdateInfo(false, error = "Error parsing manifest")
            }

            // If appVersion is missing, consider no update available
            if (manifest.appVersion.isNullOrEmpty()) {
                return@withContext UpdateInfo(
                    isUpdateAvailable = false,
                    latestVersion = VersionInfo.getAppVersion(context),
                    releaseNotes = "You have the latest version",
                    downloadUrl = "https://github.com/gougoul88/Aroli/releases",
                )
            }

            val currentVersion = VersionInfo.getAppVersion(context)
            val latestVersion = manifest.appVersion!!
            val isUpdateAvailable = compareVersions(currentVersion, latestVersion) < 0

            UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersion = latestVersion,
                releaseNotes = if (isUpdateAvailable) "Version $latestVersion available" else "You have the latest version",
                downloadUrl = "https://github.com/gougoul88/Aroli/releases",
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(false, error = "Network error: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Compare deux versions (1.0, 1.1, 2.0, etc).
     * Retourne: < 0 si v1 < v2, 0 si v1 == v2, > 0 si v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
