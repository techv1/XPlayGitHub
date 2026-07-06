package com.techv1.xplay.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.domain.repository.VideoRepository
import com.techv1.xplay.domain.util.Resource
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
class HomeViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            
            combine(
                repository.getHomeFeed(),
                repository.getTrending(),
                repository.getWatchHistory(),
                repository.getFavorites()
            ) { homeFeedRes, trendingRes, watchHistory, favorites ->
                if (homeFeedRes is Resource.Error) {
                    return@combine HomeUiState.Error(homeFeedRes.message ?: "An error occurred")
                }
                if (trendingRes is Resource.Error) {
                    return@combine HomeUiState.Error(trendingRes.message ?: "An error occurred")
                }
                
                if (homeFeedRes is Resource.Loading || trendingRes is Resource.Loading) {
                    return@combine HomeUiState.Loading
                }
                
                val categories = homeFeedRes.data ?: emptyList()
                val trending = trendingRes.data ?: emptyList()
                
                val heroVideos = trending.take(5)
                val continueWatching = watchHistory.filter { it.watchProgressSeconds > 0 }
                
                val userGenres = watchHistory.flatMap { it.genres }.distinct()
                val recommendations = if (userGenres.isNotEmpty()) {
                    categories.flatMap { it.videos }
                        .filter { video -> video.genres.any { it in userGenres } }
                        .distinctBy { it.id }
                        .take(10)
                } else {
                    categories.flatMap { it.videos }.distinctBy { it.id }.take(10)
                }
                
                // Map local favorite/progress state dynamically
                val favoritesMap = favorites.associateBy { it.id }
                val historyMap = watchHistory.associateBy { it.id }
                
                val mappedHero = heroVideos.map { 
                    it.copy(
                        isFavorite = favoritesMap.containsKey(it.id),
                        watchProgressSeconds = historyMap[it.id]?.watchProgressSeconds ?: 0
                    )
                }
                val mappedTrending = trending.map { 
                    it.copy(
                        isFavorite = favoritesMap.containsKey(it.id),
                        watchProgressSeconds = historyMap[it.id]?.watchProgressSeconds ?: 0
                    )
                }
                val mappedRecommendations = recommendations.map { 
                    it.copy(
                        isFavorite = favoritesMap.containsKey(it.id),
                        watchProgressSeconds = historyMap[it.id]?.watchProgressSeconds ?: 0
                    )
                }
                val mappedCategories = categories.map { category ->
                    category.copy(
                        videos = category.videos.map { 
                            it.copy(
                                isFavorite = favoritesMap.containsKey(it.id),
                                watchProgressSeconds = historyMap[it.id]?.watchProgressSeconds ?: 0
                            )
                        }
                    )
                }

                HomeUiState.Success(
                    heroVideos = mappedHero,
                    continueWatching = continueWatching,
                    trending = mappedTrending,
                    recommendations = mappedRecommendations,
                    categories = mappedCategories
                )
            }.onEach { combinedState ->
                _state.value = combinedState
            }.launchIn(viewModelScope)
        }
    }
    
    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }
}
