package com.dream.wowiptv.domain.usecase

import com.dream.wowiptv.domain.repository.LiveTvRepository
import javax.inject.Inject

class RefreshAllEpgUseCase @Inject constructor(
    private val repository: LiveTvRepository
) {
    suspend operator fun invoke() = repository.refreshAllEpg()
}
