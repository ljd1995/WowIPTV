package com.dream.wowiptv.domain.model

data class EpgEntry(
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val isNowPlaying: Boolean
)
