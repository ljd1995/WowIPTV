package com.dream.wowiptv.presentation.epg

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.domain.usecase.GetLiveStreamsUseCase
import com.dream.wowiptv.domain.usecase.GetShortEpgUseCase
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpgViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getShortEpgUseCase: GetShortEpgUseCase,
    private val getLiveStreamsUseCase: GetLiveStreamsUseCase
) : ViewModel() {

    private val streamId: Int? = savedStateHandle.get<Int>("streamId")?.takeIf { it > 0 }

    private val _selectedChannelId = MutableStateFlow(streamId)
    val selectedChannelId: StateFlow<Int?> = _selectedChannelId.asStateFlow()

    val channels: StateFlow<UiState<List<LiveStream>>>

    val epgEntries: StateFlow<UiState<List<EpgEntry>>>

    init {
        val channelsFlow = getLiveStreamsUseCase(null)

        channels = if (streamId != null) {
            channelsFlow
                .map { list -> UiState.Success(list.filter { it.id == streamId }) as UiState<List<LiveStream>> }
                .catch { emit(UiState.Error(it.message ?: "Failed to load channels")) }
                .onStart { emit(UiState.Loading) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
        } else {
            channelsFlow
                .map { UiState.Success(it) as UiState<List<LiveStream>> }
                .catch { emit(UiState.Error(it.message ?: "Failed to load channels")) }
                .onStart { emit(UiState.Loading) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
        }

        epgEntries = _selectedChannelId
            .flatMapLatest { id ->
                if (id != null) {
                    getShortEpgUseCase(id)
                        .map { UiState.Success(it) as UiState<List<EpgEntry>> }
                        .catch { emit(UiState.Error(it.message ?: "Failed to load EPG")) }
                        .onStart { emit(UiState.Loading) }
                } else {
                    flowOf(UiState.Empty)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

        if (streamId == null) {
            viewModelScope.launch {
                val state = channels.first { it is UiState.Success<*> }
                if (state is UiState.Success && state.data.isNotEmpty()) {
                    _selectedChannelId.value = state.data.first().id
                }
            }
        }
    }

    fun selectChannel(id: Int) {
        _selectedChannelId.value = id
    }
}
