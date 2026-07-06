package com.techv1.xplay.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techv1.xplay.domain.repository.VideoRepository
import com.techv1.xplay.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _genreFilter = MutableStateFlow<String?>(null)
    val genreFilter = _genreFilter.asStateFlow()

    val state: StateFlow<SearchUiState> = _searchQuery
        .debounce(400)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchUiState.Idle)
            } else {
                repository.search(query).map { resource ->
                    when (resource) {
                        is Resource.Loading -> SearchUiState.Loading
                        is Resource.Error -> SearchUiState.Error(resource.message ?: "An error occurred")
                        is Resource.Success -> {
                            val results = resource.data ?: emptyList()
                            SearchUiState.Success(results)
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState.Idle
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String?) {
        _genreFilter.value = genre
        if (genre != null) {
            _searchQuery.value = genre
        } else {
            _searchQuery.value = ""
        }
    }
}
