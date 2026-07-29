package com.dream.wowiptv.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.domain.model.XtreamSource
import com.dream.wowiptv.domain.usecase.ManageSourcesUseCase
import com.dream.wowiptv.domain.usecase.SwitchSourceUseCase
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val manageSourcesUseCase: ManageSourcesUseCase,
    private val switchSourceUseCase: SwitchSourceUseCase
) : ViewModel() {

    val sources: StateFlow<UiState<List<XtreamSource>>> = manageSourcesUseCase.getSources()
        .map { list ->
            if (list.isEmpty()) UiState.Empty else UiState.Success(list)
        }
        .catch { e -> emit(UiState.Error(e.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val activeSourceId: StateFlow<Long?> = manageSourcesUseCase.getActiveSource()
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addSource(name: String, serverUrl: String, port: Int, username: String, password: String) {
        viewModelScope.launch {
            manageSourcesUseCase.addSource(name, serverUrl, port, username, password)
        }
    }

    fun updateSource(id: Long, name: String, serverUrl: String, port: Int, username: String, password: String) {
        viewModelScope.launch {
            manageSourcesUseCase.updateSource(
                XtreamSource(id, name, serverUrl, port, username, password)
            )
        }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch {
            manageSourcesUseCase.deleteSource(id)
        }
    }

    fun switchSource(id: Long) {
        viewModelScope.launch {
            switchSourceUseCase(id)
        }
    }
}
