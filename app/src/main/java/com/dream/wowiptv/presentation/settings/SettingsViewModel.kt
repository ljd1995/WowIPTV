package com.dream.wowiptv.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.BuildConfig
import com.dream.wowiptv.data.local.AppPreferences
import com.dream.wowiptv.data.local.SourcePreferences
import com.dream.wowiptv.data.local.dao.DataCleanupDao
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
    private val appPreferences: AppPreferences,
    private val dataCleanupDao: DataCleanupDao,
) : ViewModel() {

    val versionName: String = BuildConfig.VERSION_NAME

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    private val _refreshingUser = MutableStateFlow(false)
    val refreshingUser: StateFlow<Boolean> = _refreshingUser.asStateFlow()

    private val _syncingAll = MutableStateFlow(false)
    val syncingAll: StateFlow<Boolean> = _syncingAll.asStateFlow()

    private val _clearingCache = MutableStateFlow(false)
    val clearingCache: StateFlow<Boolean> = _clearingCache.asStateFlow()

    val defaultPlaybackSpeed: StateFlow<Float> = appPreferences.defaultPlaybackSpeed
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1f)

    val showPlayerStatus: StateFlow<Boolean> = appPreferences.showPlayerStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoplayNextEpisode: StateFlow<Boolean> = appPreferences.autoplayNextEpisode
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showContinueWatching: StateFlow<Boolean> = appPreferences.showContinueWatching
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showFavorites: StateFlow<Boolean> = appPreferences.showFavorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showRecent: StateFlow<Boolean> = appPreferences.showRecent
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val splashPreload: StateFlow<Boolean> = appPreferences.splashPreload
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showCastAvatars: StateFlow<Boolean> = appPreferences.showCastAvatars
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val tmdbApiKey: StateFlow<String> = appPreferences.tmdbApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val themeMode: StateFlow<String> = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    init {
        viewModelScope.launch {
            val cached = sourcePreferences.userInfo.first()
            if (cached != null) _userInfo.value = cached
            refreshUserInfo()
        }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        viewModelScope.launch { appPreferences.setDefaultPlaybackSpeed(speed) }
    }

    fun setShowPlayerStatus(show: Boolean) {
        viewModelScope.launch { appPreferences.setShowPlayerStatus(show) }
    }

    fun setAutoplayNextEpisode(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAutoplayNextEpisode(enabled) }
    }

    fun setShowContinueWatching(show: Boolean) {
        viewModelScope.launch { appPreferences.setShowContinueWatching(show) }
    }

    fun setShowFavorites(show: Boolean) {
        viewModelScope.launch { appPreferences.setShowFavorites(show) }
    }

    fun setShowRecent(show: Boolean) {
        viewModelScope.launch { appPreferences.setShowRecent(show) }
    }

    fun setSplashPreload(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setSplashPreload(enabled) }
    }

    fun setShowCastAvatars(show: Boolean) {
        viewModelScope.launch { appPreferences.setShowCastAvatars(show) }
    }

    fun setTmdbApiKey(key: String) {
        viewModelScope.launch { appPreferences.setTmdbApiKey(key) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun refreshUserInfo() {
        viewModelScope.launch {
            _refreshingUser.value = true
            val result = getUserInfoUseCase()
            if (result != null) {
                _userInfo.value = result
                sourcePreferences.saveUserInfo(result)
            }
            _refreshingUser.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dataCleanupDao.clearWatchProgress()
        }
    }

    fun clearFavorites() {
        viewModelScope.launch {
            dataCleanupDao.clearFavoriteStreams()
            dataCleanupDao.clearFavoriteVod()
        }
    }

    fun clearCacheAndResync() {
        viewModelScope.launch {
            _clearingCache.value = true
            try {
                dataCleanupDao.clearContentCache()
                val active = manageSourcesUseCase.getActiveSource().first()
                if (active != null) {
                    switchSourceUseCase(active.id)
                }
            } finally {
                _clearingCache.value = false
            }
        }
    }

    fun syncAllSources() {
        viewModelScope.launch {
            _syncingAll.value = true
            try {
                val all = manageSourcesUseCase.getSources().first()
                val activeId = manageSourcesUseCase.getActiveSource().first()?.id
                all.forEach { source ->
                    switchSourceUseCase(source.id)
                }
                if (activeId != null) {
                    switchSourceUseCase(activeId)
                }
            } finally {
                _syncingAll.value = false
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

    suspend fun addSource(name: String, serverUrl: String, port: Int, username: String, password: String, type: String = "xtream"): Long {
        val newId = manageSourcesUseCase.addSource(name, serverUrl, port, username, password, type)
        switchSourceUseCase(newId)
        return newId
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
