package com.dream.wowiptv.presentation.movies

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dream.wowiptv.R
import com.dream.wowiptv.data.local.AppPreferences
import com.dream.wowiptv.domain.model.VodInfo
import com.dream.wowiptv.domain.repository.TmdbRepository
import com.dream.wowiptv.domain.usecase.GetVodInfoUseCase
import com.dream.wowiptv.domain.usecase.WatchProgressUseCase
import com.dream.wowiptv.presentation.common.UiState
import com.dream.wowiptv.presentation.common.components.GradientBackground
import com.dream.wowiptv.presentation.common.components.CastAvatarRow
import com.dream.wowiptv.presentation.common.components.PersonAvatar
import com.dream.wowiptv.presentation.common.components.ErrorView
import com.dream.wowiptv.presentation.common.components.LoadingIndicator
import com.dream.wowiptv.presentation.common.theme.DarkColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import android.content.Context

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val getVodInfoUseCase: GetVodInfoUseCase,
    private val watchProgressUseCase: WatchProgressUseCase,
    private val appPreferences: AppPreferences,
    private val tmdbRepository: TmdbRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val vodId: Int = savedStateHandle.get<Int>("vodId") ?: 0

    val vodInfo: StateFlow<UiState<VodInfo>> = flow {
        emit(UiState.Loading)
        val info = getVodInfoUseCase(vodId)
        emit(UiState.Success(info))
    }.catch { emit(UiState.Error(it.message ?: context.getString(R.string.err_load_details))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val avatarsEnabled: StateFlow<Boolean> = combine(
        appPreferences.showCastAvatars,
        appPreferences.tmdbApiKey
    ) { show, key -> show && key.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _castImages = MutableStateFlow<Map<String, String>>(emptyMap())
    val castImages: StateFlow<Map<String, String>> = _castImages.asStateFlow()

    fun loadCastImages(info: VodInfo) {
        if (!avatarsEnabled.value || _castImages.value.isNotEmpty()) return
        viewModelScope.launch {
            val key = appPreferences.tmdbApiKey.first()
            if (key.isBlank()) return@launch
            val names = (listOfNotNull(info.director) + listOfNotNull(info.cast))
                .flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(12)
            if (names.isEmpty()) return@launch
            val map = tmdbRepository.fetchPeopleImages(info.name, info.releasedate, names, key)
            _castImages.value = map.filterValues { !it.isNullOrBlank() }.mapValues { it.value!! }
        }
    }

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
    val avatarsEnabled by viewModel.avatarsEnabled.collectAsState()
    val castImages by viewModel.castImages.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshPosition()
    }

    LaunchedEffect(vodInfoState, avatarsEnabled) {
        val info = (vodInfoState as? UiState.Success)?.data
        if (info != null && avatarsEnabled) {
            viewModel.loadCastImages(info)
        }
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
            GradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                val savedPos by viewModel.savedPosition.collectAsState()
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
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
                                .height(140.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                    )
                                )
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.statusBarsPadding()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 112.dp)
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
                                    text = formatDuration(secs, LocalContext.current),
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
                                text = stringResource(R.string.movies_overview),
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
                                text = stringResource(R.string.movies_cast),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val castNames = cast.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (avatarsEnabled && castNames.isNotEmpty()) {
                                CastAvatarRow(names = castNames, images = castImages)
                            } else {
                                Text(
                                    text = cast,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        info.director?.let { director ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.movies_director),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val directorName = director.trim()
                            if (avatarsEnabled && directorName.isNotEmpty()) {
                                PersonAvatar(name = directorName, imageUrl = castImages[directorName])
                            } else {
                                Text(
                                    text = director,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (savedPos > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { onPlay(info.id, info.name, 0L) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF8B5CF6)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6))
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.common_restart))
                            }
                            Button(
                                onClick = { onPlay(info.id, info.name, savedPos) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.common_continue))
                            }
                        }
                    } else {
                        Button(
                            onClick = { onPlay(info.id, info.name, 0L) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.common_play))
                    }
                }
            }
            }
        }
    }
    }
    }
}

private fun formatDuration(secs: Int, context: Context): String {
    val hours = secs / 3600
    val minutes = (secs % 3600) / 60
    val seconds = secs % 60
    return when {
        hours > 0 -> context.getString(R.string.movies_duration_hm, hours, minutes)
        minutes > 0 -> context.getString(R.string.movies_duration_ms, minutes, seconds)
        else -> context.getString(R.string.movies_duration_s, seconds)
    }
}
