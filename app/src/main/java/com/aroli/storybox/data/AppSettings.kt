package com.aroli.storybox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** Default parent PIN (changeable in Parent Settings, not meant as real security - just a kid-proof gate). */
const val DEFAULT_PARENT_CODE = "000000"

/** Backup security code in case user loses their PIN (can be used to access parent menu). */
const val BACKUP_PARENT_CODE = "568749"

private val Context.dataStore by preferencesDataStore(name = "aroli_settings")

enum class ContentMode { LOCAL, WEB }

/**
 * DataStore-backed app settings: content mode, persisted local folder URI, and the
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
        val SLEEP_TIMEOUT_MINUTES = intPreferencesKey("sleep_timeout_minutes")  // Auto-sleep timeout in minutes (default 10)
        val SHOW_BATTERY_PERCENTAGE = booleanPreferencesKey("show_battery_percentage")  // Show/hide battery percentage (default true)
        val NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")  // Enable/disable night mode (default false)
        val NIGHT_MODE_START = stringPreferencesKey("night_mode_start")  // Night mode start time (default "21:00")
        val NIGHT_MODE_END = stringPreferencesKey("night_mode_end")  // Night mode end time (default "07:00")
        val SHOW_TIME_DISPLAY = booleanPreferencesKey("show_time_display")  // Show/hide time display (default true)
        val MAX_VOLUME_PERCENT = intPreferencesKey("max_volume_percent")  // Parental volume cap, 0-100 (default 100 = no cap)
        val SELECTED_STORY_IDS = stringSetPreferencesKey("selected_story_ids")  // Web mode: parent-picked subset. Key absent = not configured yet (show all).
    }

    val mode: Flow<ContentMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.MODE]?.let { runCatching { ContentMode.valueOf(it) }.getOrNull() } ?: ContentMode.WEB
    }

    val folderUri: Flow<String?> = context.dataStore.data.map { it[Keys.FOLDER_URI] }

    val allowAiStories: Flow<Boolean> = context.dataStore.data.map { it[Keys.ALLOW_AI_STORIES] ?: true }

    val userAge: Flow<Int?> = context.dataStore.data.map { it[Keys.USER_AGE] }

    val parentCode: Flow<String> = context.dataStore.data.map { it[Keys.PARENT_CODE] ?: DEFAULT_PARENT_CODE }

    val storyLanguage: Flow<String> = context.dataStore.data.map { it[Keys.STORY_LANGUAGE] ?: "fr" }  // Default to French

    val sleepTimeoutMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SLEEP_TIMEOUT_MINUTES] ?: 10 }  // Default to 10 minutes

    val showBatteryPercentage: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_BATTERY_PERCENTAGE] ?: true }  // Default to true

    val nightModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NIGHT_MODE_ENABLED] ?: false }  // Default to false

    val nightModeStart: Flow<String> = context.dataStore.data.map { it[Keys.NIGHT_MODE_START] ?: "21:00" }  // Default 21:00

    val nightModeEnd: Flow<String> = context.dataStore.data.map { it[Keys.NIGHT_MODE_END] ?: "07:00" }  // Default 07:00

    val showTimeDisplay: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TIME_DISPLAY] ?: true }  // Default to true

    val maxVolumePercent: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_VOLUME_PERCENT] ?: 100 }  // Default to 100 (no cap)

    // Null = parent hasn't configured a selection yet - treat as "show all" (backward compatible default).
    val selectedStoryIds: Flow<Set<String>?> = context.dataStore.data.map { it[Keys.SELECTED_STORY_IDS] }

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

    suspend fun setSleepTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SLEEP_TIMEOUT_MINUTES] = minutes }
    }

    suspend fun setShowBatteryPercentage(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_BATTERY_PERCENTAGE] = show }
    }

    suspend fun setNightModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_MODE_ENABLED] = enabled }
    }

    suspend fun setNightModeStart(time: String) {
        context.dataStore.edit { it[Keys.NIGHT_MODE_START] = time }
    }

    suspend fun setNightModeEnd(time: String) {
        context.dataStore.edit { it[Keys.NIGHT_MODE_END] = time }
    }

    suspend fun setShowTimeDisplay(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TIME_DISPLAY] = show }
    }

    suspend fun setMaxVolumePercent(percent: Int) {
        context.dataStore.edit { it[Keys.MAX_VOLUME_PERCENT] = percent.coerceIn(0, 100) }
    }

    suspend fun setSelectedStoryIds(ids: Set<String>) {
        context.dataStore.edit { it[Keys.SELECTED_STORY_IDS] = ids }
    }

    /** Clears the on-disk manifest + downloaded-audio cache used by GitHubContentRepository. */
    fun clearCache() {
        File(context.cacheDir, "audio").deleteRecursively()
        File(context.cacheDir, "manifest.json").delete()
    }
}
