package com.techv1.xplay.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Custom shimmer linear gradient brush matching (#16161D -> #1E1E28 -> #16161D) spec */
@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1200f
): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShimmerTranslate"
        )
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF16161D),
                Color(0xFF1E1E28),
                Color(0xFF16161D)
            ),
            start = Offset(x = translateAnimation - targetValue, y = 0f),
            end = Offset(x = translateAnimation, y = 200f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}
