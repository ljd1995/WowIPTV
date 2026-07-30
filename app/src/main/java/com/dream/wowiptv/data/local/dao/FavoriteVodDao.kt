package com.dream.wowiptv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dream.wowiptv.data.local.entity.FavoriteVodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteVodDao {

    @Query("SELECT * FROM favorite_vod WHERE sourceId = :sourceId")
    fun getAllBySource(sourceId: Long): Flow<List<FavoriteVodEntity>>

    @Query("SELECT vodId FROM favorite_vod WHERE sourceId = :sourceId AND type = :type")
    suspend fun getFavoriteIdsBySource(sourceId: Long, type: String): List<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_vod WHERE vodId = :vodId AND sourceId = :sourceId)")
    suspend fun isFavorite(vodId: Int, sourceId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteVodEntity)

    @Query("DELETE FROM favorite_vod WHERE vodId = :vodId AND sourceId = :sourceId")
    suspend fun delete(vodId: Int, sourceId: Long)

    @Query("DELETE FROM favorite_vod WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: Long)
}