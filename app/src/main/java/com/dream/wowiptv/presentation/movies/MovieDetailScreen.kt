package com.dream.wowiptv.presentation.movies

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.domain.model.VodInfo
import com.dream.wowiptv.domain.usecase.GetVodInfoUseCase
import com.dream.wowiptv.domain.usecase.WatchProgressUseCase
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.AccentBlue
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val getVodInfoUseCase: GetVodInfoUseCase,
    private val watchProgressUseCase: WatchProgressUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vodId: Int = savedStateHandle.get<Int>("vodId") ?: 0

    val vodInfo: StateFlow<UiState<VodInfo>> = flow {
        emit(UiState.Loading)
        val info = getVodInfoUseCase(vodId)
        emit(UiState.Success(info))
    }.catch { emit(UiState.Error(it.message ?: "加载详情失败")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _savedPosition = MutableStateFlow(0L)
    val savedPosition: StateFlow<Long> = _savedPosition.asStateFlow()

    fun refreshPosition() {
        viewModelScope.launch {
            _savedPosition.value = watchProgressUseCase.getProgress("vod_$vodId")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    onPlay: (Int, String, Long) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val vodInfoState by viewModel.vodInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPosition()
    }

    when (val state = vodInfoState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error -> ErrorView(
            message = state.message,
            onRetry = { }
        )
        is UiState.Empty -> {}
        is UiState.Success -> {
            val info = state.data

            MaterialTheme(colorScheme = DarkColorScheme) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                val savedPos by viewModel.savedPosition.collectAsState()
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .statusBarsPadding()
                    ) {
                        AsyncImage(
                            model = info.cover,
                            contentDescription = info.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f))
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
                            text = info.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            info.releasedate?.take(4)?.let { year ->
                                Text(
                                    text = year,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            info.rating?.let { rating ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.height(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f".format(rating),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            info.durationSecs?.let { secs ->
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formatDuration(secs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        info.genre?.let { genreStr ->
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

                        info.plot?.let { plot ->
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

                        info.cast?.let { cast ->
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

                        info.director?.let { director ->
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

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    if (savedPos > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { onPlay(info.id, info.name, 0L) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("重新播放")
                            }
                            Button(
                                onClick = { onPlay(info.id, info.name, savedPos) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("继续播放")
                            }
                        }
                    } else {
                        Button(
                            onClick = { onPlay(info.id, info.name, 0L) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "播放")
                        }
                    }
                }
            }
            }
        }
    }
}

private fun formatDuration(secs: Int): String {
    val hours = secs / 3600
    val minutes = (secs % 3600) / 60
    val seconds = secs % 60
    return when {
        hours > 0 -> "${hours}小时${minutes}分钟"
        minutes > 0 -> "${minutes}分钟${seconds}秒"
        else -> "${seconds}秒"
    }
}
