package com.clhs.score.analytics

import android.content.Context

enum class UsageMetric {
    APP_OPEN,
    GRADE_QUERY,
    SCHEDULE_OPEN,
    SUBJECT_TREND_OPEN,
    SCORE_SIMULATOR_OPEN,
    GRADE_EXPORT,
    GRADE_REMINDER_START,
}

data class UsageStatistics(
    val startedAtMillis: Long?,
    val counts: Map<UsageMetric, Long>,
) {
    fun count(metric: UsageMetric): Long = counts[metric] ?: 0L
}

class UsageStatisticsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun recordEvent(name: String, parameters: Map<String, Any?>) {
        val metric = usageMetricForEvent(name, parameters) ?: return
        synchronized(lock) {
            val key = metric.preferenceKey()
            val editor = preferences.edit()
                .putLong(key, preferences.getLong(key, 0L) + 1L)
            if (!preferences.contains(KEY_STARTED_AT)) {
                editor.putLong(KEY_STARTED_AT, System.currentTimeMillis())
            }
            editor.apply()
        }
    }

    fun snapshot(): UsageStatistics = synchronized(lock) {
        UsageStatistics(
            startedAtMillis = preferences.takeIf { it.contains(KEY_STARTED_AT) }
                ?.getLong(KEY_STARTED_AT, 0L),
            counts = UsageMetric.entries.associateWith { metric ->
                preferences.getLong(metric.preferenceKey(), 0L)
            },
        )
    }

    private fun UsageMetric.preferenceKey(): String = "count_${name.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "usage_statistics"
        const val KEY_STARTED_AT = "started_at"
        val lock = Any()
    }
}

internal fun usageMetricForEvent(
    name: String,
    parameters: Map<String, Any?>,
): UsageMetric? = when (name) {
    AnalyticsEvents.APP_OPEN_ROUTE -> UsageMetric.APP_OPEN
    AnalyticsEvents.GRADE_QUERY -> UsageMetric.GRADE_QUERY.onSuccess(parameters)
    AnalyticsEvents.SCHEDULE_OPEN -> UsageMetric.SCHEDULE_OPEN
    AnalyticsEvents.FEATURE_OPEN -> if (
        parameters[AnalyticsParams.FEATURE] == AnalyticsValues.FEATURE_SUBJECT_TREND
    ) {
        UsageMetric.SUBJECT_TREND_OPEN
    } else {
        null
    }
    AnalyticsEvents.SCORE_SIMULATOR_USED -> UsageMetric.SCORE_SIMULATOR_OPEN
    AnalyticsEvents.EXPORT_GRADES -> UsageMetric.GRADE_EXPORT.onSuccess(parameters)
    AnalyticsEvents.GRADE_REMINDER_START -> UsageMetric.GRADE_REMINDER_START.onSuccess(parameters)
    else -> null
}

private fun UsageMetric.onSuccess(parameters: Map<String, Any?>): UsageMetric? =
    takeIf { parameters[AnalyticsParams.RESULT] == AnalyticsValues.RESULT_SUCCESS }
