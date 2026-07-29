package com.dream.wowiptv.domain.model

data class Episode(
    val id: Int,
    val episodeNum: Int,
    val title: String,
    val containerExtension: String,
    val plot: String?,
    val releasedate: String?,
    val durationSecs: Int?
)
