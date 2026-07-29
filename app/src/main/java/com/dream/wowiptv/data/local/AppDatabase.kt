package com.dream.wowiptv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dream.wowiptv.data.local.dao.SourceDao
import com.dream.wowiptv.data.local.entity.SourceEntity

@Database(entities = [SourceEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
}
