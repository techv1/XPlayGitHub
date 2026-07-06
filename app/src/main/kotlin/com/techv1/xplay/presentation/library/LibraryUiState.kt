package com.techv1.xplay.presentation.library

import com.techv1.xplay.domain.model.Video

sealed interface LibraryUiState {
    object Loading : LibraryUiState
    
    data class Success(
        val favorites: List<Video>,
        val watchHistory: List<Video>
    ) : LibraryUiState
    
    data class Error(val message: String) : LibraryUiState
}
