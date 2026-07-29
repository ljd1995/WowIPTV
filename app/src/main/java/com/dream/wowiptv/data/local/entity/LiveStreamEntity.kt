package com.dream.wowiptv.data.local.entity

import androidx.room.Entity

@Entity(tableName = "live_streams", primaryKeys = ["streamId", "sourceId"])
data class LiveStreamEntity(
    val streamId: Int,
    val name: String,
    val streamIcon: String?,
    val epgChannelId: String?,
    val categoryId: Int?,
    val tvArchive: Boolean,
    val sourceId: Long
)
