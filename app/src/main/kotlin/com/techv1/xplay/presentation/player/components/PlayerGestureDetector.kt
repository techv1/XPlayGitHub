package com.techv1.xplay.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DRAG_SENSITIVITY = 0.003f

@Composable
fun PlayerGestureDetector(
    onToggleControls: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var verticalFeedback by remember { mutableStateOf<VerticalFeedback?>(null) }
    var dragSide by remember { mutableStateOf<DragSide?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    coroutineScope {
                        launch {
                            detectTapGestures(
                                onTap = { onToggleControls() },
                                onDoubleTap = { offset ->
                                    when {
                                        offset.x < size.width * 0.33f -> {
                                            onSeekBackward()
                                            seekFeedback = SeekFeedback.Backward
                                        }
                                        offset.x > size.width * 0.66f -> {
                                            onSeekForward()
                                            seekFeedback = SeekFeedback.Forward
                                        }
                                        else -> onToggleControls()
                                    }
                                }
                            )
                        }
                        launch {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    dragSide = if (offset.x < size.width / 2f) {
                                        DragSide.Left
                                    } else {
                                        DragSide.Right
                                    }
                                    verticalFeedback = if (dragSide == DragSide.Left) {
                                        VerticalFeedback.Brightness
                                    } else {
                                        VerticalFeedback.Volume
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = -dragAmount * DRAG_SENSITIVITY
                                    when (dragSide) {
                                        DragSide.Left -> onBrightnessChange(delta)
                                        DragSide.Right -> onVolumeChange(delta)
                                        null -> { }
                                    }
                                },
                                onDragEnd = {
                                    dragSide = null
                                },
                                onDragCancel = {
                                    dragSide = null
                                }
                            )
                        }
                    }
                }
        )

        seekFeedback?.let { feedback ->
            SeekFeedbackOverlay(
                feedback = feedback,
                onDismiss = { seekFeedback = null }
            )
        }

        verticalFeedback?.let { feedback ->
            VerticalFeedbackOverlay(
                feedback = feedback,
                onDismiss = { verticalFeedback = null }
            )
        }
    }
}

private enum class DragSide { Left, Right }
private enum class SeekFeedback { Forward, Backward }
private enum class VerticalFeedback { Volume, Brightness }

@Composable
private fun SeekFeedbackOverlay(
    feedback: SeekFeedback,
    onDismiss: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(feedback) {
        delay(600)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (feedback == SeekFeedback.Forward) "+10s" else "-10s",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun VerticalFeedbackOverlay(
    feedback: VerticalFeedback,
    onDismiss: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(feedback) {
        delay(600)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (feedback == VerticalFeedback.Volume) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = if (feedback == VerticalFeedback.Volume) "Volume" else "Brightness",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
