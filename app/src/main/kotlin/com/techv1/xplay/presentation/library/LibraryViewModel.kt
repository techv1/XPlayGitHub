package com.techv1.xplay.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        observeLibraryData()
    }

    private fun observeLibraryData() {
        _state.value = LibraryUiState.Loading
        combine(
            repository.getFavorites(),
            repository.getWatchHistory()
        ) { favorites, watchHistory ->
            LibraryUiState.Success(
                favorites = favorites,
                watchHistory = watchHistory
            )
        }.onEach { combinedState ->
            _state.value = combinedState
        }.launchIn(viewModelScope)
    }

    fun removeHistoryItem(videoId: String) {
        viewModelScope.launch {
            repository.deleteHistoryItem(videoId)
        }
    }

    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }
}
