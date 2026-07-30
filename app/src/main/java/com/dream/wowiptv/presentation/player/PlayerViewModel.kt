package com.dream.wowiptv.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.usecase.GetShortEpgUseCase
import com.dream.wowiptv.domain.usecase.PlayStreamUseCase
import com.dream.wowiptv.domain.usecase.WatchProgressUseCase
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
    private val getShortEpgUseCase: GetShortEpgUseCase,
    private val watchProgressUseCase: WatchProgressUseCase
) : ViewModel() {

    val streamType: String = savedStateHandle["streamType"] ?: "live"
    val streamId: String = savedStateHandle["streamId"] ?: "0"
    val streamName: String = savedStateHandle["name"] ?: ""

    private val _streamUrl = MutableStateFlow("")
    val streamUrl: StateFlow<String> = _streamUrl.asStateFlow()

    private val _epgEntries = MutableStateFlow<List<EpgEntry>>(emptyList())
    val epgEntries: StateFlow<List<EpgEntry>> = _epgEntries.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        android.util.Log.d("PlayerVM", "init streamType=$streamType streamId=$streamId name=$streamName")
        loadStreamUrl()
        if (streamType == "live") {
            observeEpg()
        }
    }

    private fun loadStreamUrl() {
        viewModelScope.launch {
            try {
                val idNum = streamId.toIntOrNull() ?: 0
                val type = when (streamType) {
                    "live" -> PlayStreamUseCase.StreamType.Live(idNum)
                    "vod" -> PlayStreamUseCase.StreamType.Vod(idNum)
                    "series" -> PlayStreamUseCase.StreamType.Series(streamId)
                    else -> PlayStreamUseCase.StreamType.Live(idNum)
                }
                _streamUrl.value = playStreamUseCase(type)
                android.util.Log.d("PlayerVM", "URL loaded for $streamType $streamId")
                if (streamType == "live") {
                    val contentId = "live_$streamId"
                    val pos = System.currentTimeMillis() / 1000
                    watchProgressUseCase.saveProgress(contentId, "live", streamName, null, pos, 0L)
                    android.util.Log.d("PlayerVM", "saved live $contentId pos=$pos")
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "loadStreamUrl failed", e)
            }
        }
    }

    private fun observeEpg() {
        viewModelScope.launch {
            getShortEpgUseCase(streamId.toIntOrNull() ?: 0).collect { entries ->
                _epgEntries.value = entries
            }
        }
    }

    fun saveProgress(contentId: String, position: Long, duration: Long) {
        viewModelScope.launch {
            try {
                val pos = if (streamType == "live") System.currentTimeMillis() / 1000 else position
                watchProgressUseCase.saveProgress(
                    contentId = contentId,
                    contentType = streamType,
                    name = streamName,
                    icon = null,
                    position = pos,
                    duration = duration
                )
                android.util.Log.d("PlayerVM", "saved $streamType $contentId pos=$pos")
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "save failed", e)
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
