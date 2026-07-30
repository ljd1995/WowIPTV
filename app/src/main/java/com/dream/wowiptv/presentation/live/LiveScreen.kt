package com.dream.wowiptv.presentation.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.domain.model.LiveCategory
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.EpgInfoBar
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    viewModel: LiveViewModel = hiltViewModel(),
    onPlayStream: (Int) -> Unit,
    onNavigateToEpg: (Int) -> Unit
) {
    val categoriesState by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val streamsState by viewModel.streams.collectAsState()
    val epgMap by viewModel.epgMap.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(streamsState) {
        if (streamsState !is UiState.Loading) {
            isRefreshing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (categoriesState is UiState.Success) {
            val cats = (categoriesState as UiState.Success<List<LiveCategory>>).data
            if (cats.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("全部") }
                        )
                    }
                    items(cats, key = { it.id }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (val streams = streamsState) {
                is UiState.Loading -> {
                    if (!isRefreshing) {
                        LoadingIndicator()
                    }
                }
                is UiState.Error -> {
                    ErrorView(
                        message = streams.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无频道",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is UiState.Success -> {
                    if (streams.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无频道",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.refresh()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(streams.data, key = { it.id }) { stream ->
                                    val epgList = epgMap[stream.id] ?: emptyList()
                                    val currentEpg = epgList.find { it.isNowPlaying }
                                    val nextEpg = epgList.firstOrNull { !it.isNowPlaying }

                                    ChannelItem(
                                        stream = stream,
                                        currentEpg = currentEpg,
                                        nextEpg = nextEpg,
                                        onPlay = { onPlayStream(stream.id) },
                                        onEpg = { onNavigateToEpg(stream.id) }
                                    )
                                }
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
    stream: LiveStream,
    currentEpg: com.dream.wowiptv.domain.model.EpgEntry?,
    nextEpg: com.dream.wowiptv.domain.model.EpgEntry?,
    onPlay: () -> Unit,
    onEpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onPlay,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = stream.iconUrl,
                contentDescription = stream.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                EpgInfoBar(
                    currentEpg = currentEpg,
                    nextEpg = nextEpg
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onEpg) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "EPG"
                )
            }
        }
    }
}
