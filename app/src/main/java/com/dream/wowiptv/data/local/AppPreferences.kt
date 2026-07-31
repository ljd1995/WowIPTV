package com.dream.wowiptv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        private val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        private val SHOW_PLAYER_STATUS = booleanPreferencesKey("show_player_status")
        private val AUTOPLAY_NEXT_EPISODE = booleanPreferencesKey("autoplay_next_episode")
        private val SHOW_CONTINUE_WATCHING = booleanPreferencesKey("show_continue_watching")
        private val SHOW_FAVORITES = booleanPreferencesKey("show_favorites")
        private val SHOW_RECENT = booleanPreferencesKey("show_recent")
        private val SPLASH_PRELOAD = booleanPreferencesKey("splash_preload")
    }

    val defaultPlaybackSpeed: Flow<Float> = context.appDataStore.data.map { it[DEFAULT_PLAYBACK_SPEED] ?: 1f }

    val showPlayerStatus: Flow<Boolean> = context.appDataStore.data.map { it[SHOW_PLAYER_STATUS] ?: true }

    val autoplayNextEpisode: Flow<Boolean> = context.appDataStore.data.map { it[AUTOPLAY_NEXT_EPISODE] ?: true }

    val showContinueWatching: Flow<Boolean> = context.appDataStore.data.map { it[SHOW_CONTINUE_WATCHING] ?: true }

    val showFavorites: Flow<Boolean> = context.appDataStore.data.map { it[SHOW_FAVORITES] ?: true }

    val showRecent: Flow<Boolean> = context.appDataStore.data.map { it[SHOW_RECENT] ?: true }

    val splashPreload: Flow<Boolean> = context.appDataStore.data.map { it[SPLASH_PRELOAD] ?: true }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.appDataStore.edit { it[DEFAULT_PLAYBACK_SPEED] = speed }
    }

    suspend fun setShowPlayerStatus(show: Boolean) {
        context.appDataStore.edit { it[SHOW_PLAYER_STATUS] = show }
    }

    suspend fun setAutoplayNextEpisode(enabled: Boolean) {
        context.appDataStore.edit { it[AUTOPLAY_NEXT_EPISODE] = enabled }
    }

    suspend fun setShowContinueWatching(show: Boolean) {
        context.appDataStore.edit { it[SHOW_CONTINUE_WATCHING] = show }
    }

    suspend fun setShowFavorites(show: Boolean) {
        context.appDataStore.edit { it[SHOW_FAVORITES] = show }
    }

    suspend fun setShowRecent(show: Boolean) {
        context.appDataStore.edit { it[SHOW_RECENT] = show }
    }

    suspend fun setSplashPreload(enabled: Boolean) {
        context.appDataStore.edit { it[SPLASH_PRELOAD] = enabled }
    }
}
