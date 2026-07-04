package com.techv1.xplay.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.techv1.xplay.domain.model.Video

data class VideoDto(
    @SerializedName("id")           val id: String,
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String,
    @SerializedName("thumbnail_url")val thumbnailUrl: String,
    @SerializedName("backdrop_url") val backdropUrl: String,
    @SerializedName("stream_url")   val streamUrl: String,
    @SerializedName("duration_sec") val durationSeconds: Int,
    @SerializedName("quality")      val qualityLabel: String,
    @SerializedName("genres")       val genres: List<String>,
    @SerializedName("year")         val year: Int,
    @SerializedName("rating")       val rating: Float
) {
    fun toDomain() = Video(
        id             = id,
        title          = title,
        description    = description,
        thumbnailUrl   = thumbnailUrl,
        backdropUrl    = backdropUrl,
        streamUrl      = streamUrl,
        durationSeconds = durationSeconds,
        qualityLabel   = qualityLabel,
        genres         = genres,
        year           = year,
        rating         = rating
    )
}

data class CategoryDto(
    @SerializedName("id")     val id: String,
    @SerializedName("name")   val name: String,
    @SerializedName("videos") val videos: List<VideoDto>
)
