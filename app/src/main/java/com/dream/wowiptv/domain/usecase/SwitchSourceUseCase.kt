package com.dream.wowiptv.domain.usecase

import com.dream.wowiptv.domain.repository.LiveTvRepository
import com.dream.wowiptv.domain.repository.SeriesRepository
import com.dream.wowiptv.domain.repository.SourceRepository
import com.dream.wowiptv.domain.repository.VodRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

class SwitchSourceUseCase @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val liveTvRepository: LiveTvRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke(sourceId: Long) {
        supervisorScope {
            sourceRepository.switchSource(sourceId)
            launch { runCatching { liveTvRepository.refreshAll() } }
            launch { runCatching { vodRepository.refreshAll() } }
            launch { runCatching { seriesRepository.refreshAll() } }
        }
    }
}
