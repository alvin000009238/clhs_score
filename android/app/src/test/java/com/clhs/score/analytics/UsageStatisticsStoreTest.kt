package com.clhs.score.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageStatisticsStoreTest {
    @Test
    fun mapsOnlyRequestedUsageEvents() {
        val success = mapOf(AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS)
        val failure = mapOf(AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE)

        assertEquals(UsageMetric.APP_OPEN, usageMetricForEvent(AnalyticsEvents.APP_OPEN_ROUTE, emptyMap()))
        assertEquals(UsageMetric.GRADE_QUERY, usageMetricForEvent(AnalyticsEvents.GRADE_QUERY, success))
        assertEquals(UsageMetric.SCHEDULE_OPEN, usageMetricForEvent(AnalyticsEvents.SCHEDULE_OPEN, emptyMap()))
        assertEquals(
            UsageMetric.SUBJECT_TREND_OPEN,
            usageMetricForEvent(
                AnalyticsEvents.FEATURE_OPEN,
                mapOf(AnalyticsParams.FEATURE to AnalyticsValues.FEATURE_SUBJECT_TREND),
            ),
        )
        assertEquals(
            UsageMetric.SCORE_SIMULATOR_OPEN,
            usageMetricForEvent(AnalyticsEvents.SCORE_SIMULATOR_USED, emptyMap()),
        )
        assertEquals(UsageMetric.GRADE_EXPORT, usageMetricForEvent(AnalyticsEvents.EXPORT_GRADES, success))
        assertEquals(
            UsageMetric.GRADE_REMINDER_START,
            usageMetricForEvent(AnalyticsEvents.GRADE_REMINDER_START, success),
        )

        assertNull(usageMetricForEvent(AnalyticsEvents.GRADE_QUERY, failure))
        assertNull(usageMetricForEvent(AnalyticsEvents.EXPORT_GRADES, failure))
        assertNull(usageMetricForEvent(AnalyticsEvents.GRADE_REMINDER_START, failure))
        assertNull(
            usageMetricForEvent(
                AnalyticsEvents.FEATURE_OPEN,
                mapOf(AnalyticsParams.FEATURE to AnalyticsValues.FEATURE_SETTINGS),
            ),
        )
        assertNull(usageMetricForEvent(AnalyticsEvents.LOGOUT, emptyMap()))
    }
}
