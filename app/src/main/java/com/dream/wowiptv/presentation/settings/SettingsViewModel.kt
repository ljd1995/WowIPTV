package com.dream.wowiptv.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.BuildConfig
import com.dream.wowiptv.data.local.SourcePreferences
import com.dream.wowiptv.domain.model.UserInfo
import com.dream.wowiptv.domain.model.XtreamSource
import com.dream.wowiptv.domain.usecase.GetUserInfoUseCase
import com.dream.wowiptv.domain.usecase.ManageSourcesUseCase
import com.dream.wowiptv.domain.usecase.SwitchSourceUseCase
import com.dream.wowiptv.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val manageSourcesUseCase: ManageSourcesUseCase,
    private val switchSourceUseCase: SwitchSourceUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val sourcePreferences: SourcePreferences,
) : ViewModel() {

    val versionName: String = BuildConfig.VERSION_NAME

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = sourcePreferences.userInfo.first()
            if (cached != null) _userInfo.value = cached
            refreshUserInfo()
        }
    }

    fun refreshUserInfo() {
        viewModelScope.launch {
            val result = getUserInfoUseCase()
            if (result != null) {
                _userInfo.value = result
                sourcePreferences.saveUserInfo(result)
            }
        }
    }

    val sources: StateFlow<UiState<List<XtreamSource>>> = manageSourcesUseCase.getSources()
        .map { list ->
            if (list.isEmpty()) UiState.Empty else UiState.Success(list)
        }
        .catch { e -> emit(UiState.Error(e.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val activeSourceId: StateFlow<Long?> = manageSourcesUseCase.getActiveSource()
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _syncingIds = MutableStateFlow<Set<Long>>(emptySet())
    val syncingIds: StateFlow<Set<Long>> = _syncingIds.asStateFlow()

    suspend fun addSource(name: String, serverUrl: String, port: Int, username: String, password: String) {
        val newId = manageSourcesUseCase.addSource(name, serverUrl, port, username, password)
        switchSourceUseCase(newId)
    }

    suspend fun updateSource(id: Long, name: String, serverUrl: String, port: Int, username: String, password: String) {
        manageSourcesUseCase.updateSource(
            XtreamSource(id, name, serverUrl, port, username, password)
        )
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

    fun syncSource(id: Long) {
        viewModelScope.launch {
            _syncingIds.update { it + id }
            try {
                switchSourceUseCase(id)
            } finally {
                _syncingIds.update { it - id }
            }
        }
    }
}
