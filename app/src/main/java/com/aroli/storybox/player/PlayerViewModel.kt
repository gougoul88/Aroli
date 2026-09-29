package com.aroli.storybox.player

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aroli.storybox.data.StoryItem
import com.aroli.storybox.data.StoryRepository
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
)

/** Wraps a single ExoPlayer instance: play/pause, wrap-around prev/next over the active repository's list. */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: StoryRepository? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // handleAudioFocus = true lets ExoPlayer duck/pause automatically on focus loss.
    private val player: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true,
        )
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
        viewModelScope.launch {
            val stories = repository.list()
            val index = startIndex.coerceIn(0, (stories.size - 1).coerceAtLeast(0))
            _uiState.update { it.copy(stories = stories, currentIndex = index) }
            if (stories.isNotEmpty()) preparePlayback(index)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() = navigate(+1)

    fun previous() = navigate(-1)

    private fun navigate(delta: Int) {
        val state = _uiState.value
        if (state.stories.isEmpty()) return
        // Wrap-around at list ends (confirmed in plan.md).
        val newIndex = (state.currentIndex + delta + state.stories.size) % state.stories.size
        _uiState.update { it.copy(currentIndex = newIndex) }
        viewModelScope.launch { preparePlayback(newIndex) }
    }

    private suspend fun preparePlayback(index: Int) {
        val repo = repository ?: return
        val item = _uiState.value.stories.getOrNull(index) ?: return
        try {
            val uri = repo.resolvePlayableUri(item)
            _uiState.update { it.copy(unavailableMessage = null) }
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            // Wait for a tap on the cover image (onTogglePlayPause) instead of auto-playing.
            player.playWhenReady = false
        } catch (e: Exception) {
            // Offline and not cached yet (GitHubContentRepository) - friendly fallback state (plan.md Phase 6).
            _uiState.update { it.copy(unavailableMessage = "'${item.title}' unavailable offline") }
        }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(noisyReceiver)
        player.release()
        super.onCleared()
    }
}
