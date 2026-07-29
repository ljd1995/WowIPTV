package com.dream.wowiptv.di

import android.content.Context
import androidx.room.Room
import com.dream.wowiptv.data.local.AppDatabase
import com.dream.wowiptv.data.local.dao.EpgDao
import com.dream.wowiptv.data.local.dao.LiveCategoryDao
import com.dream.wowiptv.data.local.dao.LiveStreamDao
import com.dream.wowiptv.data.local.dao.SeriesCategoryDao
import com.dream.wowiptv.data.local.dao.SeriesDao
import com.dream.wowiptv.data.local.dao.SourceDao
import com.dream.wowiptv.data.local.dao.VodCategoryDao
import com.dream.wowiptv.data.local.dao.VodInfoDao
import com.dream.wowiptv.data.local.dao.VodStreamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wowiptv.db"
        ).fallbackToDestructiveMigration(false).build()
    }

    @Provides fun provideSourceDao(db: AppDatabase): SourceDao = db.sourceDao()
    @Provides fun provideLiveCategoryDao(db: AppDatabase): LiveCategoryDao = db.liveCategoryDao()
    @Provides fun provideLiveStreamDao(db: AppDatabase): LiveStreamDao = db.liveStreamDao()
    @Provides fun provideEpgDao(db: AppDatabase): EpgDao = db.epgDao()
    @Provides fun provideVodCategoryDao(db: AppDatabase): VodCategoryDao = db.vodCategoryDao()
    @Provides fun provideVodStreamDao(db: AppDatabase): VodStreamDao = db.vodStreamDao()
    @Provides fun provideVodInfoDao(db: AppDatabase): VodInfoDao = db.vodInfoDao()
    @Provides fun provideSeriesCategoryDao(db: AppDatabase): SeriesCategoryDao = db.seriesCategoryDao()
    @Provides fun provideSeriesDao(db: AppDatabase): SeriesDao = db.seriesDao()
}
