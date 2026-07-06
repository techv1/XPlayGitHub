package com.techv1.xplay.data.repository

import com.techv1.xplay.data.local.dao.VideoDao
import com.techv1.xplay.data.local.entity.FavoriteEntity
import com.techv1.xplay.data.local.entity.HistoryEntity
import com.techv1.xplay.data.remote.StreamtapeExtractor
import com.techv1.xplay.data.remote.XPlayApiService
import com.techv1.xplay.domain.model.Category
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.domain.repository.VideoRepository
import com.techv1.xplay.domain.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val api: XPlayApiService,
    private val dao: VideoDao
) : VideoRepository {

    override fun getHomeFeed(): Flow<Resource<List<Category>>> = flow {
        emit(Resource.Loading())
        try {
            val remoteFeed = api.getHomeFeed()
            val categories = remoteFeed.map { categoryDto ->
                val videos = categoryDto.videos.map { videoDto ->
                    val isFav = dao.isFavoriteDirect(videoDto.id)
                    val historyItem = dao.getHistoryItem(videoDto.id)
                    videoDto.toDomain().copy(
                        isFavorite = isFav,
                        watchProgressSeconds = historyItem?.watchProgressSeconds ?: 0
                    )
                }
                Category(
                    id = categoryDto.id,
                    name = categoryDto.name,
                    videos = videos
                )
            }
            emit(Resource.Success(categories))
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        } catch (e: IOException) {
            emit(Resource.Error("Couldn't reach server. Check your internet connection."))
        }
    }

    override fun getVideoDetails(id: String): Flow<Resource<Video>> = flow {
        emit(Resource.Loading())
        try {
            val remoteVideo = api.getVideoDetails(id)
            val isFav = dao.isFavoriteDirect(id)
            val historyItem = dao.getHistoryItem(id)
            val video = remoteVideo.toDomain().copy(
                isFavorite = isFav,
                watchProgressSeconds = historyItem?.watchProgressSeconds ?: 0
            )
            emit(Resource.Success(video))
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        } catch (e: IOException) {
            emit(Resource.Error("Couldn't reach server. Check your internet connection."))
        }
    }

    override fun search(query: String, page: Int): Flow<Resource<List<Video>>> = flow {
        emit(Resource.Loading())
        try {
            val remoteResults = api.search(query, page)
            val videos = remoteResults.map { videoDto ->
                val isFav = dao.isFavoriteDirect(videoDto.id)
                val historyItem = dao.getHistoryItem(videoDto.id)
                videoDto.toDomain().copy(
                    isFavorite = isFav,
                    watchProgressSeconds = historyItem?.watchProgressSeconds ?: 0
                )
            }
            emit(Resource.Success(videos))
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        } catch (e: IOException) {
            emit(Resource.Error("Couldn't reach server. Check your internet connection."))
        }
    }

    override fun getRecommendations(userId: String, genres: List<String>): Flow<Resource<List<Video>>> = flow {
        emit(Resource.Loading())
        try {
            val genresString = genres.joinToString(",")
            val remoteRecs = api.getRecommendations(userId, genresString)
            val videos = remoteRecs.map { videoDto ->
                val isFav = dao.isFavoriteDirect(videoDto.id)
                val historyItem = dao.getHistoryItem(videoDto.id)
                videoDto.toDomain().copy(
                    isFavorite = isFav,
                    watchProgressSeconds = historyItem?.watchProgressSeconds ?: 0
                )
            }
            emit(Resource.Success(videos))
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        } catch (e: IOException) {
            emit(Resource.Error("Couldn't reach server. Check your internet connection."))
        }
    }

    override fun getTrending(): Flow<Resource<List<Video>>> = flow {
        emit(Resource.Loading())
        try {
            val remoteTrending = api.getTrending()
            val videos = remoteTrending.map { videoDto ->
                val isFav = dao.isFavoriteDirect(videoDto.id)
                val historyItem = dao.getHistoryItem(videoDto.id)
                videoDto.toDomain().copy(
                    isFavorite = isFav,
                    watchProgressSeconds = historyItem?.watchProgressSeconds ?: 0
                )
            }
            emit(Resource.Success(videos))
        } catch (e: HttpException) {
            emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
        } catch (e: IOException) {
            emit(Resource.Error("Couldn't reach server. Check your internet connection."))
        }
    }

    override suspend fun resolveDirectPlaybackUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        StreamtapeExtractor.getDlLink("https://streamtape.com/v/$videoId")
    }

    override fun getFavorites(): Flow<List<Video>> {
        return dao.getFavorites().map { list ->
            list.map { favoriteEntity ->
                val historyItem = dao.getHistoryItem(favoriteEntity.videoId)
                favoriteEntity.toDomain(historyItem?.watchProgressSeconds ?: 0)
            }
        }
    }

    override suspend fun toggleFavorite(video: Video) {
        val isFav = dao.isFavoriteDirect(video.id)
        if (isFav) {
            dao.deleteFavoriteById(video.id)
        } else {
            dao.insertFavorite(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    description = video.description,
                    thumbnailUrl = video.thumbnailUrl,
                    backdropUrl = video.backdropUrl,
                    streamUrl = video.streamUrl,
                    durationSeconds = video.durationSeconds,
                    qualityLabel = video.qualityLabel,
                    genres = video.genres.joinToString(","),
                    year = video.year,
                    rating = video.rating,
                    timestampAdded = System.currentTimeMillis()
                )
            )
        }
    }

    override fun getWatchHistory(): Flow<List<Video>> {
        return dao.getWatchHistory().map { list ->
            list.map { historyEntity ->
                val isFav = dao.isFavoriteDirect(historyEntity.videoId)
                historyEntity.toDomain(isFav)
            }
        }
    }

    override suspend fun updateWatchProgress(video: Video, progressSeconds: Int) {
        dao.insertHistory(
            HistoryEntity(
                videoId = video.id,
                title = video.title,
                description = video.description,
                thumbnailUrl = video.thumbnailUrl,
                backdropUrl = video.backdropUrl,
                streamUrl = video.streamUrl,
                durationSeconds = video.durationSeconds,
                qualityLabel = video.qualityLabel,
                genres = video.genres.joinToString(","),
                year = video.year,
                rating = video.rating,
                watchProgressSeconds = progressSeconds,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteHistoryItem(videoId: String) {
        dao.deleteHistoryById(videoId)
    }
}
