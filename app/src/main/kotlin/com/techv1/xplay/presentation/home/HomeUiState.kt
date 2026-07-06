package com.techv1.xplay.presentation.home

import com.techv1.xplay.domain.model.Category
import com.techv1.xplay.domain.model.Video

sealed interface HomeUiState {
    object Loading : HomeUiState
    
    data class Success(
        val heroVideos: List<Video>,
        val continueWatching: List<Video>,
        val trending: List<Video>,
        val recommendations: List<Video>,
        val categories: List<Category>
    ) : HomeUiState
    
    data class Error(val message: String) : HomeUiState
}
