package com.techv1.xplay.presentation.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.presentation.components.pressClickable
import com.techv1.xplay.presentation.player.components.PlayerControlsOverlay
import com.techv1.xplay.presentation.player.components.PlayerGestureDetector
import com.techv1.xplay.presentation.player.components.TrackSelectorBottomSheet
import com.techv1.xplay.ui.theme.BackgroundCinema

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun UnifiedPlayerScreen(
    navController: NavController,
    viewModel: UnifiedPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val player by viewModel.player.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val areControlsVisible by viewModel.areControlsVisible.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    DisposableEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
        window?.let { w ->
            val controller = WindowInsetsControllerCompat(w, w.decorView)
            if (isFullscreen) {
                WindowCompat.setDecorFitsSystemWindows(w, false)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                WindowCompat.setDecorFitsSystemWindows(w, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window?.let { w ->
                WindowCompat.setDecorFitsSystemWindows(w, true)
                val controller = WindowInsetsControllerCompat(w, w.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        viewModel.toggleFullscreen()
    }

    var showTrackSelector by remember { mutableStateOf(false) }
    var showSpeedSelector by remember { mutableStateOf(false) }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    var currentVolume by remember(audioManager) {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    val onVolumeChange: (Float) -> Unit = { delta ->
        if (maxVolume > 0) {
            val newVolume = (currentVolume + delta * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            currentVolume = newVolume
        }
    }

    val onBrightnessChange: (Float) -> Unit = { delta ->
        window?.attributes?.let { layoutParams ->
            val newBrightness = (layoutParams.screenBrightness + delta).coerceIn(0.1f, 1f)
            layoutParams.screenBrightness = newBrightness
            window.attributes = layoutParams
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCinema)
    ) {
        when (val currentState = state) {
            is UnifiedPlayerUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is UnifiedPlayerUiState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is UnifiedPlayerUiState.Success -> {
                if (isFullscreen) {
                    LandscapePlayerLayout(
                        video = currentState.video,
                        player = player,
                        playbackState = playbackState,
                        areControlsVisible = areControlsVisible,
                        viewModel = viewModel,
                        onVolumeChange = onVolumeChange,
                        onBrightnessChange = onBrightnessChange,
                        onShowTrackSelector = { showTrackSelector = true },
                        onShowSpeedSelector = { showSpeedSelector = true }
                    )
                } else {
                    PortraitPlayerLayout(
                        video = currentState.video,
                        recommendations = currentState.recommendations,
                        isFavorite = currentState.isFavorite,
                        player = player,
                        playbackState = playbackState,
                        areControlsVisible = areControlsVisible,
                        viewModel = viewModel,
                        navController = navController,
                        onVolumeChange = onVolumeChange,
                        onBrightnessChange = onBrightnessChange,
                        onShowTrackSelector = { showTrackSelector = true },
                        onShowSpeedSelector = { showSpeedSelector = true }
                    )
                }

                TrackSelectorBottomSheet(
                    player = player,
                    isVisible = showTrackSelector,
                    onDismiss = { showTrackSelector = false }
                )

                SpeedSelectorBottomSheet(
                    currentSpeed = playbackState.playbackSpeed,
                    isVisible = showSpeedSelector,
                    onSpeedSelected = {
                        viewModel.setPlaybackSpeed(it)
                        showSpeedSelector = false
                    },
                    onDismiss = { showSpeedSelector = false }
                )
            }
        }
    }
}

@Composable
private fun LandscapePlayerLayout(
    video: Video,
    player: androidx.media3.common.Player?,
    playbackState: UnifiedPlayerViewModel.PlaybackState,
    areControlsVisible: Boolean,
    viewModel: UnifiedPlayerViewModel,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onShowTrackSelector: () -> Unit,
    onShowSpeedSelector: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PlayerGestureDetector(
            onToggleControls = { viewModel.onUserInteraction() },
            onSeekForward = { viewModel.seekForward() },
            onSeekBackward = { viewModel.seekBackward() },
            onVolumeChange = onVolumeChange,
            onBrightnessChange = onBrightnessChange,
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerSurface(player = player)
        }

        PlayerControlsOverlay(
            isVisible = areControlsVisible,
            title = video.title,
            isPlaying = playbackState.isPlaying,
            isFullscreen = true,
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            bufferedPositionMs = playbackState.bufferedPositionMs,
            playbackSpeed = playbackState.playbackSpeed,
            onBackClick = { viewModel.toggleFullscreen() },
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seekTo(it) },
            onToggleFullscreen = { viewModel.toggleFullscreen() },
            onTrackSelectorClick = onShowTrackSelector,
            onSpeedClick = onShowSpeedSelector
        )
    }
}

@Composable
private fun PortraitPlayerLayout(
    video: Video,
    recommendations: List<Video>,
    isFavorite: Boolean,
    player: androidx.media3.common.Player?,
    playbackState: UnifiedPlayerViewModel.PlaybackState,
    areControlsVisible: Boolean,
    viewModel: UnifiedPlayerViewModel,
    navController: NavController,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onShowTrackSelector: () -> Unit,
    onShowSpeedSelector: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.40f)
        ) {
            PlayerGestureDetector(
                onToggleControls = { viewModel.onUserInteraction() },
                onSeekForward = { viewModel.seekForward() },
                onSeekBackward = { viewModel.seekBackward() },
                onVolumeChange = onVolumeChange,
                onBrightnessChange = onBrightnessChange,
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerSurface(player = player)
            }

            PlayerControlsOverlay(
                isVisible = areControlsVisible,
                title = video.title,
                isPlaying = playbackState.isPlaying,
                isFullscreen = false,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                bufferedPositionMs = playbackState.bufferedPositionMs,
                playbackSpeed = playbackState.playbackSpeed,
                onBackClick = { navController.popBackStack() },
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onToggleFullscreen = { viewModel.toggleFullscreen() },
                onTrackSelectorClick = onShowTrackSelector,
                onSpeedClick = onShowSpeedSelector
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(contentType = "metadata") {
                MetadataSection(
                    video = video,
                    isFavorite = isFavorite,
                    viewModel = viewModel,
                    navController = navController
                )
            }

            if (recommendations.isNotEmpty()) {
                item(contentType = "section_header") {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(
                    recommendations,
                    key = { it.id },
                    contentType = { "recommendation" }
                ) { recVideo ->
                    RecommendationCard(
                        video = recVideo,
                        onClick = {
                            navController.navigate(
                                com.techv1.xplay.presentation.navigation.Screen.Watch.createRoute(recVideo.id)
                            ) {
                                launchSingleTop = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSurface(
    player: androidx.media3.common.Player?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                useController = false
            }
        },
        update = { playerView ->
            if (playerView.player != player) {
                playerView.player = player
            }
        }
    )
}

@Composable
private fun MetadataSection(
    video: Video,
    isFavorite: Boolean,
    viewModel: UnifiedPlayerViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${video.year} • ${video.qualityLabel} • ${video.genres.joinToString(", ")}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { viewModel.toggleFavorite() },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = if (isFavorite) "In Watchlist" else "Watchlist")
            }

            OutlinedButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://streamtape.com/v/${video.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = "Share")
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    video: Video,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pressClickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.thumbnailUrl)
                    .crossfade(false)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectorBottomSheet(
    currentSpeed: Float,
    isVisible: Boolean,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.techv1.xplay.ui.theme.BackgroundSurface
    ) {
        Text(
            text = "Playback speed",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        PLAYBACK_SPEEDS.forEach { speed ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSpeedSelected(speed) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = speed == currentSpeed,
                    onClick = { onSpeedSelected(speed) }
                )
                Text(
                    text = "${speed}x",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
