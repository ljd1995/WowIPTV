package com.dream.wowiptv.presentation.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.model.LiveCategory
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.domain.usecase.GetLiveCategoriesUseCase
import com.dream.wowiptv.domain.usecase.GetLiveStreamsUseCase
import com.dream.wowiptv.domain.usecase.GetShortEpgUseCase
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val getLiveCategoriesUseCase: GetLiveCategoriesUseCase,
    private val getLiveStreamsUseCase: GetLiveStreamsUseCase,
    private val getShortEpgUseCase: GetShortEpgUseCase
) : ViewModel() {

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
            getLiveStreamsUseCase(categoryId)
                .map { UiState.Success(it) as UiState<List<LiveStream>> }
                .catch { emit(UiState.Error(it.message ?: "加载频道失败")) }
                .onStart { emit(UiState.Loading) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _epgMap = MutableStateFlow<Map<Int, List<EpgEntry>>>(emptyMap())
    val epgMap: StateFlow<Map<Int, List<EpgEntry>>> = _epgMap.asStateFlow()

    init {
        loadEpgForStreams()
    }

    fun selectCategory(id: Int?) {
        _selectedCategoryId.value = id
    }

    fun refresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    private fun loadEpgForStreams() {
        viewModelScope.launch {
            streams.collect { state ->
                if (state is UiState.Success) {
                    val result: Map<Int, List<EpgEntry>> = coroutineScope {
                        state.data.map { stream ->
                            async {
                                stream.id to try {
                                    getShortEpgUseCase(stream.id).first()
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                        }.awaitAll().toMap()
                    }
                    _epgMap.value = result
                }
            }
        }
    }
}
