package com.techv1.xplay.presentation.player

import com.techv1.xplay.domain.model.Video

sealed interface UnifiedPlayerUiState {
    object Loading : UnifiedPlayerUiState
    data class Error(val message: String) : UnifiedPlayerUiState
    data class Success(
        val video: Video,
        val recommendations: List<Video>,
        val isFavorite: Boolean,
        val isFullscreen: Boolean,
        val areControlsVisible: Boolean
    ) : UnifiedPlayerUiState
}
