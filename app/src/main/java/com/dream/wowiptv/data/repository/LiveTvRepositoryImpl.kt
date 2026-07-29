package com.dream.wowiptv.data.repository

import com.dream.wowiptv.data.local.dao.EpgDao
import com.dream.wowiptv.data.local.dao.LiveCategoryDao
import com.dream.wowiptv.data.local.dao.LiveStreamDao
import com.dream.wowiptv.data.mapper.toDomain
import com.dream.wowiptv.data.mapper.toEntity
import com.dream.wowiptv.data.remote.xtream.DynamicBaseUrlInterceptor
import com.dream.wowiptv.data.remote.xtream.XtreamApi
import com.dream.wowiptv.domain.model.EpgEntry
import com.dream.wowiptv.domain.model.LiveCategory
import com.dream.wowiptv.domain.model.LiveStream
import com.dream.wowiptv.domain.repository.LiveTvRepository
import com.dream.wowiptv.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveTvRepositoryImpl @Inject constructor(
    private val api: XtreamApi,
    private val liveCategoryDao: LiveCategoryDao,
    private val liveStreamDao: LiveStreamDao,
    private val epgDao: EpgDao,
    private val sourceRepository: SourceRepository,
    private val baseUrlInterceptor: DynamicBaseUrlInterceptor
) : LiveTvRepository {

    override fun getCategories(): Flow<List<LiveCategory>> = flow {
        val source = sourceRepository.getActiveSource().first() ?: return@flow
        emitAll(
            liveCategoryDao.getBySource(source.id).map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    override fun getStreams(categoryId: Int?): Flow<List<LiveStream>> = flow {
        val source = sourceRepository.getActiveSource().first() ?: return@flow
        emitAll(
            liveStreamDao.getBySourceAndCategory(source.id, categoryId).map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    override fun getShortEpg(streamId: Int): Flow<List<EpgEntry>> = flow {
        val source = sourceRepository.getActiveSource().first() ?: return@flow
        emitAll(
            epgDao.getByStream(streamId, source.id).map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    override suspend fun refreshAll() {
        val source = sourceRepository.getActiveSource().first() ?: return
        configureBaseUrl(source.serverUrl, source.port)

        val liveCategories = api.getLiveCategories(source.username, source.password)
        val liveStreams = api.getLiveStreams(source.username, source.password)

        liveCategoryDao.deleteBySource(source.id)
        liveCategoryDao.insertAll(liveCategories.map { it.toDomain().toEntity(source.id) })

        liveStreamDao.deleteBySource(source.id)
        liveStreamDao.insertAll(liveStreams.map { it.toDomain().toEntity(source.id) })
    }

    suspend fun refreshEpg(streamId: Int) {
        val source = sourceRepository.getActiveSource().first() ?: return
        configureBaseUrl(source.serverUrl, source.port)

        val epgResponse = api.getShortEpg(source.username, source.password, streamId)
        val entries = epgResponse.toDomain(streamId)

        epgDao.deleteByStream(streamId, source.id)
        epgDao.insertAll(entries.map { it.toEntity(streamId, source.id) })
    }

    private fun configureBaseUrl(serverUrl: String, port: Int) {
        baseUrlInterceptor.setBaseUrl("http://$serverUrl:$port")
    }
}
