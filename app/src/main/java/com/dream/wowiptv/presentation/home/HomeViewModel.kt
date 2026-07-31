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
import com.dream.wowiptv.data.local.SourcePreferences
import com.dream.wowiptv.data.local.AppPreferences
import com.dream.wowiptv.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    val continueCategoryNames: Map<String, String> = emptyMap(),
    val vodRating: Map<Int, String> = emptyMap(),
    val seriesRating: Map<Int, String> = emptyMap(),
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
    private val sourcePreferences: SourcePreferences,
    appPreferences: AppPreferences
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val showContinueWatching: StateFlow<Boolean> = appPreferences.showContinueWatching
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showFavorites: StateFlow<Boolean> = appPreferences.showFavorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showRecent: StateFlow<Boolean> = appPreferences.showRecent
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _data = MutableStateFlow(HomeSection())
    val data: StateFlow<HomeSection> = _data.asStateFlow()

    init {
        loadUserInfo()
        loadData()
    }

    private suspend fun loadCategoryMaps(sourceId: Long): Triple<Map<Int, String>, Map<Int, String>, Map<Int, String>> {
        val live = liveCategoryDao.getBySource(sourceId).first().associate { it.categoryId to it.name }
        val vod = vodCategoryDao.getBySource(sourceId).first().associate { it.categoryId to it.name }
        val series = seriesCategoryDao.getBySource(sourceId).first().associate { it.categoryId to it.name }
        return Triple(live, vod, series)
    }

    private fun loadData() {
        viewModelScope.launch {
            while (true) {
                val source = sourceRepository.getActiveSource().first() ?: run {
                    delay(2000)
                    continue
                }

                val (liveCatMap, vodCatMap, seriesCatMap) = loadCategoryMaps(source.id)
                val progress = watchProgressDao.getAllBySource(source.id).first()
                val live = liveStreamDao.getBySource(source.id).first()
                val vod = vodStreamDao.getBySource(source.id).first()
                val series = seriesDao.getBySource(source.id).first()
                val (vodRatingMap, seriesRatingMap) = buildRatingMaps(vod, series)

                val enriched = progress.map { wp ->
                    val icon = when (wp.contentType) {
                        "vod" -> vod.find { it.streamId == wp.contentId.removePrefix("vod_").toIntOrNull() }?.streamIcon
                        "series" -> series.find { it.seriesId == wp.contentId.removePrefix("series_").toIntOrNull() }?.cover
                        "live" -> live.find { it.streamId == wp.contentId.removePrefix("live_").toIntOrNull() }?.streamIcon
                        else -> null
                    }
                    wp.copy(icon = icon ?: wp.icon)
                }
                val catNames = progress.associate { wp ->
                    val id = wp.contentId.removePrefix("vod_").removePrefix("series_").removePrefix("live_").toIntOrNull()
                    val name = when (wp.contentType) {
                        "live" -> live.find { it.streamId == id }?.categoryId?.let { cid -> liveCatMap[cid] }
                        "vod" -> vod.find { it.streamId == id }?.categoryId?.let { cid -> vodCatMap[cid] }
                        "series" -> series.find { it.seriesId == id }?.categoryId?.let { cid -> seriesCatMap[cid] }
                        else -> null
                    }
                    wp.contentId to (name ?: "")
                }.filter { it.value.isNotEmpty() }

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
                    continueWatching = enriched.take(10),
                    continueCategoryNames = catNames,
                    liveCategoryNames = liveCatMap,
                    vodCategoryNames = vodCatMap,
                    seriesCategoryNames = seriesCatMap,
                    vodRating = vodRatingMap,
                    seriesRating = seriesRatingMap
                )
                _isRefreshing.value = false
                delay(2000)
            }
        }
    }

    private fun buildRatingMaps(vod: List<VodStreamEntity>, series: List<SeriesEntity>): Pair<Map<Int, String>, Map<Int, String>> {
        val vodMap = vod.filter { !it.rating.isNullOrBlank() }.associate { it.streamId to it.rating!! }
        val seriesMap = series.filter { !it.rating.isNullOrBlank() }.associate { it.seriesId to it.rating!! }
        return vodMap to seriesMap
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first()
            val expiry = sourcePreferences.expDate.first()
            _data.value = _data.value.copy(
                username = source?.username ?: "",
                expiryDate = formatExpiry(expiry)
            )
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
            val (vodRatingMap, seriesRatingMap) = buildRatingMaps(vod, series)

            val catNames = progress.associate { wp ->
                val id = wp.contentId.removePrefix("vod_").removePrefix("series_").removePrefix("live_").toIntOrNull()
                val name = when (wp.contentType) {
                    "live" -> live.find { it.streamId == id }?.categoryId?.let { cid -> liveCatMap[cid] }
                    "vod" -> vod.find { it.streamId == id }?.categoryId?.let { cid -> vodCatMap[cid] }
                    "series" -> series.find { it.seriesId == id }?.categoryId?.let { cid -> seriesCatMap[cid] }
                    else -> null
                }
                wp.contentId to (name ?: "")
            }.filter { it.value.isNotEmpty() }

            _data.value = _data.value.copy(
                continueWatching = enriched.take(10),
                continueCategoryNames = catNames,
                favoriteStreams = favStreams,
                favoriteMovies = favVods.filter { it.type == "movie" },
                favoriteSeries = favVods.filter { it.type == "series" },
                recentLive = live.take(10),
                recentMovies = recentMovies,
                recentSeries = series.take(10),
                liveCategoryNames = liveCatMap,
                vodCategoryNames = vodCatMap,
                seriesCategoryNames = seriesCatMap,
                vodRating = vodRatingMap,
                seriesRating = seriesRatingMap
            )
            _isRefreshing.value = false
        }
    }
}