package com.dream.wowiptv.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.data.local.dao.FavoriteStreamDao
import com.dream.wowiptv.data.local.dao.FavoriteVodDao
import com.dream.wowiptv.data.local.dao.LiveStreamDao
import com.dream.wowiptv.data.local.dao.SeriesDao
import com.dream.wowiptv.data.local.dao.VodStreamDao
import com.dream.wowiptv.data.local.entity.FavoriteStreamEntity
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import com.dream.wowiptv.data.local.entity.LiveStreamEntity
import com.dream.wowiptv.data.local.entity.SeriesEntity
import com.dream.wowiptv.data.local.entity.VodStreamEntity
import com.dream.wowiptv.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HomeSection(
    val favoriteStreams: List<FavoriteStreamEntity> = emptyList(),
    val favoriteMovies: List<FavoriteVodEntity> = emptyList(),
    val favoriteSeries: List<FavoriteVodEntity> = emptyList(),
    val recentLive: List<LiveStreamEntity> = emptyList(),
    val recentMovies: List<VodStreamEntity> = emptyList(),
    val recentSeries: List<SeriesEntity> = emptyList(),
    val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val liveStreamDao: LiveStreamDao,
    private val vodStreamDao: VodStreamDao,
    private val seriesDao: SeriesDao,
    private val favoriteStreamDao: FavoriteStreamDao,
    private val favoriteVodDao: FavoriteVodDao
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _data = MutableStateFlow(HomeSection())
    val data: StateFlow<HomeSection> = _data.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first() ?: return@launch
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

                _data.value = HomeSection(
                    favoriteStreams = favStreams,
                    favoriteMovies = favVods.filter { it.type == "movie" },
                    favoriteSeries = favVods.filter { it.type == "series" },
                    recentLive = live.take(10),
                    recentMovies = recentMovies,
                    recentSeries = series.take(10)
                )
                _isRefreshing.value = false
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first() ?: return@launch
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

            _data.value = HomeSection(
                favoriteStreams = favStreams,
                favoriteMovies = favVods.filter { it.type == "movie" },
                favoriteSeries = favVods.filter { it.type == "series" },
                recentLive = live.take(10),
                recentMovies = recentMovies,
                recentSeries = series.take(10)
            )
            _isRefreshing.value = false
        }
    }
}