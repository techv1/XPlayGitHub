@file:OptIn(ExperimentalMaterial3Api::class)

package com.techv1.xplay.presentation.home

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.techv1.xplay.domain.model.Video
import com.techv1.xplay.presentation.components.pressClickable
import com.techv1.xplay.presentation.components.shimmerBrush
import com.techv1.xplay.presentation.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadHomeFeed()
            delay(1000)
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val homeState = state) {
            is HomeUiState.Loading -> {
                HomeSkeleton()
            }
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = homeState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            is HomeUiState.Success -> {
                HomeScreenContent(
                    state = homeState,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun HomeScreenContent(
    state: HomeUiState.Success,
    navController: NavController
) {
    val allVideos = remember(state) {
        val list = mutableListOf<Video>()
        list.addAll(state.continueWatching)
        list.addAll(state.recommendations)
        state.categories.forEach { category ->
            list.addAll(category.videos)
        }
        list.distinctBy { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Top Header ──
        item(contentType = "header") {
            Header()
        }

        // ── Hero Banner Carousel ──
        if (state.heroVideos.isNotEmpty()) {
            item(contentType = "hero") {
                HeroCarousel(
                    videos = state.heroVideos,
                    onPlayClick = { video ->
                        navController.navigate(Screen.Watch.createRoute(video.id))
                    },
                    onDetailsClick = { video ->
                        navController.navigate(Screen.Watch.createRoute(video.id))
                    }
                )
            }
        }

        // ── Blog Type Scroll Feed ──
        items(
            allVideos,
            key = { it.id },
            contentType = { "video" }
        ) { video ->
            BlogVideoCard(
                video = video,
                onClick = {
                    navController.navigate(Screen.Watch.createRoute(video.id))
                }
            )
        }
    }
}

@Composable
private fun BlogVideoCard(
    video: Video,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .pressClickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F1F23) // Obsidian Vault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Video Thumbnail Banner (1.85:1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.85f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.backdropUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Quality Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.qualityLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Video Info Metadata
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Star rating display
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", video.rating),
                            color = Color(0xFFFFB300),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Genres tags list and release year
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.year.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "•", color = Color.White.copy(alpha = 0.3f))
                    video.genres.take(2).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = genre,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Description plot block
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "XPlay",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary, // Netflix Red
            fontWeight = FontWeight.Black
        )
        // Profile Icon with custom styling
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuATgoJjsWMGmL8A03_2IzODzinJd9Orp2oXUPBORKnL0_UyhGylySBup8jSQrCy6H6e7_rFweNdQ5lXVZ7xxBMKKLktJf7qnOSiJKTRghR803DHyDgX12553GpH0PkKgFQtTLGRXx-3XIrRihIMxKpRl3PmHMlZZNXuGb2AwMuRCKFwTypR18_4fmNjglAHAC_TIBsMQcJ_2-WXshChvcfsyyhACjm1IOV6nb8HXABO8tlgTDYkzlRbHw",
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    videos: List<Video>,
    onPlayClick: (Video) -> Unit,
    onDetailsClick: (Video) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { videos.size })
    var glowColor by remember { mutableStateOf(Color.Transparent) }
    val animatedGlowColor by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(durationMillis = 800),
        label = "GlowColor"
    )

    val context = LocalContext.current

    // Auto-advance pager
    LaunchedEffect(key1 = pagerState.currentPage) {
        delay(6000)
        val nextPage = (pagerState.currentPage + 1) % videos.size
        pagerState.animateScrollToPage(nextPage)
    }

    // Dynamic Color extraction from current page thumbnail
    LaunchedEffect(key1 = pagerState.currentPage) {
        val currentVideo = videos[pagerState.currentPage]
        val imageLoader = Coil.imageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(currentVideo.backdropUrl)
            .allowHardware(false)
            .build()
        
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    val color = palette?.getDarkVibrantColor(0x00000000) ?: 0
                    glowColor = if (color != 0) Color(color).copy(alpha = 0.35f) else Color.Transparent
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Ambient background glow (Dynamic color tinting)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(32.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(animatedGlowColor, Color.Transparent),
                        radius = 600f
                    )
                )
        )

        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val video = videos[page]
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(video.backdropUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradient scrim overlaid on bottom 50%
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0B0B0F))
                            )
                        )
                )

                // Carousel details Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    val currentVideo = videos[pagerState.currentPage]
                    Text(
                        text = currentVideo.title,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentVideo.genres.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onPlayClick(currentVideo) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary, // Netflix Red
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            modifier = Modifier.pressClickable { onPlayClick(currentVideo) }
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { onDetailsClick(currentVideo) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier.pressClickable { onDetailsClick(currentVideo) }
                        ) {
                            Text(text = "More Info", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                // Dot Page Indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(videos.size) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                                .background(
                                    color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoShelf(
    title: String,
    videos: List<Video>,
    showProgress: Boolean = false,
    onVideoClick: (Video) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressClickable { onVideoClick(video) }
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Progress bar overlay (e.g. Continue Watching)
                    if (showProgress && video.durationSeconds > 0) {
                        val progressFraction = video.watchProgressSeconds.toFloat() / video.durationSeconds.toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 36.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(shimmerBrush())
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush())
            )
        }

        // Hero Carousel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(shimmerBrush())
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Shelf Title
        Box(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                .size(width = 180.dp, height = 24.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(shimmerBrush())
        )

        // Shelf Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false
        ) {
            items(5) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.large)
                        .background(shimmerBrush())
                )
            }
        }
    }
}
