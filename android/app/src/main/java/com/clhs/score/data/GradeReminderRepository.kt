package com.clhs.score.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.gradeReminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "grade_reminder",
)

class GradeReminderRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val state: Flow<GradeReminderState> = appContext.gradeReminderDataStore.data.map { prefs ->
        prefs[KEY_STATE]?.let { serialized ->
            runCatching { json.decodeFromString<GradeReminderState>(serialized) }.getOrNull()
        } ?: GradeReminderState()
    }

    suspend fun loadState(): GradeReminderState = state.first()

    suspend fun saveState(state: GradeReminderState) {
        appContext.gradeReminderDataStore.edit { prefs ->
            prefs[KEY_STATE] = json.encodeToString(state)
        }
    }

    suspend fun clearLatestChangeSet() {
        appContext.gradeReminderDataStore.edit { prefs ->
            val current = prefs[KEY_STATE]?.let { serialized ->
                runCatching { json.decodeFromString<GradeReminderState>(serialized) }.getOrNull()
            } ?: return@edit
            prefs[KEY_STATE] = json.encodeToString(current.copy(latestChangeSet = null))
        }
    }

    suspend fun stop(reason: String) {
        saveState(GradeReminderState(stoppedReason = reason))
    }

    private companion object {
        val KEY_STATE = stringPreferencesKey("state")
    }
}
