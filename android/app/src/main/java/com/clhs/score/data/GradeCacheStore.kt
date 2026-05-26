package com.clhs.score.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.gradeDataStore: DataStore<Preferences> by preferencesDataStore(name = "grade_cache")

class GradeCacheStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun saveStructure(studentNo: String, structure: List<YearTermOption>) {
        val key = stringPreferencesKey("structure_$studentNo")
        val serialized = json.encodeToString(structure)
        context.gradeDataStore.edit { prefs ->
            prefs[key] = serialized
        }
    }

    suspend fun loadStructure(studentNo: String): List<YearTermOption>? {
        val key = stringPreferencesKey("structure_$studentNo")
        val serialized = context.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        return try {
            json.decodeFromString<List<YearTermOption>>(serialized)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveGradeReport(studentNo: String, yearValue: String, examValue: String, report: GradeReport) {
        val key = stringPreferencesKey("grade_${studentNo}_${yearValue}_${examValue}")
        val serialized = json.encodeToString(report.toCachedGradeReport())
        context.gradeDataStore.edit { prefs ->
            prefs[key] = serialized
        }
    }

    suspend fun loadGradeReport(studentNo: String, yearValue: String, examValue: String): GradeReport? {
        val key = stringPreferencesKey("grade_${studentNo}_${yearValue}_${examValue}")
        val serialized = context.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        decodeCachedGradeReport(serialized)?.let { return it }
        return decodeLegacyGradeReport(serialized)?.also { migratedReport ->
            context.gradeDataStore.edit { prefs ->
                prefs[key] = json.encodeToString(migratedReport.toCachedGradeReport())
            }
        }
    }

    suspend fun clearStudent(studentNo: String) {
        val structureKey = "structure_$studentNo"
        val gradePrefix = "grade_${studentNo}_"
        context.gradeDataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { key -> key.name == structureKey || key.name.startsWith(gradePrefix) }
                .forEach { key -> prefs.remove(key) }
        }
    }

    suspend fun clearAll() {
        context.gradeDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun decodeCachedGradeReport(serialized: String): GradeReport? =
        runCatching {
            json.decodeFromString<CachedGradeReport>(serialized).toGradeReport()
        }.getOrNull()

    private fun decodeLegacyGradeReport(serialized: String): GradeReport? =
        runCatching {
            json.decodeFromString<GradeReport>(serialized).withoutRawResult()
        }.getOrNull()
}
