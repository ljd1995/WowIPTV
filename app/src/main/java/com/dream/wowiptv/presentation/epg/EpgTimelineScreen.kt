package com.dream.wowiptv.presentation.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import com.dream.wowiptv.presentation.common.theme.LiveRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val hourWidth = 120.dp
private val totalHours = 6
private val timelineWidth = hourWidth * totalHours
private val channelRowHeight = 56.dp
private val channelLabelWidth = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpgTimelineScreen(
    viewModel: EpgViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPlayChannel: (Int) -> Unit
) {
    val channelsState by viewModel.channels.collectAsState()
    val epgDataState by viewModel.epgData.collectAsState()
    val selectedChannelId by viewModel.selectedChannelId.collectAsState()

    var selectedProgram by remember { mutableStateOf<EpgEntry?>(null) }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            containerColor = Color(0xFF1E1E1E),
            topBar = {
                TopAppBar(
                    title = { Text("EPG", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = channelsState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> ErrorView(message = state.message, onRetry = { })
                    is UiState.Empty, is UiState.Success -> {
                        val channels = (state as? UiState.Success)?.data ?: emptyList()
                        if (channels.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No channels",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            when (val epgState = epgDataState) {
                                is UiState.Error -> ErrorView(
                                    message = epgState.message,
                                    onRetry = { viewModel.retryLoadEpg() }
                                )
                                is UiState.Loading -> {
                                    if (channelsState is UiState.Success) {
                                        LoadingIndicator()
                                    }
                                }
                                else -> {
                                    EpgGrid(
                                        channels = channels,
                                        epgDataState = epgDataState,
                                        selectedChannelId = selectedChannelId,
                                        onSelectChannel = viewModel::selectChannel,
                                        onProgramClick = { selectedProgram = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedProgram?.let { program ->
            ModalBottomSheet(
                onDismissRequest = { selectedProgram = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF2C2C2C)
            ) {
                ProgramDetailContent(program = program)
            }
        }
    }
}

@Composable
private fun EpgGrid(
    channels: List<LiveStream>,
    epgDataState: UiState<Map<Int, List<EpgEntry>>>,
    selectedChannelId: Int?,
    onSelectChannel: (Int) -> Unit,
    onProgramClick: (EpgEntry) -> Unit
) {
    val timelineScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val currentTimeMs = remember { System.currentTimeMillis() }
    val timelineStartMs = remember { currentTimeMs - 2 * 3600 * 1000L }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(timelineScrollState)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Spacer(modifier = Modifier.width(channelLabelWidth))
            Row(modifier = Modifier.width(timelineWidth)) {
                for (i in 0 until totalHours) {
                    val hourTime = timelineStartMs + i * 3600 * 1000L
                    Text(
                        text = formatTime(hourTime),
                        modifier = Modifier
                            .width(hourWidth)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Row {
                Column(
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                        .width(channelLabelWidth)
                ) {
                    channels.forEach { channel ->
                        ChannelLabel(
                            stream = channel,
                            isSelected = channel.id == selectedChannelId,
                            onClick = { onSelectChannel(channel.id) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(timelineScrollState)
                        .verticalScroll(verticalScrollState)
                ) {
                    Box(modifier = Modifier.width(timelineWidth)) {
                        val epgData = when (epgDataState) {
                            is UiState.Success -> epgDataState.data
                            else -> emptyMap()
                        }

                        Column {
                            channels.forEach { channel ->
                                ChannelTimelineRow(
                                    epgEntries = epgData[channel.id].orEmpty(),
                                    currentTimeMs = currentTimeMs,
                                    timelineStartMs = timelineStartMs,
                                    onClick = onProgramClick
                                )
                            }
                        }

                        val nowOffset = hourWidth * ((currentTimeMs - timelineStartMs) / 3600000f)
                        Box(
                            modifier = Modifier
                                .offset(x = nowOffset)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(LiveRed)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLabel(
    stream: LiveStream,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(channelRowHeight)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stream.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ChannelTimelineRow(
    epgEntries: List<EpgEntry>,
    currentTimeMs: Long,
    timelineStartMs: Long,
    onClick: (EpgEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(channelRowHeight)
            .width(timelineWidth)
    ) {
        var currentX = 0.dp
        val sorted = epgEntries.sortedBy { it.startTime }

        sorted.forEach { entry ->
            val startOffset = hourWidth * max(
                0f,
                ((entry.startTime - timelineStartMs) / 3600000f)
            )

            val gap = (startOffset - currentX).coerceAtLeast(0.dp)
            if (gap > 0.dp) {
                Spacer(modifier = Modifier.width(gap))
                currentX += gap
            }

            val durationHours = max(
                0.05f,
                (entry.endTime - max(entry.startTime, timelineStartMs)) / 3600000f
            )
            val blockWidth = (hourWidth * durationHours).coerceAtLeast(60.dp)

            val isCurrent = entry.isNowPlaying

            Box(
                modifier = Modifier
                    .width(blockWidth)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(onClick = { onClick(entry) })
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTimeShort(entry.startTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            currentX += blockWidth
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ProgramDetailContent(program: EpgEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = program.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!program.description.isNullOrBlank()) {
            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Text(
            text = "${formatTime(program.startTime)} - ${formatTime(program.endTime)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatTimeShort(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
