package com.techv1.xplay.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.techv1.xplay.ui.theme.AccentPrimary

@Composable
fun PlayerSeekbar(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeek: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableLongStateOf(0L) }

    val displayedPositionMs = if (isDragging) dragPositionMs else positionMs
    val maxMs = durationMs.coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Slider(
            value = displayedPositionMs.toFloat().coerceIn(0f, maxMs.toFloat()),
            onValueChange = {
                isDragging = true
                dragPositionMs = it.toLong().coerceIn(0, maxMs)
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(dragPositionMs)
            },
            valueRange = 0f..maxMs.coerceAtLeast(1).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AccentPrimary,
                activeTrackColor = AccentPrimary,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                disabledThumbColor = Color.White.copy(alpha = 0.38f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.38f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatElapsedTime(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = formatElapsedTime(maxMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

private fun formatElapsedTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt().coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
