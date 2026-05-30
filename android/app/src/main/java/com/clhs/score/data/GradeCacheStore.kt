package com.clhs.score.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
        return runCatching {
            json.decodeFromString<List<YearTermOption>>(serialized)
        }.onFailure { error ->
            DeveloperDiagnostics.recordEvent(
                context = context,
                area = "GradeCache",
                message = "structure cache decode failed: ${error::class.simpleName ?: "unknown"}",
            )
        }.getOrNull()
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
        val migrated = decodeLegacyGradeReport(serialized)?.also { migratedReport ->
            context.gradeDataStore.edit { prefs ->
                prefs[key] = json.encodeToString(migratedReport.toCachedGradeReport())
            }
        }
        if (migrated == null) {
            DeveloperDiagnostics.recordEvent(
                context = context,
                area = "GradeCache",
                message = "grade report cache decode failed",
            )
        }
        return migrated
    }

    private inline fun <reified T> decodeCacheValue(serialized: String, label: String): T? =
        runCatching {
            json.decodeFromString<T>(serialized)
        }.onFailure { error ->
            DeveloperDiagnostics.recordEvent(
                context = context,
                area = "GradeCache",
                message = "$label cache decode failed: ${error::class.simpleName ?: "unknown"}",
            )
        }.getOrNull()

    suspend fun saveScheduleReport(studentNo: String, yearValue: String, report: ScheduleReport) {
        val key = stringPreferencesKey("schedule_${studentNo}_${yearValue}")
        val latestKey = stringPreferencesKey("schedule_latest_$studentNo")
        val serialized = json.encodeToString(report)
        context.gradeDataStore.edit { prefs ->
            prefs[key] = serialized
            prefs[latestKey] = serialized
        }
    }

    suspend fun loadLatestScheduleReport(studentNo: String): ScheduleReport? {
        val key = stringPreferencesKey("schedule_latest_$studentNo")
        val serialized = context.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        return decodeCacheValue(serialized, "latest schedule")
    }

    suspend fun loadScheduleReport(studentNo: String, yearValue: String): ScheduleReport? {
        val key = stringPreferencesKey("schedule_${studentNo}_${yearValue}")
        val serialized = context.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        return decodeCacheValue(serialized, "schedule")
    }

    suspend fun clearStudent(studentNo: String) {
        val structureKey = "structure_$studentNo"
        val gradePrefix = "grade_${studentNo}_"
        val schedulePrefix = "schedule_${studentNo}_"
        context.gradeDataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { key -> key.name == structureKey || key.name.startsWith(gradePrefix) || key.name.startsWith(schedulePrefix) }
                .forEach { key -> prefs.remove(key) }
        }
    }

    suspend fun clearAll() {
        context.gradeDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean) {
        val showTeacherKey = booleanPreferencesKey("widget_show_teacher")
        val showClassroomKey = booleanPreferencesKey("widget_show_classroom")
        val showTimeKey = booleanPreferencesKey("widget_show_time")
        context.gradeDataStore.edit { prefs ->
            prefs[showTeacherKey] = showTeacher
            prefs[showClassroomKey] = showClassroom
            prefs[showTimeKey] = showTime
        }
    }

    suspend fun getWidgetPreferences(): Triple<Boolean, Boolean, Boolean> {
        val showTeacherKey = booleanPreferencesKey("widget_show_teacher")
        val showClassroomKey = booleanPreferencesKey("widget_show_classroom")
        val showTimeKey = booleanPreferencesKey("widget_show_time")

        var showTeacher = true
        var showClassroom = true
        var showTime = true

        context.gradeDataStore.data.firstOrNull()?.let { prefs ->
            showTeacher = prefs[showTeacherKey] ?: true
            showClassroom = prefs[showClassroomKey] ?: true
            showTime = prefs[showTimeKey] ?: true
        }

        return Triple(showTeacher, showClassroom, showTime)
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
