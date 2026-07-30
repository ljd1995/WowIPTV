package com.dream.wowiptv.domain.usecase

import com.dream.wowiptv.data.local.dao.FavoriteVodDao
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import com.dream.wowiptv.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CreateFavoriteUseCase @Inject constructor(
    private val favoriteVodDao: FavoriteVodDao,
    private val sourceRepository: SourceRepository
) {
    private suspend fun getSourceId(): Long {
        return sourceRepository.getActiveSource().first()?.id ?: error("No active source")
    }

    suspend fun toggleMovie(vodId: Int, name: String, icon: String?, categoryId: Int) {
        val sourceId = getSourceId()
        if (favoriteVodDao.isFavorite(vodId, sourceId)) {
            favoriteVodDao.delete(vodId, sourceId)
        } else {
            favoriteVodDao.insert(FavoriteVodEntity(vodId, sourceId, "movie", name, icon, categoryId))
        }
    }

    suspend fun toggleSeries(seriesId: Int, name: String, icon: String?, categoryId: Int) {
        val sourceId = getSourceId()
        if (favoriteVodDao.isFavorite(seriesId, sourceId)) {
            favoriteVodDao.delete(seriesId, sourceId)
        } else {
            favoriteVodDao.insert(FavoriteVodEntity(seriesId, sourceId, "series", name, icon, categoryId))
        }
    }

    fun getMovieFavoriteIds(): Flow<Set<Int>> {
        return sourceRepository.getActiveSource().map { source ->
            if (source == null) emptySet()
            else favoriteVodDao.getFavoriteIdsBySource(source.id, "movie").toSet()
        }
    }

    fun getSeriesFavoriteIds(): Flow<Set<Int>> {
        return sourceRepository.getActiveSource().map { source ->
            if (source == null) emptySet()
            else favoriteVodDao.getFavoriteIdsBySource(source.id, "series").toSet()
        }
    }
}