package com.dream.wowiptv.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.usecase.GetShortEpgUseCase
import com.dream.wowiptv.domain.usecase.PlayStreamUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playStreamUseCase: PlayStreamUseCase,
    private val getShortEpgUseCase: GetShortEpgUseCase
) : ViewModel() {

    private val streamType: String = savedStateHandle["streamType"] ?: "live"
    private val streamId: Int = savedStateHandle["streamId"] ?: 0

    private val _streamUrl = MutableStateFlow("")
    val streamUrl: StateFlow<String> = _streamUrl.asStateFlow()

    private val _epgEntries = MutableStateFlow<List<EpgEntry>>(emptyList())
    val epgEntries: StateFlow<List<EpgEntry>> = _epgEntries.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        loadStreamUrl()
        if (streamType == "live") {
            observeEpg()
        }
    }

    private fun loadStreamUrl() {
        viewModelScope.launch {
            val type = when (streamType) {
                "live" -> PlayStreamUseCase.StreamType.Live(streamId)
                "vod" -> PlayStreamUseCase.StreamType.Vod(streamId)
                "series" -> PlayStreamUseCase.StreamType.Series(streamId)
                else -> PlayStreamUseCase.StreamType.Live(streamId)
            }
            _streamUrl.value = playStreamUseCase(type)
        }
    }

    private fun observeEpg() {
        viewModelScope.launch {
            getShortEpgUseCase(streamId).collect { entries ->
                _epgEntries.value = entries
            }
        }
    }

    fun play() {
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }
}
