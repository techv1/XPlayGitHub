package com.techv1.xplay.data.remote

import com.techv1.xplay.data.remote.dto.CategoryDto
import com.techv1.xplay.data.remote.dto.VideoDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface XPlayApiService {
    @GET("home")
    suspend fun getHomeFeed(): List<CategoryDto>

    @GET("videos/{id}")
    suspend fun getVideoDetails(@Path("id") id: String): VideoDto

    @GET("search")
    suspend fun search(@Query("q") query: String, @Query("page") page: Int = 1): List<VideoDto>

    @GET("recommendations")
    suspend fun getRecommendations(
        @Query("userId") userId: String,
        @Query("genres") genres: String
    ): List<VideoDto>

    @GET("trending")
    suspend fun getTrending(): List<VideoDto>
}
