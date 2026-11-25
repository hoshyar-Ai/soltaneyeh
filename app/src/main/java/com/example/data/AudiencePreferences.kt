package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AudienceLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soltaniyeh_prefs")

class AudiencePreferences(private val context: Context) {
    private val audienceKey = stringPreferencesKey("audience_level")
    private val runtimeOpenAiKey = stringPreferencesKey("runtime_openai_api_key")

    val audienceLevelFlow: Flow<AudienceLevel> = context.dataStore.data.map { prefs ->
        AudienceLevel.fromId(prefs[audienceKey])
    }

    val runtimeOpenAiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[runtimeOpenAiKey].orEmpty().trim()
    }

    suspend fun setAudienceLevel(level: AudienceLevel) {
        context.dataStore.edit { prefs ->
            prefs[audienceKey] = level.id
        }
    }

    suspend fun setRuntimeOpenAiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            val trimmed = apiKey.trim()
            if (trimmed.isEmpty()) {
                prefs.remove(runtimeOpenAiKey)
            } else {
                prefs[runtimeOpenAiKey] = trimmed
            }
        }
    }

    suspend fun clearRuntimeOpenAiKey() {
        context.dataStore.edit { prefs ->
            prefs.remove(runtimeOpenAiKey)
        }
    }
}

