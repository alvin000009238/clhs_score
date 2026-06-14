package com.clhs.score.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsParameterSanitizerTest {
    @Test
    fun sanitizerKeepsOnlyAllowedAnonymousParameters() {
        val sanitized = AnalyticsParameterSanitizer.sanitize(
            mapOf(
                AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                AnalyticsParams.ENABLED to true,
                AnalyticsParams.YEAR_COUNT to 2,
                "studentNo" to "S123",
                "cookies" to "SESSION=secret",
                "apiToken" to "secret",
                "url" to "https://example.com/private",
                "rawResult" to "raw",
            ),
        )

        assertEquals(AnalyticsValues.RESULT_SUCCESS, sanitized[AnalyticsParams.RESULT])
        assertEquals(1L, sanitized[AnalyticsParams.ENABLED])
        assertEquals(2L, sanitized[AnalyticsParams.YEAR_COUNT])
        assertFalse(sanitized.containsKey("studentNo"))
        assertFalse(sanitized.containsKey("cookies"))
        assertFalse(sanitized.containsKey("apiToken"))
        assertFalse(sanitized.containsKey("url"))
        assertFalse(sanitized.containsKey("rawResult"))
    }

    @Test
    fun countBucketAvoidsExactLargeCounts() {
        assertEquals("0", AnalyticsParameterSanitizer.countBucket(0))
        assertEquals("1", AnalyticsParameterSanitizer.countBucket(1))
        assertEquals("2_3", AnalyticsParameterSanitizer.countBucket(3))
        assertEquals("4_6", AnalyticsParameterSanitizer.countBucket(6))
        assertEquals("7_10", AnalyticsParameterSanitizer.countBucket(10))
        assertEquals("11_plus", AnalyticsParameterSanitizer.countBucket(42))
    }
}
