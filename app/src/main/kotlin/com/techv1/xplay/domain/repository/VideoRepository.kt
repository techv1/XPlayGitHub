package com.techv1.xplay.domain.repository

import com.techv1.xplay.domain.model.Category
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getHomeFeed(): Flow<Resource<List<Category>>>
    fun getVideoDetails(id: String): Flow<Resource<Video>>
    fun search(query: String, page: Int = 1): Flow<Resource<List<Video>>>
    fun getRecommendations(userId: String, genres: List<String>): Flow<Resource<List<Video>>>
    fun getTrending(): Flow<Resource<List<Video>>>

    suspend fun resolveDirectPlaybackUrl(videoId: String): String?

    // Local / Persistence
    fun getFavorites(): Flow<List<Video>>
    suspend fun toggleFavorite(video: Video)
    fun getWatchHistory(): Flow<List<Video>>
    suspend fun updateWatchProgress(video: Video, progressSeconds: Int)
    suspend fun deleteHistoryItem(videoId: String)
}
