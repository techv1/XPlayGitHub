package com.techv1.xplay.domain.model

data class PlaybackState(
    val videoId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
}
