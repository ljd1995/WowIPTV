package com.dream.wowiptv.presentation.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.data.local.dao.FavoriteStreamDao
import com.dream.wowiptv.data.local.entity.FavoriteStreamEntity
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.model.LiveCategory
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.domain.repository.LiveTvRepository
import com.dream.wowiptv.domain.usecase.GetLiveCategoriesUseCase
import com.dream.wowiptv.domain.usecase.GetLiveStreamsUseCase
import com.dream.wowiptv.domain.usecase.GetShortEpgUseCase
import com.dream.wowiptv.domain.usecase.PlayStreamUseCase
import com.dream.wowiptv.domain.usecase.WatchProgressUseCase
import com.dream.wowiptv.domain.repository.SourceRepository
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val getLiveCategoriesUseCase: GetLiveCategoriesUseCase,
    private val getLiveStreamsUseCase: GetLiveStreamsUseCase,
    private val getShortEpgUseCase: GetShortEpgUseCase,
    private val liveTvRepository: LiveTvRepository,
    private val playStreamUseCase: PlayStreamUseCase,
    private val sourceRepository: SourceRepository,
    private val favoriteStreamDao: FavoriteStreamDao,
    private val watchProgressUseCase: WatchProgressUseCase
) : ViewModel() {

    companion object {
        const val FAVORITES_ID = -1
    }

    val categories: StateFlow<UiState<List<LiveCategory>>> = getLiveCategoriesUseCase()
        .map { UiState.Success(it) as UiState<List<LiveCategory>> }
        .catch { emit(UiState.Error(it.message ?: "加载分类失败")) }
        .onStart { emit(UiState.Loading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)

    val streams: StateFlow<UiState<List<LiveStream>>> = combine(
        _selectedCategoryId,
        _refreshTrigger
    ) { categoryId, _ -> categoryId }
        .flatMapLatest { categoryId ->
            if (categoryId == FAVORITES_ID) {
                getLiveStreamsUseCase(null)
                    .map { allStreams ->
                        val favIds = _favoriteIds.value
                        UiState.Success(allStreams.filter { it.id in favIds }) as UiState<List<LiveStream>>
                    }
                    .catch { emit(UiState.Error(it.message ?: "加载收藏失败")) }
                    .onStart { emit(UiState.Loading) }
            } else {
                getLiveStreamsUseCase(categoryId)
                    .map { UiState.Success(it) as UiState<List<LiveStream>> }
                    .catch { emit(UiState.Error(it.message ?: "加载频道失败")) }
                    .onStart { emit(UiState.Loading) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredStreams: StateFlow<UiState<List<LiveStream>>> = combine(
        streams, _searchQuery
    ) { s, query ->
        if (s !is UiState.Success) return@combine s
        if (query.isBlank()) return@combine s
        UiState.Success(s.data.filter { it.name.contains(query, ignoreCase = true) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val categoryCounts: StateFlow<Map<Int, Int>> = streams.map { s ->
        if (s !is UiState.Success) return@map emptyMap()
        s.data.groupBy { it.categoryId }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _currentStream = MutableStateFlow<LiveStream?>(null)
    val currentStream: StateFlow<LiveStream?> = _currentStream.asStateFlow()

    private val _streamUrl = MutableStateFlow("")
    val streamUrl: StateFlow<String> = _streamUrl.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _epgEntries = MutableStateFlow<List<EpgEntry>>(emptyList())
    val epgEntries: StateFlow<List<EpgEntry>> = _epgEntries.asStateFlow()

    private val _channelEpg = MutableStateFlow<Map<Int, String>>(emptyMap())
    val channelEpg: StateFlow<Map<Int, String>> = _channelEpg.asStateFlow()

    private val _epgLoadedIds = MutableStateFlow<Set<Int>>(emptySet())

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private var currentSourceId: Long = 0

    init {
        loadFavoriteIds()
    }

    private fun loadFavoriteIds() {
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first()
            if (source != null) {
                currentSourceId = source.id
                val ids = favoriteStreamDao.getFavoriteIdsBySource(source.id)
                _favoriteIds.value = ids.toSet()
            }
        }
    }

    fun selectCategory(id: Int?) {
        _selectedCategoryId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun playStream(stream: LiveStream) {
        _currentStream.value = stream
        _isPlaying.value = true
        _streamUrl.value = ""
        viewModelScope.launch {
            try {
                val url = playStreamUseCase(PlayStreamUseCase.StreamType.Live(stream.id))
                _streamUrl.value = url
            } catch (_: Exception) {
                _streamUrl.value = ""
            }
            try {
                val contentId = "live_${stream.id}"
                watchProgressUseCase.saveProgress(contentId, "live", stream.name, stream.iconUrl, System.currentTimeMillis() / 1000, 0L)
                android.util.Log.d("PlayerVM", "saved live $contentId name=${stream.name}")
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "save live failed", e)
            }
        }
        loadEpg(stream.id)
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun exitFullscreen() {
        _isFullscreen.value = false
    }

    fun toggleFavorite(stream: LiveStream) {
        viewModelScope.launch {
            sourceRepository.getActiveSource().first()?.let { source ->
                if (_favoriteIds.value.contains(stream.id)) {
                    favoriteStreamDao.delete(stream.id, source.id)
                    _favoriteIds.value = _favoriteIds.value - stream.id
                } else {
                    favoriteStreamDao.insert(
                        FavoriteStreamEntity(
                            streamId = stream.id,
                            sourceId = source.id,
                            name = stream.name,
                            iconUrl = stream.iconUrl,
                            categoryId = stream.categoryId
                        )
                    )
                    _favoriteIds.value = _favoriteIds.value + stream.id
                }
            }
        }
    }

    fun loadChannelEpg(streamId: Int) {
        if (_epgLoadedIds.value.contains(streamId)) return
        _epgLoadedIds.update { it + streamId }
        viewModelScope.launch {
            val cached = runCatching { getShortEpgUseCase(streamId).first() }.getOrNull().orEmpty()
            publishCurrentEpg(streamId, cached)
            if (cached.isEmpty()) {
                runCatching { liveTvRepository.refreshEpg(streamId) }
                val entries = runCatching { getShortEpgUseCase(streamId).first() }.getOrNull().orEmpty()
                publishCurrentEpg(streamId, entries)
            }
        }
    }

    private fun publishCurrentEpg(streamId: Int, entries: List<EpgEntry>) {
        val current = entries.find { it.isNowPlaying } ?: entries.firstOrNull()
        current?.let { _channelEpg.update { m -> m + (streamId to it.title) } }
    }

    private fun loadEpg(streamId: Int) {
        viewModelScope.launch {
            try {
                val entries = getShortEpgUseCase(streamId).first()
                _epgEntries.value = entries
            } catch (_: Exception) {
                _epgEntries.value = emptyList()
            }
        }
    }
}
