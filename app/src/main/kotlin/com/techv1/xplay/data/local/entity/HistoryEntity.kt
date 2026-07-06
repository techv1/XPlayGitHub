package com.techv1.xplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.techv1.xplay.domain.model.Video

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val backdropUrl: String,
    val streamUrl: String,
    val durationSeconds: Int,
    val qualityLabel: String,
    val genres: String, // comma-separated values
    val year: Int,
    val rating: Float,
    val watchProgressSeconds: Int,
    val lastWatchedTimestamp: Long
) {
    fun toDomain(isFavorite: Boolean) = Video(
        id = videoId,
        title = title,
        description = description,
        thumbnailUrl = thumbnailUrl,
        backdropUrl = backdropUrl,
        streamUrl = streamUrl,
        durationSeconds = durationSeconds,
        qualityLabel = qualityLabel,
        genres = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        year = year,
        rating = rating,
        isFavorite = isFavorite,
        watchProgressSeconds = watchProgressSeconds
    )
}
