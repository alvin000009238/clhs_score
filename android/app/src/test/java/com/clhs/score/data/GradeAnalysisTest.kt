package com.clhs.score.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeAnalysisTest {
    @Test
    fun percentileFormatsRankAsTopPercent() {
        assertEquals("前 35%", percentile(13.0, 37)?.percentLabel)
        assertEquals("前 27%", percentile(60.0, 219)?.percentLabel)
    }

    @Test
    fun strengthAndWeaknessAreSortedByClassAverageDiff() {
        val analysis = buildGradeAnalysis(MockGradeSystem.generateReport(StudentScenario.NORMAL))

        assertEquals("數學", analysis.strengths.first().subjectName)
        assertEquals("社會", analysis.weaknesses.first().subjectName)
    }

    @Test
    fun summaryTextHandlesRankAndSubjectHighlights() {
        val summary = buildGradeAnalysis(MockGradeSystem.generateReport(StudentScenario.NORMAL)).summaryText

        assertTrue(summary.contains("本次班排 15/38"))
        assertTrue(summary.contains("優勢科目為數學"))
        assertTrue(summary.contains("待加強為社會"))
    }

    @Test
    fun previousExamLookupStaysInsideSameTerm() {
        val year = YearTermOption(
            text = "114上",
            value = "114_1",
            exams = listOf(
                ExamOption("第一次段考", "E1"),
                ExamOption("第二次段考", "E2"),
                ExamOption("期末考", "E3"),
            ),
        )

        assertEquals("E2", year.previousExamOf("E3")?.value)
        assertNull(year.previousExamOf("E1"))
        assertNull(year.previousExamOf("missing"))
    }

    @Test
    fun previousExamsLookupReturnsAtMostTwoInChronologicalOrder() {
        val year = YearTermOption(
            text = "114上",
            value = "114_1",
            exams = listOf(
                ExamOption("第一次段考", "E1"),
                ExamOption("第二次段考", "E2"),
                ExamOption("期末考", "E3"),
            ),
        )

        assertEquals(listOf("E1", "E2"), year.previousExamsOf("E3").map { it.value })
        assertEquals(listOf("E1"), year.previousExamsOf("E2").map { it.value })
        assertTrue(year.previousExamsOf("E1").isEmpty())
    }

    @Test
    fun simulatorHistoryUsesLastThreePreviousExamsInSameTerm() {
        val structure = listOf(
            YearTermOption(
                text = "114上",
                value = "114_1",
                exams = listOf(
                    ExamOption("第一次段考", "E1"),
                    ExamOption("第二次段考", "E2"),
                    ExamOption("第三次段考", "E3"),
                    ExamOption("第四次段考", "E4"),
                    ExamOption("期末考", "E5"),
                ),
            ),
        )

        val source = structure.simulationHistorySource("114_1", "E5")

        assertEquals(false, source?.usesPreviousTerm)
        assertEquals(listOf("E2", "E3", "E4"), source?.exams?.map { it.value })
    }

    @Test
    fun simulatorHistoryCanUsePreviousTermWhenSameTermIsEmpty() {
        val structure = listOf(
            YearTermOption(
                text = "113下",
                value = "113_2",
                exams = listOf(
                    ExamOption("第一次段考", "P1"),
                    ExamOption("期末考", "P2"),
                ),
            ),
            YearTermOption(
                text = "114上",
                value = "114_1",
                exams = listOf(ExamOption("第一次段考", "E1")),
            ),
        )

        val source = structure.simulationHistorySource("114_1", "E1")

        assertEquals(true, source?.usesPreviousTerm)
        assertEquals("113_2", source?.yearTerm?.value)
        assertEquals(listOf("P1", "P2"), source?.exams?.map { it.value })
    }

    @Test
    fun simulatorHistoryUsesPreviousThreeExamsAcrossTerms() {
        val structure = listOf(
            YearTermOption(
                text = "113上",
                value = "113_1",
                exams = listOf(ExamOption("期末考", "A3")),
            ),
            YearTermOption(
                text = "113下",
                value = "113_2",
                exams = listOf(
                    ExamOption("第一次段考", "P1"),
                    ExamOption("期末考", "P2"),
                ),
            ),
            YearTermOption(
                text = "114上",
                value = "114_1",
                exams = listOf(ExamOption("第一次段考", "E1")),
            ),
        )

        val source = structure.simulationHistorySource("114_1", "E1")

        assertEquals(true, source?.usesPreviousTerm)
        assertEquals(listOf("113_1", "113_2", "113_2"), source?.historyExams?.map { it.yearValue })
        assertEquals(listOf("A3", "P1", "P2"), source?.historyExams?.map { it.examValue })
    }

    @Test
    fun sameTermHistoryDoesNotFallBackToPreviousTerm() {
        val structure = listOf(
            YearTermOption(
                text = "113下",
                value = "113_2",
                exams = listOf(
                    ExamOption("第一次段考", "P1"),
                    ExamOption("期末考", "P2"),
                ),
            ),
            YearTermOption(
                text = "114上",
                value = "114_1",
                exams = listOf(ExamOption("期中考1", "E1")),
            ),
        )

        assertNull(structure.sameTermHistorySource("114_1", "E1"))
        assertEquals("113_2", structure.simulationHistorySource("114_1", "E1")?.yearTerm?.value)
    }

    @Test
    fun latestYearTermAndExamPreferNewestOptions() {
        val structure = listOf(
            YearTermOption(
                text = "113下",
                value = "113_2",
                exams = listOf(ExamOption("第一次段考", "E1")),
            ),
            YearTermOption(
                text = "114上",
                value = "114_1",
                exams = listOf(
                    ExamOption("第一次段考", "E1"),
                    ExamOption("第二次段考", "E2"),
                    ExamOption("期末考", "E3"),
                ),
            ),
            YearTermOption(
                text = "113上",
                value = "113_1",
                exams = listOf(ExamOption("期末考", "E3")),
            ),
        )

        val latestYear = structure.latestYearTerm()

        assertEquals("114_1", latestYear?.value)
        assertEquals("E3", latestYear?.latestExam()?.value)
    }

    @Test
    fun comparisonCalculatesAverageRankAndSubjectDeltas() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val previous = MockGradeSystem.generateReport(
            customScores = listOf(70.0, 78.0, 62.0, 70.0, 70.0, 70.0, 70.0),
            customClassRank = 18.0,
            customAverageScore = 70.0,
        )

        val comparison = buildGradeComparison(current, previous, previousExamName = "第二次段考")

        assertEquals("第二次段考", comparison.previousExamName)
        assertTrue(comparison.averageDelta > 0.0)
        assertEquals(3, comparison.classRankDelta)
        assertEquals(18.0, comparison.subjectComparisons["數學"]?.scoreDelta ?: 0.0, 0.001)
        assertNotNull(buildGradeAnalysis(current, previous, "第二次段考").comparison)
    }

    @Test
    fun gradeTrendKeepsOldToNewOrder() {
        val trend = buildGradeTrend(
            currentExamName = "期末考",
            currentReport = MockGradeSystem.generateReport(customScores = listOf(84.0, 84.0, 84.0, 84.0, 84.0, 84.0, 84.0)),
            previousReports = listOf(
                "第一次段考" to MockGradeSystem.generateReport(customClassRank = 22.0),
                "第二次段考" to MockGradeSystem.generateReport(customClassRank = 18.0),
            ),
        )

        assertEquals(listOf("第一次段考", "第二次段考", "期末考"), trend.points.map { it.examName })
        assertEquals(3, trend.points.size)
        assertTrue(trend.averageLine.contains("→"))
    }

    @Test
    fun localInsightsChooseFocusStrengthAndProjection() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val previous = MockGradeSystem.generateReport(
            customScores = listOf(62.0, 78.0, 70.0, 70.0, 70.0, 70.0, 70.0),
            customClassRank = 18.0,
        )
        val analysis = buildGradeAnalysis(current, previous, "第二次段考")
        val insights = LocalScoreInsightProvider().buildInsights(current, analysis)

        assertNotNull(insights.projection)
        assertTrue(insights.items.any { it.title == "最值得補強" && it.body.contains("社會") })
        assertTrue(insights.items.any { it.title == "最具優勢" && it.body.contains("數學") })
        assertTrue(insights.items.any { it.title == "排名推估" && it.body.contains("粗估") })
        assertTrue((insights.projection?.estimatedClassRank ?: 99) >= 1)
    }

    @Test
    fun simulationDefaultScoresMatchCurrentWeightedAverage() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)

        val simulation = simulateScores(
            currentReport = current,
            historyReports = emptyList(),
            adjustedScores = emptyMap(),
        )

        assertEquals(current.weightedAverage(), simulation.adjustedAverage, 0.001)
        assertEquals(current.weightedAverage(), simulation.projectedAverage, 0.001)
        assertEquals(15, simulation.estimatedClassRank)
    }

    @Test
    fun simulationAdjustsMultipleSubjectsAndProjectsFromHistory() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val history = listOf(
            MockGradeSystem.generateReport(customScores = listOf(60.0, 70.0, 60.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 20.0),
            MockGradeSystem.generateReport(customScores = listOf(70.0, 75.0, 65.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 16.0),
            MockGradeSystem.generateReport(customScores = listOf(78.0, 80.0, 70.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 13.0),
        )

        val simulation = simulateScores(
            currentReport = current,
            historyReports = history,
            adjustedScores = mapOf(
                "國語文" to 68.0,
                "英語文" to 82.0,
                "數學" to 84.0,
            ),
        )

        assertEquals(77.125, simulation.adjustedAverage, 0.01)
        assertTrue(simulation.projectedAverage > simulation.adjustedAverage)
        assertTrue((simulation.estimatedClassRank ?: 99) < 15)
        assertTrue((simulation.estimatedClassRank ?: 0) in 1..38)
    }

    @Test
    fun simulationDoesNotEstimateRankWithFewerThanThreeHistoryPoints() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val history = listOf(
            MockGradeSystem.generateReport(customClassRank = 20.0),
            MockGradeSystem.generateReport(customClassRank = 16.0),
        )

        val simulation = simulateScores(
            currentReport = current,
            historyReports = history,
            adjustedScores = mapOf("國語文" to 68.0),
        )

        assertNull(simulation.estimatedClassRank)
    }

    @Test
    fun simulationRankRegressionClampsToClassBounds() {
        val current = MockGradeSystem.generateReport(customScores = listOf(80.0, 80.0, 80.0, 80.0, 80.0, 80.0, 80.0))
        val history = listOf(
            MockGradeSystem.generateReport(customScores = listOf(60.0, 60.0, 60.0, 60.0, 60.0, 60.0, 60.0), customClassRank = 30.0),
            MockGradeSystem.generateReport(customScores = listOf(70.0, 70.0, 70.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 20.0),
            MockGradeSystem.generateReport(customScores = listOf(80.0, 80.0, 80.0, 80.0, 80.0, 80.0, 80.0), customClassRank = 10.0),
        )

        val simulation = simulateScores(
            currentReport = current,
            historyReports = history,
            adjustedScores = mapOf(
                "國語文" to 100.0,
                "英語文" to 100.0,
                "數學" to 100.0,
            ),
        )

        assertEquals(1, simulation.estimatedClassRank)
    }

    @Test
    fun simulationRelativeRankUsesFrontBackSpreadWhenTopBottomIsInvalid() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val history = listOf(
            MockGradeSystem.generateReport(customScores = listOf(60.0, 70.0, 60.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 20.0),
            MockGradeSystem.generateReport(customScores = listOf(70.0, 75.0, 65.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 16.0),
            MockGradeSystem.generateReport(customScores = listOf(78.0, 80.0, 70.0, 70.0, 70.0, 70.0, 70.0), customClassRank = 13.0),
        )

        val simulation = simulateScores(
            currentReport = current,
            historyReports = history,
            adjustedScores = mapOf(
                "國語文" to 68.0,
                "英語文" to 82.0,
                "數學" to 84.0,
            ),
        )

        assertTrue((simulation.estimatedClassRank ?: 99) < 15)
    }

    @Test
    fun simulationDoesNotEstimateRankWhenHistoryRankDataIsMissing() {
        val current = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val history = listOf(
            MockGradeSystem.generateReport(
                customScores = listOf(60.0, 70.0, 60.0, 70.0, 70.0, 70.0, 70.0), 
                customClassRank = null
            ),
        )

        val simulation = simulateScores(
            currentReport = current,
            historyReports = history,
            adjustedScores = mapOf("國語文" to 68.0),
        )

        assertTrue(simulation.projectedAverage > simulation.adjustedAverage)
        assertNull(simulation.estimatedClassRank)
    }

    @Test
    fun localInsightsDoNotEstimateRankWhenRankDataMissing() {
        val current = MockGradeSystem.generateReport(customClassRank = null)
        val reportWithNullClassCount = current.copy(examSummary = current.examSummary?.copy(classCount = null))
        val analysis = buildGradeAnalysis(reportWithNullClassCount)
        val insights = LocalScoreInsightProvider().buildInsights(reportWithNullClassCount, analysis)

        assertNull(insights.projection?.estimatedClassRank)
        assertTrue(insights.items.any { it.body.contains("排名資料不足") })
    }
}
