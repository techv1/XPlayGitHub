package com.techv1.xplay.presentation.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.domain.repository.VideoRepository
import com.techv1.xplay.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnifiedPlayerViewModel @Inject constructor(
    application: Application,
    private val repository: VideoRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val videoId: String = checkNotNull(savedStateHandle["videoId"])

    private val _state = MutableStateFlow<UnifiedPlayerUiState>(UnifiedPlayerUiState.Loading)
    val state: StateFlow<UnifiedPlayerUiState> = _state.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _areControlsVisible = MutableStateFlow(true)
    val areControlsVisible: StateFlow<Boolean> = _areControlsVisible.asStateFlow()

    private var progressJob: Job? = null
    private var controlsHideJob: Job? = null
    private var lastSavedProgressSeconds = -1

    init {
        loadVideo()
    }

    private fun loadVideo() {
        viewModelScope.launch {
            _state.value = UnifiedPlayerUiState.Loading
            try {
                val resource = repository.getVideoDetails(videoId)
                    .first { it is Resource.Success || it is Resource.Error }

                when (resource) {
                    is Resource.Error -> {
                        _state.value = UnifiedPlayerUiState.Error(
                            resource.message ?: "An unexpected error occurred"
                        )
                    }
                    is Resource.Success -> {
                        val video = resource.data
                        if (video == null) {
                            _state.value = UnifiedPlayerUiState.Error("Video not found")
                            return@launch
                        }
                        val isFav = repository.getFavorites().first().any { it.id == video.id }
                        val progress = repository.getWatchHistory().first()
                            .find { it.id == video.id }?.watchProgressSeconds ?: 0
                        val mappedVideo = video.copy(
                            isFavorite = isFav,
                            watchProgressSeconds = progress
                        )
                        val recs = loadRecommendations(mappedVideo)

                        _state.value = UnifiedPlayerUiState.Success(
                            video = mappedVideo,
                            recommendations = recs,
                            isFavorite = isFav,
                            isFullscreen = false,
                            areControlsVisible = true
                        )

                        preparePlayer(mappedVideo.id, progress)
                        resetControlsHideTimer()
                    }
                    is Resource.Loading -> {
                        // Filtered out by first { ... } but required for exhaustive when.
                    }
                }
            } catch (e: Exception) {
                _state.value = UnifiedPlayerUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    private suspend fun loadRecommendations(video: Video): List<Video> {
        val recsResult = repository.getRecommendations("user_123", video.genres)
            .first { it is Resource.Success || it is Resource.Error }

        val recs = (recsResult as? Resource.Success)?.data
            ?.filter { it.id != video.id }
            ?: emptyList()
        if (recs.isNotEmpty()) return recs

        val trendingResult = repository.getTrending()
            .first { it is Resource.Success || it is Resource.Error }

        return ((trendingResult as? Resource.Success)?.data ?: emptyList())
            .filter { it.id != video.id }
            .take(RECOMMENDATION_LIMIT)
    }

    private fun preparePlayer(videoId: String, startPositionSeconds: Int) {
        viewModelScope.launch {
            val url = repository.resolveDirectPlaybackUrl(videoId)
            if (url.isNullOrBlank()) {
                _state.value = UnifiedPlayerUiState.Error("Could not resolve playback URL")
                return@launch
            }

            val exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                seekTo(startPositionSeconds * 1000L)
                prepare()
                playWhenReady = true
                addListener(playerListener)
            }
            _player.value = exoPlayer
            startProgressPolling()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePlaybackState()
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackState()
                saveProgressIfNeeded()
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
        }
    }

    private fun updatePlaybackState() {
        val exo = _player.value ?: return
        _playbackState.update {
            it.copy(
                isPlaying = exo.isPlaying,
                positionMs = exo.currentPosition.coerceAtLeast(0),
                durationMs = exo.duration.coerceAtLeast(0),
                bufferedPositionMs = exo.bufferedPosition.coerceAtLeast(0),
                playbackSpeed = exo.playbackParameters.speed
            )
        }
    }

    private fun saveProgressIfNeeded() {
        val video = (_state.value as? UnifiedPlayerUiState.Success)?.video ?: return
        val exo = _player.value ?: return
        val progressSeconds = (exo.currentPosition / 1000).toInt()
        if (progressSeconds > 0 && progressSeconds != lastSavedProgressSeconds) {
            viewModelScope.launch {
                repository.updateWatchProgress(video, progressSeconds)
            }
            lastSavedProgressSeconds = progressSeconds
        }
    }

    fun togglePlayPause() {
        _player.value?.let { player ->
            player.playWhenReady = !player.playWhenReady
        }
    }

    fun seekTo(positionMs: Long) {
        val player = _player.value ?: return
        val duration = _playbackState.value.durationMs
        player.seekTo(positionMs.coerceIn(0, duration))
        updatePlaybackState()
    }

    fun seekBackward(millis: Long = SEEK_OFFSET_MS) {
        seekTo(_playbackState.value.positionMs - millis)
    }

    fun seekForward(millis: Long = SEEK_OFFSET_MS) {
        seekTo(_playbackState.value.positionMs + millis)
    }

    fun setPlaybackSpeed(speed: Float) {
        _player.value?.setPlaybackSpeed(speed)
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun onUserInteraction() {
        _areControlsVisible.value = true
        resetControlsHideTimer()
    }

    fun hideControls() {
        _areControlsVisible.value = false
        controlsHideJob?.cancel()
    }

    private fun resetControlsHideTimer() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(CONTROLS_AUTO_HIDE_DELAY_MS)
            _areControlsVisible.value = false
        }
    }

    fun toggleFavorite() {
        val current = (_state.value as? UnifiedPlayerUiState.Success)?.video ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
            val isFav = repository.getFavorites().first().any { it.id == current.id }
            _state.update { state ->
                if (state is UnifiedPlayerUiState.Success) {
                    state.copy(
                        video = state.video.copy(isFavorite = isFav),
                        isFavorite = isFav
                    )
                } else state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgressIfNeeded()
        controlsHideJob?.cancel()
        progressJob?.cancel()
        _player.value?.removeListener(playerListener)
        _player.value?.release()
        _player.value = null
    }

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val bufferedPositionMs: Long = 0L,
        val playbackSpeed: Float = 1f
    )

    companion object {
        private const val PROGRESS_POLL_INTERVAL_MS = 200L
        private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3000L
        private const val SEEK_OFFSET_MS = 10_000L
        private const val RECOMMENDATION_LIMIT = 10
    }
}
