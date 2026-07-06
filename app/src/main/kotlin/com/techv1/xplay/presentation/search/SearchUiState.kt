package com.techv1.xplay.presentation.search

import com.techv1.xplay.domain.model.Video

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<Video>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
