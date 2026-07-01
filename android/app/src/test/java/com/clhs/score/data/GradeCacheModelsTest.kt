package com.clhs.score.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeCacheModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun cachedReportOmitsRawResultButRestoresUiFields() {
        val report = FakeData.latestReport().copy(
            rawResult = JsonObject(
                mapOf("heavy_debug_payload" to JsonPrimitive("raw-json-that-should-not-be-cached")),
            ),
        )

        val serialized = json.encodeToString(report.toCachedGradeReport())
        val restored = json.decodeFromString<CachedGradeReport>(serialized).toGradeReport()

        assertFalse(serialized.contains("rawResult"))
        assertFalse(serialized.contains("heavy_debug_payload"))
        assertEquals(report.message, restored.message)
        assertEquals(report.studentInfo, restored.studentInfo)
        assertEquals(report.examSummary, restored.examSummary)
        assertEquals(report.subjects, restored.subjects)
        assertEquals(report.standards, restored.standards)
        assertTrue(restored.rawResult.isEmpty())
    }

    @Test
    fun oldCachedReportVersionIsExpired() {
        val cached = FakeData.latestReport().toCachedGradeReport().copy(cacheVersion = 1)

        assertNull(cached.toCurrentGradeReport())
    }
}
