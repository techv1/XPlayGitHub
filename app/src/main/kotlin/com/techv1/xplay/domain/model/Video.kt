package com.techv1.xplay.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val backdropUrl: String,
    val streamUrl: String,          // direct playable URL from backend
    val durationSeconds: Int,
    val qualityLabel: String,       // e.g. "4K", "1080p", "720p"
    val genres: List<String>,
    val year: Int,
    val rating: Float,              // 0.0 – 10.0
    val isFavorite: Boolean = false,
    val watchProgressSeconds: Int = 0
)

@Immutable
data class Category(
    val id: String,
    val name: String,
    val videos: List<Video>
)

@Immutable
data class Recommendation(
    val video: Video,
    val reason: RecommendationReason
)

enum class RecommendationReason {
    CONTINUE_WATCHING,
    BECAUSE_YOU_WATCHED,
    TRENDING,
    SIMILAR_GENRE,
    SERVER_SUGGESTED
}
