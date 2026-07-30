package com.dream.wowiptv.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dream.wowiptv.data.local.dao.FavoriteStreamDao
import com.dream.wowiptv.data.local.dao.FavoriteVodDao
import com.dream.wowiptv.data.local.entity.FavoriteStreamEntity
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import com.dream.wowiptv.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeSection(
    val favoriteStreams: List<FavoriteStreamEntity> = emptyList(),
    val favoriteMovies: List<FavoriteVodEntity> = emptyList(),
    val favoriteSeries: List<FavoriteVodEntity> = emptyList(),
    val recentAll: List<Any> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val favoriteStreamDao: FavoriteStreamDao,
    private val favoriteVodDao: FavoriteVodDao
) : ViewModel() {

    private val _data = MutableStateFlow(HomeSection())
    val data: StateFlow<HomeSection> = _data.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val source = sourceRepository.getActiveSource().first() ?: return@launch
            val favStreams = favoriteStreamDao.getAllBySource(source.id).first()
            val allFavVod = favoriteVodDao.getAllBySource(source.id).first()
            val favMovies = allFavVod.filter { it.type == "movie" }
            val favSeries = allFavVod.filter { it.type == "series" }
            _data.value = HomeSection(
                favoriteStreams = favStreams,
                favoriteMovies = favMovies,
                favoriteSeries = favSeries,
                recentAll = favMovies + favSeries
            )
        }
    }
}