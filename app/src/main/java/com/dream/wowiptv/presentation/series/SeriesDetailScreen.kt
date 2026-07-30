package com.dream.wowiptv.presentation.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.dream.wowiptv.domain.model.Episode
import com.dream.wowiptv.domain.model.Season
import com.dream.wowiptv.domain.model.SeriesInfo
import com.dream.wowiptv.domain.usecase.GetSeriesInfoUseCase
import com.dream.wowiptv.domain.usecase.WatchProgressUseCase
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.AccentBlue
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val getSeriesInfoUseCase: GetSeriesInfoUseCase,
    private val watchProgressUseCase: WatchProgressUseCase
) : ViewModel() {

    private val _info = MutableStateFlow<UiState<SeriesInfo>>(UiState.Loading)
    val info: StateFlow<UiState<SeriesInfo>> = _info.asStateFlow()

    private val _episodePositions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val episodePositions: StateFlow<Map<String, Long>> = _episodePositions.asStateFlow()

    fun load(seriesId: Int) {
        viewModelScope.launch {
            _info.value = UiState.Loading
            try {
                val result = getSeriesInfoUseCase(seriesId)
                _info.value = UiState.Success(result)
                val positions = mutableMapOf<String, Long>()
                result.episodes.values.flatten().forEach { ep ->
                    val pos = watchProgressUseCase.getProgress("series_${ep.id}")
                    if (pos > 0) positions[ep.id] = pos
                }
                _episodePositions.value = positions
            } catch (e: Exception) {
                _info.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }
}

@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onPlayEpisode: (episodeId: String, episodeTitle: String, position: Long) -> Unit
) {
    val infoState by viewModel.info.collectAsState()

    LaunchedEffect(seriesId) {
        viewModel.load(seriesId)
    }

    when (val state = infoState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.load(seriesId) })
        is UiState.Empty -> ErrorView(message = "暂无数据", onRetry = { viewModel.load(seriesId) })
        is UiState.Success -> SeriesDetailContent(
            info = state.data,
            episodePositions = viewModel.episodePositions.collectAsState().value,
            onBack = onBack,
            onPlayEpisode = onPlayEpisode
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesDetailContent(
    info: SeriesInfo,
    episodePositions: Map<String, Long> = emptyMap(),
    onBack: () -> Unit,
    onPlayEpisode: (episodeId: String, episodeTitle: String, position: Long) -> Unit
) {
    val series = info.info
    val allEpisodes = info.episodes.values.flatten()

    MaterialTheme(colorScheme = DarkColorScheme) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .statusBarsPadding()
            ) {
                AsyncImage(
                    model = series.cover,
                    contentDescription = series.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = series.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    series.rating?.let { rating ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.height(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    series.releaseDate?.let { date ->
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = date.take(10),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                series.genre?.let { genreStr ->
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        genreStr.split(",").forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = tag.trim(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }

                series.plot?.let { plot ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "剧情简介",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plot,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                series.cast?.let { cast ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "演员",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cast,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                series.director?.let { director ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "导演",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = director,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "剧集列表",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                val seasons = info.seasons
                var selectedSeasonIdx by remember { mutableStateOf(0) }

                if (seasons.isNotEmpty()) {
                    val selectedSeason = seasons.getOrElse(selectedSeasonIdx) { seasons.first() }
                    var seasonEpisodes = allEpisodes.filter { it.seasonNum == selectedSeason.seasonNumber }
                    if (seasonEpisodes.isEmpty()) {
                        seasonEpisodes = allEpisodes.take(selectedSeason.episodeCount)
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(seasons) { idx, season ->
                            val isSelected = idx == selectedSeasonIdx
                            Surface(
                                onClick = { selectedSeasonIdx = idx },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF1E88E5) else Color(0xFF2C2C2C)
                            ) {
                                Text(
                                    text = season.name,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (seasonEpisodes.isEmpty()) {
                        Text(
                            text = "暂无剧集",
                            color = Color(0xFF999999),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)).padding(8.dp)
                        ) {
                            seasonEpisodes.forEachIndexed { index, episode ->
                                EpisodeItem(
                                    episode = episode,
                                    savedPosition = episodePositions[episode.id] ?: 0L,
                                    onPlay = {
                                        val episodeTitle = "${series.name} - ${selectedSeason.name} E${episode.episodeNum}"
                                        onPlayEpisode(episode.id, episodeTitle, episodePositions[episode.id] ?: 0L)
                                    }
                                )
                                if (index < seasonEpisodes.lastIndex) {
                                    HorizontalDivider(color = Color(0xFF444444), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    }
}

@Composable
private fun EpisodeItem(
    episode: Episode,
    savedPosition: Long = 0L,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = episode.episodeNum.toString().padStart(2, '0'),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (savedPosition > 0) Color(0xFF4CAF50) else AccentBlue,
            modifier = Modifier.width(28.dp)
        )
        if (savedPosition > 0) {
            Text(
                text = "▶",
                color = Color(0xFF4CAF50),
                fontSize = 10.sp,
                modifier = Modifier.width(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFDDDDDD),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.plot != null) {
                Text(
                    text = episode.plot,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "播放",
            tint = AccentBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}
