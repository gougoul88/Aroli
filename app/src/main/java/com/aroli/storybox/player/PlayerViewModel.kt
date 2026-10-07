package com.aroli.storybox.player

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.aroli.storybox.data.StoryItem
import com.aroli.storybox.data.StoryRepository
import com.aroli.storybox.data.LocalFolderRepository
import com.aroli.storybox.data.FilteredStoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val stories: List<StoryItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val unavailableMessage: String? = null,
    val isLoading: Boolean = true,  // Tracks whether stories are still being loaded
    val canNavigateBack: Boolean = false,  // True if inside a subfolder (only when using LocalFolderRepository)
)

/** Wraps a single ExoPlayer instance: play/pause, wrap-around prev/next over the active repository's list. */
@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: StoryRepository? = null
    private var localFolderRepository: LocalFolderRepository? = null  // For folder navigation

    /** Index currently loaded into the player via setMediaItem+prepare, or null if nothing prepared yet. */
    private var preparedIndex: Int? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Sources are always local (SAF file or pre-downloaded cache file) - the default 2.5s buffer-before-play
    // threshold is a network-streaming safeguard we don't need, and was the main cause of a perceived delay
    // when starting playback.
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
            300,
            500,
        )
        .build()

    // handleAudioFocus = true lets ExoPlayer duck/pause automatically on focus loss.
    private val player: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true,
        )
        .setLoadControl(loadControl)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    init {
        application.registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        })
    }

    /** Loads [repository]'s story list and starts playback at [startIndex] (e.g. a restored per-profile bookmark). */
    fun load(repository: StoryRepository, startIndex: Int = 0) {
        this.repository = repository
        this.localFolderRepository = extractLocalFolderRepository(repository)
        _uiState.update { it.copy(isLoading = true) }  // Mark as loading
        viewModelScope.launch {
            val stories = repository.list()
            val index = startIndex.coerceIn(0, (stories.size - 1).coerceAtLeast(0))
            val canNavigateBack = localFolderRepository?.canNavigateBack() ?: false
            _uiState.update { it.copy(stories = stories, currentIndex = index, isLoading = false, canNavigateBack = canNavigateBack) }  // Mark loading complete
            if (stories.isNotEmpty()) preparePlayback(index)
        }
    }

    /** Initializes the player with preloaded stories and prepares playback at [startIndex]. Used for startup optimization. */
    fun loadWithInitialStories(repository: StoryRepository, stories: List<StoryItem>, startIndex: Int = 0) {
        this.repository = repository
        this.localFolderRepository = extractLocalFolderRepository(repository)
        val index = startIndex.coerceIn(0, (stories.size - 1).coerceAtLeast(0))
        val canNavigateBack = localFolderRepository?.canNavigateBack() ?: false
        _uiState.value = PlayerUiState(stories = stories, currentIndex = index, isLoading = false, canNavigateBack = canNavigateBack)  // Already loaded
        if (stories.isNotEmpty()) {
            viewModelScope.launch {
                preparePlayback(index)
            }
        }
    }

    /** Prepares the current item on-demand if it wasn't already (e.g. right after entering a folder), then toggles play/pause. */
    fun togglePlayPause() {
        val index = _uiState.value.currentIndex
        if (preparedIndex != index) {
            viewModelScope.launch {
                preparePlayback(index)
                player.playWhenReady = true
            }
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun next() = navigate(+1)

    fun previous() = navigate(-1)

    /** Enter a folder. Called when tapping a folder item. */
    fun openFolder(folder: StoryItem) {
        if (!folder.isFolder || localFolderRepository == null) return
        viewModelScope.launch {
            localFolderRepository!!.navigateInto(folder)
            reloadStories()
        }
    }

    /** Exit current folder and go back to parent. */
    fun goBack() {
        if (localFolderRepository == null) return
        viewModelScope.launch {
            localFolderRepository!!.navigateBack()
            reloadStories()
        }
    }

    /** Reload stories from current folder. */
    private suspend fun reloadStories() {
        val repo = repository ?: return
        val stories = repo.list()
        val canNavigateBack = localFolderRepository?.canNavigateBack() ?: false
        val index = 0  // Always start at first item when entering a folder
        preparedIndex = null  // Stale - belongs to the previous folder's list
        _uiState.update { it.copy(stories = stories, currentIndex = index, canNavigateBack = canNavigateBack) }
        // Don't prepare playback automatically - wait for user to tap the item (onTogglePlayPause)
        // This avoids slow player.prepare() blocking the UI when entering a folder with many files
    }

    private fun navigate(delta: Int) {
        val state = _uiState.value
        if (state.stories.isEmpty()) return
        // Wrap-around at list ends.
        val newIndex = (state.currentIndex + delta + state.stories.size) % state.stories.size
        _uiState.update { it.copy(currentIndex = newIndex) }
        viewModelScope.launch { preparePlayback(newIndex) }
    }

    private suspend fun preparePlayback(index: Int) {
        val repo = repository ?: return
        val item = _uiState.value.stories.getOrNull(index) ?: return
        // Skip playback if this is a folder
        if (item.isFolder) return
        try {
            val uri = repo.resolvePlayableUri(item)
            _uiState.update { it.copy(unavailableMessage = null) }
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            // Wait for a tap on the cover image (onTogglePlayPause) instead of auto-playing.
            player.playWhenReady = false
            preparedIndex = index
        } catch (e: Exception) {
            // Offline and not cached yet (GitHubContentRepository) - friendly fallback state.
            preparedIndex = null
            _uiState.update { it.copy(unavailableMessage = "'${item.title}' unavailable offline") }
        }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(noisyReceiver)
        player.release()
        super.onCleared()
    }

    /** Extract LocalFolderRepository from repository, unwrapping FilteredStoryRepository if needed. */
    private fun extractLocalFolderRepository(repository: StoryRepository): LocalFolderRepository? {
        return when (repository) {
            is LocalFolderRepository -> repository
            is FilteredStoryRepository -> extractLocalFolderRepository(repository.delegate)
            else -> null
        }
    }
}
