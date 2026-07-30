package com.dream.wowiptv.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dream.wowiptv.data.local.dao.EpgDao
import com.dream.wowiptv.data.local.dao.FavoriteStreamDao
import com.dream.wowiptv.data.local.dao.FavoriteVodDao
import com.dream.wowiptv.data.local.dao.LiveCategoryDao
import com.dream.wowiptv.data.local.dao.LiveStreamDao
import com.dream.wowiptv.data.local.dao.SeriesCategoryDao
import com.dream.wowiptv.data.local.dao.SeriesDao
import com.dream.wowiptv.data.local.dao.SourceDao
import com.dream.wowiptv.data.local.dao.VodCategoryDao
import com.dream.wowiptv.data.local.dao.VodInfoDao
import com.dream.wowiptv.data.local.dao.VodStreamDao

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE series_list ADD COLUMN releaseDate TEXT")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS favorite_vod (
                vodId INTEGER NOT NULL,
                sourceId INTEGER NOT NULL,
                type TEXT NOT NULL,
                name TEXT NOT NULL,
                icon TEXT,
                categoryId INTEGER NOT NULL,
                PRIMARY KEY(vodId, sourceId)
            )"""
        )
    }
}