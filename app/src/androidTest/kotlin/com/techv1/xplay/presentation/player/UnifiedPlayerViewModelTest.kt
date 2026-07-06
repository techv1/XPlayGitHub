package com.techv1.xplay.presentation.player

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.techv1.xplay.domain.model.Category
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.domain.repository.VideoRepository
import com.techv1.xplay.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnifiedPlayerViewModelTest {

    private val testVideo = Video(
        id = "test123",
        title = "Test Video",
        description = "Description",
        thumbnailUrl = "https://example.com/thumb.jpg",
        backdropUrl = "https://example.com/backdrop.jpg",
        streamUrl = "https://example.com/stream.mp4",
        durationSeconds = 120,
        qualityLabel = "1080p",
        genres = listOf("Action"),
        year = 2024,
        rating = 8.5f
    )

    private val recVideo = Video(
        id = "rec1",
        title = "Recommended Video",
        description = "Description",
        thumbnailUrl = "https://example.com/rec_thumb.jpg",
        backdropUrl = "https://example.com/rec_backdrop.jpg",
        streamUrl = "https://example.com/rec_stream.mp4",
        durationSeconds = 90,
        qualityLabel = "720p",
        genres = listOf("Action"),
        year = 2023,
        rating = 7.5f
    )

    @Test
    fun loadVideo_success_emitsSuccessStateWithRecommendations() = runBlocking {
        val repository = FakeVideoRepository(
            video = testVideo,
            recommendations = listOf(recVideo),
            trending = emptyList()
        )
        val viewModel = createViewModel(repository)

        val state = viewModel.state.first { it is UnifiedPlayerUiState.Success }
        val success = state as UnifiedPlayerUiState.Success
        assertEquals("test123", success.video.id)
        assertEquals(1, success.recommendations.size)
        assertEquals("rec1", success.recommendations.first().id)
        assertTrue(success.recommendations.none { it.id == testVideo.id })
    }

    @Test
    fun loadVideo_emptyRecommendations_fallsBackToTrending() = runBlocking {
        val repository = FakeVideoRepository(
            video = testVideo,
            recommendations = emptyList(),
            trending = listOf(recVideo)
        )
        val viewModel = createViewModel(repository)

        val state = viewModel.state.first { it is UnifiedPlayerUiState.Success }
        val success = state as UnifiedPlayerUiState.Success
        assertEquals(1, success.recommendations.size)
        assertEquals("rec1", success.recommendations.first().id)
    }

    @Test
    fun toggleFullscreen_updatesFullscreenState() = runBlocking {
        val repository = FakeVideoRepository(
            video = testVideo,
            recommendations = listOf(recVideo),
            trending = emptyList()
        )
        val viewModel = createViewModel(repository)
        viewModel.state.first { it is UnifiedPlayerUiState.Success }

        assertEquals(false, viewModel.isFullscreen.value)
        viewModel.toggleFullscreen()
        assertEquals(true, viewModel.isFullscreen.value)
        viewModel.toggleFullscreen()
        assertEquals(false, viewModel.isFullscreen.value)
    }

    private fun createViewModel(repository: VideoRepository): UnifiedPlayerViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val savedStateHandle = SavedStateHandle().apply { set("videoId", "test123") }
        return UnifiedPlayerViewModel(application, repository, savedStateHandle)
    }

    private class FakeVideoRepository(
        private val video: Video,
        private val recommendations: List<Video>,
        private val trending: List<Video>
    ) : VideoRepository {
        override fun getHomeFeed(): Flow<Resource<List<Category>>> =
            flowOf(Resource.Success(emptyList()))

        override fun getVideoDetails(id: String): Flow<Resource<Video>> =
            flowOf(Resource.Success(video))

        override fun search(query: String, page: Int): Flow<Resource<List<Video>>> =
            flowOf(Resource.Success(emptyList()))

        override fun getRecommendations(userId: String, genres: List<String>): Flow<Resource<List<Video>>> =
            flowOf(Resource.Success(recommendations))

        override fun getTrending(): Flow<Resource<List<Video>>> =
            flowOf(Resource.Success(trending))

        override suspend fun resolveDirectPlaybackUrl(videoId: String): String? =
            "https://example.com/playback.mp4"

        override fun getFavorites(): Flow<List<Video>> = flowOf(emptyList())
        override suspend fun toggleFavorite(video: Video) {}
        override fun getWatchHistory(): Flow<List<Video>> = flowOf(emptyList())
        override suspend fun updateWatchProgress(video: Video, progressSeconds: Int) {}
        override suspend fun deleteHistoryItem(videoId: String) {}
    }
}
