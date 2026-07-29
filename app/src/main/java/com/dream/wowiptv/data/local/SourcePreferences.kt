package com.dream.wowiptv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sourceDataStore: DataStore<Preferences> by preferencesDataStore(name = "source_preferences")

class SourcePreferences(private val context: Context) {

    companion object {
        private val ACTIVE_SOURCE_ID = longPreferencesKey("active_source_id")
    }

    val activeSourceId: Flow<Long?> = context.sourceDataStore.data.map { preferences ->
        preferences[ACTIVE_SOURCE_ID]
    }

    suspend fun setActiveSourceId(id: Long?) {
        context.sourceDataStore.edit { preferences ->
            if (id != null) {
                preferences[ACTIVE_SOURCE_ID] = id
            } else {
                preferences.remove(ACTIVE_SOURCE_ID)
            }
        }
    }
}
