package com.dream.wowiptv.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.data.local.SourcePreferences
import com.dream.wowiptv.data.local.dao.LiveStreamDao
import com.dream.wowiptv.data.local.dao.SeriesDao
import com.dream.wowiptv.data.local.dao.VodStreamDao
import com.dream.wowiptv.domain.repository.LiveTvRepository
import com.dream.wowiptv.domain.repository.SeriesRepository
import com.dream.wowiptv.domain.repository.SourceRepository
import com.dream.wowiptv.domain.repository.VodRepository
import com.dream.wowiptv.domain.usecase.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class SplashCounts(
    val live: Int = 0,
    val movie: Int = 0,
    val series: Int = 0
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val liveStreamDao: LiveStreamDao,
    private val vodStreamDao: VodStreamDao,
    private val seriesDao: SeriesDao,
    private val liveTvRepository: LiveTvRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val sourcePreferences: SourcePreferences
) : ViewModel() {

    private val _counts = MutableStateFlow<SplashCounts?>(null)
    val counts: StateFlow<SplashCounts?> = _counts.asStateFlow()

    private val _expiry = MutableStateFlow<String?>(null)
    val expiry: StateFlow<String?> = _expiry.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        preload()
    }

    private fun preload() {
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first()
            if (source == null) {
                _ready.value = true
                return@launch
            }

            _counts.value = SplashCounts(
                live = liveStreamDao.getBySource(source.id).first().size,
                movie = vodStreamDao.getBySource(source.id).first().size,
                series = seriesDao.getBySource(source.id).first().size
            )

            launch {
                val info = withTimeoutOrNull(3000) { getUserInfoUseCase() }
                if (info != null) {
                    sourcePreferences.saveUserInfo(info)
                    _expiry.value = formatExpiry(info.expDate)
                } else {
                    _expiry.value = formatExpiry(sourcePreferences.expDate.first())
                }
                _ready.value = true
            }

            launch {
                runCatching { liveTvRepository.refreshAll() }
                runCatching { vodRepository.refreshAll() }
                runCatching { seriesRepository.refreshAll() }
            }
        }
    }

    private fun formatExpiry(dateStr: String?): String {
        if (dateStr == null || dateStr.isBlank()) return ""
        val timestamp = dateStr.toLongOrNull()
        if (timestamp != null) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
            return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
        }
        return dateStr.take(10)
    }
}
