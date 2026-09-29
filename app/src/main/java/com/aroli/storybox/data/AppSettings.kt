package com.aroli.storybox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** Default parent PIN (plan.md: changeable in Parent Settings, not meant as real security - just a kid-proof gate). */
const val DEFAULT_PARENT_CODE = "568749"

private val Context.dataStore by preferencesDataStore(name = "aroli_settings")

enum class ContentMode { LOCAL, WEB }

/**
 * DataStore-backed app settings (plan.md Phase 4): content mode, persisted local folder URI, and the
 * last-played story index (reboot-persisted bookmark).
 */
class AppSettings(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("content_mode")
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val LAST_INDEX = intPreferencesKey("last_index")
        val ALLOW_AI_STORIES = booleanPreferencesKey("allow_ai_stories")
        val USER_AGE = intPreferencesKey("user_age")
        val PARENT_CODE = stringPreferencesKey("parent_code")
        val STORY_LANGUAGE = stringPreferencesKey("story_language")  // ISO 639-1 code: "fr", "en", etc.
    }

    val mode: Flow<ContentMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODE]?.let { runCatching { ContentMode.valueOf(it) }.getOrNull() } ?: ContentMode.WEB
    }

    val folderUri: Flow<String?> = context.dataStore.data.map { it[Keys.FOLDER_URI] }

    val allowAiStories: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_AI_STORIES] ?: true }

    val userAge: Flow<Int?> = context.dataStore.data.map { it[Keys.USER_AGE] }

    val parentCode: Flow<String> = context.dataStore.data.map { it[Keys.PARENT_CODE] ?: DEFAULT_PARENT_CODE }

    val storyLanguage: Flow<String> = context.dataStore.data.map { it[Keys.STORY_LANGUAGE] ?: "fr" }  // Default to French

    suspend fun setMode(mode: ContentMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setFolderUri(uri: String) {
        context.dataStore.edit { it[Keys.FOLDER_URI] = uri }
    }

    val lastIndex: Flow<Int> = context.dataStore.data.map { it[Keys.LAST_INDEX] ?: 0 }

    suspend fun setLastIndex(index: Int) {
        context.dataStore.edit { it[Keys.LAST_INDEX] = index }
    }

    suspend fun setAllowAiStories(allow: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_AI_STORIES] = allow }
    }

    suspend fun setUserAge(age: Int?) {
        context.dataStore.edit {
            if (age == null) it.remove(Keys.USER_AGE) else it[Keys.USER_AGE] = age
        }
    }

    suspend fun setParentCode(code: String) {
        context.dataStore.edit { it[Keys.PARENT_CODE] = code }
    }

    suspend fun setStoryLanguage(language: String) {
        context.dataStore.edit { it[Keys.STORY_LANGUAGE] = language }
    }

    /** Clears the on-disk manifest + downloaded-audio cache used by GitHubContentRepository. */
    fun clearCache() {
        File(context.cacheDir, "audio").deleteRecursively()
        File(context.cacheDir, "manifest.json").delete()
    }
}
