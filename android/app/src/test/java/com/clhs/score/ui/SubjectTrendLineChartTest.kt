package com.clhs.score.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectTrendLineChartTest {
    @Test
    fun chartDescriptionIncludesEveryAvailableExamPoint() {
        assertEquals(
            "3 次考試的成績趨勢。第一次段考，數學 70 分；第二次段考，數學 80 分；期末考，數學 90 分",
            buildSubjectTrendChartDescription(
                exams = listOf(
                    "114-1" to "第一次段考",
                    "114-1" to "第二次段考",
                    "114-1" to "期末考",
                ),
                subjectPoints = mapOf("數學" to listOf(70.0, 80.0, 90.0)),
            ),
        )
    }
}
