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

class GradeCacheStore(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun saveStructure(studentNo: String, structure: List<YearTermOption>) {
        val key = stringPreferencesKey("structure_$studentNo")
        val serialized = json.encodeToString(structure)
        appContext.gradeDataStore.edit { prefs ->
            prefs[key] = serialized
        }
    }

    suspend fun loadStructure(studentNo: String): List<YearTermOption>? {
        val key = stringPreferencesKey("structure_$studentNo")
        val serialized = appContext.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        return runCatching {
            json.decodeFromString<List<YearTermOption>>(serialized)
        }.onFailure { error ->
            DeveloperDiagnostics.recordEvent(
                context = appContext,
                area = "GradeCache",
                message = "structure cache decode failed: ${error::class.simpleName ?: "unknown"}",
            )
            appContext.gradeDataStore.edit { prefs -> prefs.remove(key) }
        }.getOrNull()
    }

    suspend fun saveGradeReport(studentNo: String, yearValue: String, examValue: String, report: GradeReport) {
        val key = stringPreferencesKey("grade_${studentNo}_${yearValue}_${examValue}")
        val serialized = json.encodeToString(report.toCachedGradeReport())
        appContext.gradeDataStore.edit { prefs ->
            prefs[key] = serialized
        }
    }

    suspend fun loadGradeReport(studentNo: String, yearValue: String, examValue: String): GradeReport? {
        val key = stringPreferencesKey("grade_${studentNo}_${yearValue}_${examValue}")
        val serialized = appContext.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        decodeCachedGradeReport(serialized)?.let { return it }
        val migrated = decodeLegacyGradeReport(serialized)?.also { migratedReport ->
            appContext.gradeDataStore.edit { prefs ->
                prefs[key] = json.encodeToString(migratedReport.toCachedGradeReport())
            }
        }
        if (migrated == null) {
            DeveloperDiagnostics.recordEvent(
                context = appContext,
                area = "GradeCache",
                message = "grade report cache decode failed",
            )
            appContext.gradeDataStore.edit { prefs -> prefs.remove(key) }
        }
        return migrated
    }

    private suspend inline fun <reified T> decodeCacheValue(
        key: Preferences.Key<String>,
        serialized: String,
        label: String,
    ): T? =
        runCatching {
            json.decodeFromString<T>(serialized)
        }.onFailure { error ->
            DeveloperDiagnostics.recordEvent(
                context = appContext,
                area = "GradeCache",
                message = "$label cache decode failed: ${error::class.simpleName ?: "unknown"}",
            )
            appContext.gradeDataStore.edit { prefs -> prefs.remove(key) }
        }.getOrNull()

    suspend fun saveScheduleReport(studentNo: String, yearValue: String, report: ScheduleReport) {
        val key = stringPreferencesKey("schedule_${studentNo}_${yearValue}")
        val latestKey = stringPreferencesKey("schedule_latest_$studentNo")
        val serialized = json.encodeToString(report)
        appContext.gradeDataStore.edit { prefs ->
            prefs[key] = serialized
            prefs[latestKey] = serialized
            prefs[PREF_WIDGET_SCHEDULE_REPORT] = serialized
            prefs[PREF_WIDGET_SCHEDULE_STUDENT_NO] = studentNo
        }
    }

    suspend fun saveWidgetScheduleReport(studentNo: String, report: ScheduleReport) {
        val serialized = json.encodeToString(report)
        appContext.gradeDataStore.edit { prefs ->
            prefs[PREF_WIDGET_SCHEDULE_REPORT] = serialized
            prefs[PREF_WIDGET_SCHEDULE_STUDENT_NO] = studentNo
        }
    }

    suspend fun loadLatestScheduleReport(studentNo: String): ScheduleReport? {
        val key = stringPreferencesKey("schedule_latest_$studentNo")
        val serialized = appContext.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        return decodeCacheValue(key, serialized, "latest schedule")
    }

    suspend fun loadWidgetScheduleReport(): ScheduleReport? {
        val serialized = appContext.gradeDataStore.data.map { prefs -> prefs[PREF_WIDGET_SCHEDULE_REPORT] }.firstOrNull() ?: return null
        return decodeCacheValue(PREF_WIDGET_SCHEDULE_REPORT, serialized, "widget schedule")
    }

    suspend fun loadScheduleReport(studentNo: String, yearValue: String): ScheduleReport? {
        val key = stringPreferencesKey("schedule_${studentNo}_${yearValue}")
        val serialized = appContext.gradeDataStore.data.map { prefs -> prefs[key] }.firstOrNull() ?: return null
        return decodeCacheValue(key, serialized, "schedule")
    }

    suspend fun clearWidgetScheduleReport(studentNo: String? = null) {
        appContext.gradeDataStore.edit { prefs ->
            val owner = prefs[PREF_WIDGET_SCHEDULE_STUDENT_NO]
            if (studentNo == null || owner == studentNo) {
                prefs.remove(PREF_WIDGET_SCHEDULE_REPORT)
                prefs.remove(PREF_WIDGET_SCHEDULE_STUDENT_NO)
            }
        }
    }

    suspend fun clearStudent(studentNo: String) {
        val structureKey = "structure_$studentNo"
        val gradePrefix = "grade_${studentNo}_"
        val schedulePrefix = "schedule_${studentNo}_"
        appContext.gradeDataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { key -> key.name == structureKey || key.name.startsWith(gradePrefix) || key.name.startsWith(schedulePrefix) }
                .forEach { key -> prefs.remove(key) }
            if (prefs[PREF_WIDGET_SCHEDULE_STUDENT_NO] == studentNo) {
                prefs.remove(PREF_WIDGET_SCHEDULE_REPORT)
                prefs.remove(PREF_WIDGET_SCHEDULE_STUDENT_NO)
            }
        }
    }

    suspend fun clearAll() {
        appContext.gradeDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean) {
        appContext.gradeDataStore.edit { prefs ->
            prefs[PREF_WIDGET_SHOW_TEACHER] = showTeacher
            prefs[PREF_WIDGET_SHOW_CLASSROOM] = showClassroom
            prefs[PREF_WIDGET_SHOW_TIME] = showTime
        }
    }

    suspend fun getWidgetPreferences(): Triple<Boolean, Boolean, Boolean> {
        var showTeacher = true
        var showClassroom = true
        var showTime = true

        appContext.gradeDataStore.data.firstOrNull()?.let { prefs ->
            showTeacher = prefs[PREF_WIDGET_SHOW_TEACHER] ?: true
            showClassroom = prefs[PREF_WIDGET_SHOW_CLASSROOM] ?: true
            showTime = prefs[PREF_WIDGET_SHOW_TIME] ?: true
        }

        return Triple(showTeacher, showClassroom, showTime)
    }

    fun widgetPreferencesFlow() = appContext.gradeDataStore.data.map { prefs ->
        Triple(
            prefs[PREF_WIDGET_SHOW_TEACHER] ?: true,
            prefs[PREF_WIDGET_SHOW_CLASSROOM] ?: true,
            prefs[PREF_WIDGET_SHOW_TIME] ?: true
        )
    }

    fun widgetScheduleReportFlow() = appContext.gradeDataStore.data.map { prefs ->
        val serialized = prefs[PREF_WIDGET_SCHEDULE_REPORT] ?: return@map null
        runCatching {
            json.decodeFromString<ScheduleReport>(serialized)
        }.getOrNull()
    }

    private fun decodeCachedGradeReport(serialized: String): GradeReport? =
        runCatching {
            json.decodeFromString<CachedGradeReport>(serialized).toGradeReport()
        }.getOrNull()

    private fun decodeLegacyGradeReport(serialized: String): GradeReport? =
        runCatching {
            json.decodeFromString<GradeReport>(serialized).withoutRawResult()
        }.getOrNull()

    private companion object {
        const val KEY_WIDGET_SCHEDULE_REPORT = "widget_schedule_report"
        const val KEY_WIDGET_SCHEDULE_STUDENT_NO = "widget_schedule_student_no"
        val PREF_WIDGET_SCHEDULE_REPORT = stringPreferencesKey(KEY_WIDGET_SCHEDULE_REPORT)
        val PREF_WIDGET_SCHEDULE_STUDENT_NO = stringPreferencesKey(KEY_WIDGET_SCHEDULE_STUDENT_NO)
        val PREF_WIDGET_SHOW_TEACHER = booleanPreferencesKey("widget_show_teacher")
        val PREF_WIDGET_SHOW_CLASSROOM = booleanPreferencesKey("widget_show_classroom")
        val PREF_WIDGET_SHOW_TIME = booleanPreferencesKey("widget_show_time")
    }
}
