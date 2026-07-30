package com.dream.wowiptv.presentation.player

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    streamType: String,
    streamId: String,
    streamName: String = "",
    startPosition: Long = 0L,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val streamUrl by viewModel.streamUrl.collectAsState()
    var showOverlay by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var hasSeeked by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            setPlaybackSpeed(1f)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) {
                        duration = this@apply.duration.toFloat().coerceAtLeast(0f)
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            viewModel.play()
        }
    }

    LaunchedEffect(streamUrl, startPosition) {
        if (streamUrl.isNotEmpty() && startPosition > 0 && !hasSeeked) {
            delay(500)
            exoPlayer.seekTo(startPosition)
            hasSeeked = true
        }
    }

    val contentId = when (streamType) {
        "vod" -> "vod_$streamId"
        "series" -> "series_$streamId"
        else -> ""
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (exoPlayer.isPlaying && duration > 0) {
                viewModel.saveProgress(contentId, exoPlayer.currentPosition, duration.toLong())
            }
        }
    }

    LaunchedEffect(Unit) {
        if (streamType != "live") {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (duration > 0) viewModel.saveProgress(contentId, exoPlayer.currentPosition, duration.toLong())
            exoPlayer.stop()
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                val window = act.window
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(4000)
            showOverlay = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (dur > 0) {
                position = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                duration = dur.toFloat()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showOverlay = !showOverlay }
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
            CircularProgressIndicator(
                color = Color(0xFF1E88E5),
                modifier = Modifier.size(36.dp).align(Alignment.Center),
                strokeWidth = 3.dp
            )
        }

        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        if (duration > 0) viewModel.saveProgress(contentId, exoPlayer.currentPosition, duration.toLong())
                        exoPlayer.stop()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    if (streamName.isNotBlank()) {
                        Text(
                            text = streamName,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x80000000))
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                                viewModel.pause()
                            } else {
                                exoPlayer.play()
                                viewModel.play()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    var dragging by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = Color(0xFF555555)
                        val progressColor = Color(0xFF1E88E5)
                        val thumbColor = Color(0xFF1E88E5)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            dragging = true
                                            val newPos = (offset.x / size.width).coerceIn(0f, 1f)
                                            position = newPos
                                        },
                                        onHorizontalDrag = { change, _ ->
                                            val newPos = (change.position.x / size.width).coerceIn(0f, 1f)
                                            position = newPos
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            exoPlayer.seekTo((position * duration).toLong())
                                        }
                                    )
                                }
                        ) {
                            val trackHeight = 2.dp.toPx()
                            val trackTop = size.height / 2 - trackHeight / 2
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(0f, trackTop),
                                size = size.copy(height = trackHeight),
                                cornerRadius = CornerRadius(trackHeight / 2)
                            )
                            val progressWidth = size.width * position
                            drawRoundRect(
                                color = progressColor,
                                topLeft = Offset(0f, trackTop),
                                size = size.copy(width = progressWidth, height = trackHeight),
                                cornerRadius = CornerRadius(trackHeight / 2)
                            )
                            val thumbRadius = 3.dp.toPx()
                            val thumbX = (size.width * position).coerceIn(thumbRadius, size.width - thumbRadius)
                            drawCircle(
                                color = thumbColor,
                                radius = thumbRadius,
                                center = Offset(thumbX, size.height / 2)
                            )
                        }
                    }

                    Text(
                        text = "${formatTime((position * duration).toLong())} / ${formatTime(duration.toLong())}",
                        color = Color(0xFFCCCCCC),
                        fontSize = 11.sp,
                        modifier = Modifier.width(78.dp)
                    )

                    Box {
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "设置",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${speed}x${if (speed == playbackSpeed) " ✓" else ""}",
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        playbackSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}