package com.dream.wowiptv.data.repository

import com.dream.wowiptv.data.local.dao.SeriesCategoryDao
import com.dream.wowiptv.data.local.dao.SeriesDao
import com.dream.wowiptv.data.mapper.toDomain
import com.dream.wowiptv.data.mapper.toEntity
import com.dream.wowiptv.data.remote.xtream.DynamicBaseUrlInterceptor
import com.dream.wowiptv.data.remote.xtream.XtreamApi
import com.dream.wowiptv.domain.model.SeriesCategory
import com.dream.wowiptv.domain.model.SeriesInfo
import com.dream.wowiptv.domain.model.SeriesItem
import com.dream.wowiptv.domain.repository.SeriesRepository
import com.dream.wowiptv.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val api: XtreamApi,
    private val seriesCategoryDao: SeriesCategoryDao,
    private val seriesDao: SeriesDao,
    private val sourceRepository: SourceRepository,
    private val baseUrlInterceptor: DynamicBaseUrlInterceptor
) : SeriesRepository {

    override fun getCategories(): Flow<List<SeriesCategory>> = flow {
        val source = sourceRepository.getActiveSource().first() ?: return@flow
        emitAll(
            seriesCategoryDao.getBySource(source.id).map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    override fun getSeries(categoryId: Int?): Flow<List<SeriesItem>> = flow {
        val source = sourceRepository.getActiveSource().first() ?: return@flow
        emitAll(
            seriesDao.getBySourceAndCategory(source.id, categoryId).map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    override suspend fun getInfo(seriesId: Int): SeriesInfo {
        val source = sourceRepository.getActiveSource().first() ?: error("No active source")
        configureBaseUrl(source.serverUrl, source.port)
        val dto = api.getSeriesInfo(username = source.username, password = source.password, seriesId = seriesId)
        return dto.toDomain()
    }

    override suspend fun refreshAll() {
        val source = sourceRepository.getActiveSource().first() ?: return
        configureBaseUrl(source.serverUrl, source.port)

        val categories = api.getSeriesCategories(source.username, source.password)
        val seriesList = api.getSeries(source.username, source.password)

        seriesCategoryDao.deleteBySource(source.id)
        seriesCategoryDao.insertAll(categories.map { it.toDomain().toEntity(source.id) })

        seriesDao.deleteSeriesBySource(source.id)
        seriesDao.insertAllSeries(seriesList.map { it.toDomain().toEntity(source.id) })
    }

    private fun configureBaseUrl(serverUrl: String, port: Int) {
        baseUrlInterceptor.setBaseUrl("http://$serverUrl:$port")
    }
}
