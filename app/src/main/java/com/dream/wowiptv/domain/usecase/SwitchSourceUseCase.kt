package com.dream.wowiptv.domain.usecase

import com.dream.wowiptv.domain.repository.LiveTvRepository
import com.dream.wowiptv.domain.repository.SeriesRepository
import com.dream.wowiptv.domain.repository.SourceRepository
import com.dream.wowiptv.domain.repository.VodRepository
import javax.inject.Inject

class SwitchSourceUseCase @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val liveTvRepository: LiveTvRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke(sourceId: Long) {
        sourceRepository.switchSource(sourceId)
        liveTvRepository.refreshAll()
        vodRepository.refreshAll()
        seriesRepository.refreshAll()
    }
}
