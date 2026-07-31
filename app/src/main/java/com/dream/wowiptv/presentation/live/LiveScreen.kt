package com.dream.wowiptv.presentation.live

import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.model.LiveCategory
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.LiveRed

private val DarkBg = Color(0xFF1A1A1A)
private val DarkSurface = Color(0xFF242424)
private val DarkText = Color(0xFFDDDDDD)
private val DarkTextSecondary = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    pendingStreamId: Int? = null,
    onStreamPlayed: () -> Unit = {},
    onFullscreenChanged: (Boolean) -> Unit = {},
    onOpenEpg: (Int) -> Unit = {},
    viewModel: LiveViewModel = hiltViewModel()
) {
    val categoriesState by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val streamsState by viewModel.streams.collectAsState()
    val currentStream by viewModel.currentStream.collectAsState()
    val streamUrl by viewModel.streamUrl.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val epgEntries by viewModel.epgEntries.collectAsState()
    val channelEpg by viewModel.channelEpg.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()

    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(pendingStreamId, streamsState) {
        if (pendingStreamId != null) {
            val s = streamsState
            if (s is UiState.Success) {
                val stream = s.data.find { it.id == pendingStreamId }
                if (stream != null) {
                    viewModel.playStream(stream)
                    onStreamPlayed()
                }
            }
        }
    }

    LaunchedEffect(streamsState) {
        if (streamsState !is UiState.Loading) {
            isRefreshing = false
        }
    }

    LaunchedEffect(isFullscreen) {
        onFullscreenChanged(isFullscreen)
        if (activity != null) {
            val window = activity.window
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotEmpty()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            isBuffering = true
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying && streamUrl.isNotEmpty()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = when (playbackState) {
                    Player.STATE_BUFFERING -> true
                    else -> false
                }
            }
            override fun onIsLoadingChanged(isLoading: Boolean) {
                if (isLoading) isBuffering = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    if (isFullscreen) {
        FullscreenPlayerView(
            exoPlayer = exoPlayer,
            currentStream = currentStream,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            epgEntries = epgEntries,
            onBack = { viewModel.exitFullscreen() },
            onTogglePlay = { viewModel.togglePlay() },
            onOpenEpg = { currentStream?.let { onOpenEpg(it.id) } }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerSection(
                exoPlayer = exoPlayer,
                currentStream = currentStream,
                streamUrl = streamUrl,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                epgEntries = epgEntries,
                onTogglePlay = { viewModel.togglePlay() },
                onRestart = { currentStream?.let { viewModel.playStream(it) } },
                onFullscreen = { viewModel.toggleFullscreen() },
                onOpenEpg = { currentStream?.let { onOpenEpg(it.id) } },
                modifier = Modifier.weight(0.4f)
            )

            ContentSection(
                categoriesState = categoriesState,
                selectedCategoryId = selectedCategoryId,
                streamsState = streamsState,
                favoriteIds = favoriteIds,
                currentStream = currentStream,
                searchQuery = searchQuery,
                categoryCounts = categoryCounts,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSelectCategory = { viewModel.selectCategory(it) },
                onPlayStream = { viewModel.playStream(it) },
                onToggleFavorite = { stream -> viewModel.toggleFavorite(stream) },
                onOpenEpg = onOpenEpg,
                channelEpgTitles = channelEpg,
                onLoadChannelEpg = { viewModel.loadChannelEpg(it) },
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.refresh()
                },
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

@Composable
private fun PlayerSection(
    exoPlayer: ExoPlayer,
    currentStream: LiveStream?,
    streamUrl: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    epgEntries: List<EpgEntry>,
    onTogglePlay: () -> Unit,
    onRestart: () -> Unit,
    onFullscreen: () -> Unit,
    onOpenEpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        if (streamUrl.isNotEmpty() && currentStream != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            PlayerOverlay(
                streamName = currentStream.name,
                epgEntries = epgEntries,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                onRestart = onRestart,
                onFullscreen = onFullscreen,
                onOpenEpg = onOpenEpg
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择一个频道播放",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PlayerOverlay(
    streamName: String,
    epgEntries: List<EpgEntry>,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onRestart: () -> Unit,
    onFullscreen: () -> Unit,
    onOpenEpg: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
    var volume by remember { mutableStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls }
    ) {
        if (showControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = streamName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = "EPG",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onOpenEpg)
                        .padding(2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(LiveRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            if (epgEntries.isNotEmpty()) {
                val currentEpg = epgEntries.find { it.isNowPlaying }
                val nextEpg = epgEntries.firstOrNull { !it.isNowPlaying }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .background(Color(0x80000000), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        currentEpg?.let { epg ->
                            Text(
                                text = epg.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        nextEpg?.let { epg ->
                            Text(
                                text = "下一节目: ${epg.title}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCCCCCC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = onTogglePlay)
                                .padding(4.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = onRestart)
                                .padding(4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "音量",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = { showVolumeSlider = !showVolumeSlider })
                                .padding(4.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "全屏",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = onFullscreen)
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (showControls && showVolumeSlider) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .height(180.dp)
                        .width(48.dp)
                        .background(Color(0x80000000), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { v ->
                            volume = v.toInt()
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, v.toInt(), 0)
                        },
                        valueRange = 0f..maxVolume.toFloat(),
                        modifier = Modifier
                            .width(150.dp)
                            .rotate(-90f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentSection(
    categoriesState: UiState<List<LiveCategory>>,
    selectedCategoryId: Int?,
    streamsState: UiState<List<LiveStream>>,
    favoriteIds: Set<Int>,
    currentStream: LiveStream?,
    searchQuery: String,
    categoryCounts: Map<Int, Int>,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (Int?) -> Unit,
    onPlayStream: (LiveStream) -> Unit,
    onToggleFavorite: (LiveStream) -> Unit,
    onOpenEpg: (Int) -> Unit,
    channelEpgTitles: Map<Int, String>,
    onLoadChannelEpg: (Int) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(DarkBg)) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CategorySidebar(
                categoriesState = categoriesState,
                selectedCategoryId = selectedCategoryId,
                categoryCounts = categoryCounts,
                onSelectCategory = onSelectCategory,
                modifier = Modifier.weight(0.3f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.1f))
            )

            ChannelList(
                streamsState = streamsState,
                selectedCategoryId = selectedCategoryId,
                favoriteIds = favoriteIds,
                currentStream = currentStream,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onPlayStream = onPlayStream,
                onToggleFavorite = onToggleFavorite,
                onOpenEpg = onOpenEpg,
                channelEpgTitles = channelEpgTitles,
                onLoadChannelEpg = onLoadChannelEpg,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(0.7f)
            )
        }
    }
}



@Composable
private fun CategorySidebar(
    categoriesState: UiState<List<LiveCategory>>,
    selectedCategoryId: Int?,
    categoryCounts: Map<Int, Int>,
    onSelectCategory: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().background(DarkSurface)) {
        when (categoriesState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Error -> ErrorView(message = categoriesState.message, onRetry = {})
            is UiState.Empty -> { }
            is UiState.Success -> {
                val cats = categoriesState.data
                val totalCount = categoryCounts.values.sum()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        CategoryItem(
                            name = "全部",
                            count = totalCount,
                            isSelected = selectedCategoryId == null,
                            onClick = { onSelectCategory(null) }
                        )
                    }
                    item {
                        CategoryItem(
                            name = "收藏",
                            count = null,
                            isSelected = selectedCategoryId == LiveViewModel.FAVORITES_ID,
                            onClick = { onSelectCategory(LiveViewModel.FAVORITES_ID) }
                        )
                    }
                    if (cats.isNotEmpty()) {
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                        }
                        items(cats, key = { it.id }) { category ->
                            CategoryItem(
                                name = category.name,
                                count = categoryCounts[category.id],
                                isSelected = selectedCategoryId == category.id,
                                onClick = { onSelectCategory(category.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    name: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFF333333) else Color.Transparent
    val textColor = if (isSelected) Color.White else DarkText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextSecondary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelList(
    streamsState: UiState<List<LiveStream>>,
    selectedCategoryId: Int?,
    favoriteIds: Set<Int>,
    currentStream: LiveStream?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPlayStream: (LiveStream) -> Unit,
    onToggleFavorite: (LiveStream) -> Unit,
    onOpenEpg: (Int) -> Unit,
    channelEpgTitles: Map<Int, String>,
    onLoadChannelEpg: (Int) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索", color = Color(0xFF999999), fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(14.dp))
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color(0xFF444444),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(4.dp),
                interactionSource = remember { MutableInteractionSource() }
            )
        }
        when (val state = streamsState) {
            is UiState.Loading -> {
                if (!isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
            }
            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is UiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无频道",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DarkTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedCategoryId == LiveViewModel.FAVORITES_ID) "暂无收藏频道" else "暂无频道",
                            style = MaterialTheme.typography.bodyLarge,
                            color = DarkTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(state.data, key = { _, stream -> stream.id }) { index, stream ->
                                ChannelItem(
                                    index = index + 1,
                                    stream = stream,
                                    isFavorite = stream.id in favoriteIds,
                                    isSelected = stream.id == currentStream?.id,
                                    currentEpgTitle = channelEpgTitles[stream.id],
                                    onClick = { onPlayStream(stream) },
                                    onToggleFavorite = { onToggleFavorite(stream) },
                                    onOpenEpg = { onOpenEpg(stream.id) },
                                    onLoadChannelEpg = { onLoadChannelEpg(stream.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelItem(
    index: Int,
    stream: LiveStream,
    isFavorite: Boolean,
    isSelected: Boolean,
    currentEpgTitle: String?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenEpg: () -> Unit,
    onLoadChannelEpg: () -> Unit
) {
    LaunchedEffect(stream.id) {
        onLoadChannelEpg()
    }
    val bgColor = if (isSelected) Color(0xFF333333) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!stream.iconUrl.isNullOrEmpty()) {
            AsyncImage(
                model = stream.iconUrl,
                contentDescription = stream.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stream.name.firstOrNull()?.toString() ?: "?",
                    color = DarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$index",
            color = DarkTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.name,
                style = MaterialTheme.typography.bodySmall,
                color = DarkText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!currentEpgTitle.isNullOrEmpty()) {
                Text(
                    text = currentEpgTitle,
                    color = DarkTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onOpenEpg) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = "EPG",
                tint = DarkTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) LiveRed else DarkTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FullscreenPlayerView(
    exoPlayer: ExoPlayer,
    currentStream: LiveStream?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    epgEntries: List<EpgEntry>,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onOpenEpg: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        var showFullscreenControls by remember { mutableStateOf(true) }
        LaunchedEffect(showFullscreenControls) {
            if (showFullscreenControls) {
                kotlinx.coroutines.delay(3000)
                showFullscreenControls = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showFullscreenControls = !showFullscreenControls }
        ) {
            if (showFullscreenControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x80000000))
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出全屏",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = currentStream?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onOpenEpg) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "EPG",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(LiveRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (epgEntries.isNotEmpty()) {
                    val currentEpg = epgEntries.find { it.isNowPlaying }
                    val nextEpg = epgEntries.firstOrNull { !it.isNowPlaying }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 48.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .background(Color(0x80000000), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            currentEpg?.let { epg ->
                                Text(
                                    text = epg.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            nextEpg?.let { epg ->
                                Text(
                                    text = "下一节目: ${epg.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCCCCCC),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0x80000000))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onTogglePlay)
                            .padding(6.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "全屏",
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onBack)
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
