package com.dream.wowiptv.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.data.local.dao.FavoriteStreamDao
import com.dream.wowiptv.data.local.dao.FavoriteVodDao
import com.dream.wowiptv.data.local.dao.LiveCategoryDao
import com.dream.wowiptv.data.local.dao.LiveStreamDao
import com.dream.wowiptv.data.local.dao.SeriesCategoryDao
import com.dream.wowiptv.data.local.dao.SeriesDao
import com.dream.wowiptv.data.local.dao.VodCategoryDao
import com.dream.wowiptv.data.local.dao.VodStreamDao
import com.dream.wowiptv.data.local.entity.FavoriteStreamEntity
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import com.dream.wowiptv.data.local.entity.LiveStreamEntity
import com.dream.wowiptv.data.local.entity.SeriesEntity
import com.dream.wowiptv.data.local.entity.VodStreamEntity
import com.dream.wowiptv.data.local.entity.WatchProgressEntity
import com.dream.wowiptv.data.local.dao.WatchProgressDao
import com.dream.wowiptv.domain.repository.SourceRepository
import com.dream.wowiptv.domain.usecase.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HomeSection(
    val username: String = "",
    val expiryDate: String = "",
    val liveCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val continueWatching: List<WatchProgressEntity> = emptyList(),
    val favoriteStreams: List<FavoriteStreamEntity> = emptyList(),
    val favoriteMovies: List<FavoriteVodEntity> = emptyList(),
    val favoriteSeries: List<FavoriteVodEntity> = emptyList(),
    val recentLive: List<LiveStreamEntity> = emptyList(),
    val recentMovies: List<VodStreamEntity> = emptyList(),
    val recentSeries: List<SeriesEntity> = emptyList(),
    val liveCategoryNames: Map<Int, String> = emptyMap(),
    val vodCategoryNames: Map<Int, String> = emptyMap(),
    val seriesCategoryNames: Map<Int, String> = emptyMap(),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val liveStreamDao: LiveStreamDao,
    private val vodStreamDao: VodStreamDao,
    private val seriesDao: SeriesDao,
    private val favoriteStreamDao: FavoriteStreamDao,
    private val favoriteVodDao: FavoriteVodDao,
    private val watchProgressDao: WatchProgressDao,
    private val liveCategoryDao: LiveCategoryDao,
    private val vodCategoryDao: VodCategoryDao,
    private val seriesCategoryDao: SeriesCategoryDao,
    private val getUserInfoUseCase: GetUserInfoUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _data = MutableStateFlow(HomeSection())
    val data: StateFlow<HomeSection> = _data.asStateFlow()

    init {
        loadUserInfo()
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first() ?: return@launch

            launch {
                while (true) {
                    val progress = watchProgressDao.getAllBySource(source.id).first()
                    val live = liveStreamDao.getBySource(source.id).first()
                    val vod = vodStreamDao.getBySource(source.id).first()
                    val series = seriesDao.getBySource(source.id).first()
                    android.util.Log.d("HomeVM", "continue watching: ${progress.size} items")
                    val enriched = progress.map { wp ->
                        val icon = when (wp.contentType) {
                            "vod" -> vod.find { it.streamId == wp.contentId.removePrefix("vod_").toIntOrNull() }?.streamIcon
                            "series" -> series.find { it.seriesId == wp.contentId.removePrefix("series_").toIntOrNull() }?.cover
                            "live" -> live.find { it.streamId == wp.contentId.removePrefix("live_").toIntOrNull() }?.streamIcon
                            else -> null
                        }
                        wp.copy(icon = icon ?: wp.icon)
                    }
                    _data.value = _data.value.copy(continueWatching = enriched.take(10))
                    delay(3000)
                }
            }

            while (true) {
                val favStreams = favoriteStreamDao.getAllBySource(source.id).first()
                val favVods = favoriteVodDao.getAllBySource(source.id).first()
                val live = liveStreamDao.getBySource(source.id).first()
                val vod = vodStreamDao.getBySource(source.id).first()
                val series = seriesDao.getBySource(source.id).first()

                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val cutoff = cal.time

                val recentMovies = vod.filter { e ->
                    e.added?.let {
                        try { fmt.parse(it)?.after(cutoff) == true } catch (_: Exception) { false }
                    } ?: false
                }.ifEmpty { vod.take(10) }

                val liveCatMap = liveCategoryDao.getBySource(source.id).first().associate { it.categoryId to it.name }
                val vodCatMap = vodCategoryDao.getBySource(source.id).first().associate { it.categoryId to it.name }
                val seriesCatMap = seriesCategoryDao.getBySource(source.id).first().associate { it.categoryId to it.name }

                _data.value = _data.value.copy(
                    username = source.username,
                    liveCount = live.size,
                    movieCount = vod.size,
                    seriesCount = series.size,
                    favoriteStreams = favStreams,
                    favoriteMovies = favVods.filter { it.type == "movie" },
                    favoriteSeries = favVods.filter { it.type == "series" },
                    recentLive = live.take(10),
                    recentMovies = recentMovies,
                    recentSeries = series.take(10),
                    liveCategoryNames = liveCatMap,
                    vodCategoryNames = vodCatMap,
                    seriesCategoryNames = seriesCatMap
                )
                _isRefreshing.value = false
                delay(2000)
            }
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val info = getUserInfoUseCase()
            if (info != null) {
                _data.value = _data.value.copy(expiryDate = formatExpiry(info.expDate))
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

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first() ?: return@launch
            val progress = watchProgressDao.getAllBySource(source.id).first()
            val live = liveStreamDao.getBySource(source.id).first()
            val vod = vodStreamDao.getBySource(source.id).first()
            val series = seriesDao.getBySource(source.id).first()
            val favStreams = favoriteStreamDao.getAllBySource(source.id).first()
            val favVods = favoriteVodDao.getAllBySource(source.id).first()

            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val cutoff = cal.time

            val recentMovies = vod.filter { e ->
                e.added?.let {
                    try { fmt.parse(it)?.after(cutoff) == true } catch (_: Exception) { false }
                } ?: false
            }.ifEmpty { vod.take(10) }

            val enriched = progress.map { wp ->
                val icon = when (wp.contentType) {
                    "vod" -> vod.find { it.streamId == wp.contentId.removePrefix("vod_").toIntOrNull() }?.streamIcon
                    "series" -> series.find { it.seriesId == wp.contentId.removePrefix("series_").toIntOrNull() }?.cover
                    "live" -> live.find { it.streamId == wp.contentId.removePrefix("live_").toIntOrNull() }?.streamIcon
                    else -> null
                }
                wp.copy(icon = icon ?: wp.icon)
            }

            val liveCatMap = liveCategoryDao.getBySource(source.id).first().associate { it.categoryId to it.name }
            val vodCatMap = vodCategoryDao.getBySource(source.id).first().associate { it.categoryId to it.name }
            val seriesCatMap = seriesCategoryDao.getBySource(source.id).first().associate { it.categoryId to it.name }

            _data.value = HomeSection(
                continueWatching = enriched.take(10),
                favoriteStreams = favStreams,
                favoriteMovies = favVods.filter { it.type == "movie" },
                favoriteSeries = favVods.filter { it.type == "series" },
                recentLive = live.take(10),
                recentMovies = recentMovies,
                recentSeries = series.take(10),
                liveCategoryNames = liveCatMap,
                vodCategoryNames = vodCatMap,
                seriesCategoryNames = seriesCatMap
            )
            _isRefreshing.value = false
        }
    }
}