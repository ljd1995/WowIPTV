package com.dream.wowiptv.domain.usecase

import com.dream.wowiptv.data.remote.xtream.XtreamApi
import com.dream.wowiptv.domain.model.UserInfo
import com.dream.wowiptv.domain.repository.SourceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetUserInfoUseCase @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val xtreamApi: XtreamApi
) {
    suspend operator fun invoke(): UserInfo? {
        val source = sourceRepository.getActiveSource().first() ?: return null
        kotlin.runCatching {
            val response = xtreamApi.authenticate(source.username, source.password)
            val info = response.userInfo
            if (info != null) {
                return UserInfo(
                    username = info.username,
                    expDate = info.expDate,
                    maxConnections = info.maxConnections,
                    allowedOutputFormats = info.allowedOutputFormats
                )
            }
        }
        return null
    }
}