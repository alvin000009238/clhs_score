package com.clhs.score.data

import kotlinx.serialization.json.JsonObject

enum class StudentScenario {
    NORMAL, EXCELLENT, STRUGGLING, SPECIAL
}

object MockGradeSystem {
    data class SubjectSpec(
        val name: String,
        val classAverage: Double,
        val standard: GradeStandard,
    )

    val subjectSpecs = listOf(
        SubjectSpec(
            name = "國文",
            classAverage = 76.2,
            standard = GradeStandard("國文", 91.0, 84.0, 76.0, 68.0, 55.0, 10.4, 4, 9, 12, 8, 3, 1, 1, 0, 0, 0),
        ),
        SubjectSpec(
            name = "英文",
            classAverage = 74.8,
            standard = GradeStandard("英文", 90.0, 82.0, 75.0, 66.0, 52.0, 11.6, 3, 8, 13, 8, 4, 1, 1, 0, 0, 0),
        ),
        SubjectSpec(
            name = "數學",
            classAverage = 70.5,
            standard = GradeStandard("數學", 88.0, 80.0, 71.0, 60.0, 45.0, 14.2, 5, 7, 9, 8, 5, 2, 1, 1, 0, 0),
        ),
        SubjectSpec(
            name = "自然",
            classAverage = 73.4,
            standard = GradeStandard("自然", 89.0, 81.0, 73.0, 64.0, 50.0, 12.3, 4, 7, 12, 8, 5, 1, 1, 0, 0, 0),
        ),
        SubjectSpec(
            name = "社會",
            classAverage = 75.0,
            standard = GradeStandard("社會", 92.0, 84.0, 75.0, 67.0, 54.0, 10.8, 3, 10, 11, 9, 3, 1, 1, 0, 0, 0),
        ),
        SubjectSpec(
            name = "地理",
            classAverage = 77.1,
            standard = GradeStandard("地理", 93.0, 85.0, 77.0, 69.0, 56.0, 9.9, 4, 9, 12, 8, 3, 1, 1, 0, 0, 0),
        ),
        SubjectSpec(
            name = "歷史",
            classAverage = 72.6,
            standard = GradeStandard("歷史", 89.0, 82.0, 73.0, 65.0, 52.0, 11.1, 2, 8, 12, 10, 4, 1, 1, 0, 0, 0),
        ),
    )

    fun generateReport(
        scenario: StudentScenario = StudentScenario.NORMAL,
        year: Int = 114,
        termText: String = "第 1 學期",
        examName: String = "期末考",
        customTotalScore: Double? = null,
        customAverageScore: Double? = null,
        customClassRank: Double? = null,
        customCategoryRank: Double? = null,
        customScores: List<Double>? = null,
    ): GradeReport {
        val yearTermDisplay = "$year 學年度 $termText"
        
        val scores = customScores ?: when (scenario) {
            StudentScenario.NORMAL -> listOf(78.0, 75.0, 80.0, 76.0, 70.0, 79.0, 74.0)
            StudentScenario.EXCELLENT -> listOf(95.0, 98.0, 100.0, 92.0, 96.0, 90.0, 94.0)
            StudentScenario.STRUGGLING -> listOf(45.0, 50.0, 30.0, 40.0, 55.0, 60.0, 42.0)
            StudentScenario.SPECIAL -> listOf(80.0, -1.0, 90.0, -2.0, 70.0, 85.0, 75.0) // -1:缺考, -2:作弊
        }

        val classCount = 38
        val yearRankCount = 226

        val subjects = subjectSpecs.zip(scores).mapIndexed { index, (spec, scoreValue) ->
            val classAverage = spec.classAverage
            val isAbsent = scoreValue == -1.0
            val isCheating = scoreValue == -2.0
            val actualScore = if (isAbsent || isCheating) 0.0 else scoreValue
            val scoreDisplay = when {
                isAbsent -> "缺考"
                isCheating -> "作弊"
                else -> "%.2f".format(actualScore)
            }

            val subjectClassRank = if (isAbsent || isCheating) classCount else subjectRank(actualScore, classAverage, index)
            val subjectYearRank = if (isAbsent || isCheating) yearRankCount else (subjectClassRank * 6 - 2).coerceIn(1, yearRankCount)

            SubjectScore(
                subjectName = spec.name,
                scoreDisplay = scoreDisplay,
                score = if (isAbsent || isCheating) null else actualScore,
                classAverageDisplay = "%.2f".format(classAverage),
                classAverage = classAverage,
                classRank = subjectClassRank,
                classRankCount = classCount,
                yearRank = subjectYearRank,
                yearRankCount = yearRankCount,
                yearTermDisplay = yearTermDisplay,
                flunk = actualScore < 60.0,
                absent = isAbsent,
                cheating = isCheating,
            )
        }

        val computedTotal = subjects.sumOf { it.score ?: 0.0 }
        val computedAverage = if (subjects.isNotEmpty()) computedTotal / subjects.size else 0.0
        val finalTotalScore = customTotalScore ?: computedTotal
        val finalAverageScore = customAverageScore ?: computedAverage
        
        val finalClassRank = customClassRank ?: when (scenario) {
            StudentScenario.EXCELLENT -> 1.0
            StudentScenario.STRUGGLING -> 35.0
            else -> 15.0
        }
        val finalCategoryRank = customCategoryRank ?: (finalClassRank * 6 - 2).coerceIn(1.0, yearRankCount.toDouble())

        return GradeReport(
            message = "fake mock data",
            studentInfo = StudentInfo(
                studentNo = "demo-student",
                studentName = "展示學生",
                className = "高二 3 班",
                seatNo = "18",
                updatedAt = "2026-05-20 08:00",
                showClassRank = true,
                showClassRankCount = true,
                showCategoryRank = true,
                showCategoryRankCount = true,
            ),
            examSummary = ExamSummary(
                year = year,
                termText = termText,
                examName = examName,
                totalScoreDisplay = "%.2f".format(finalTotalScore),
                averageScoreDisplay = "%.2f".format(finalAverageScore),
                classRank = finalClassRank,
                classCount = classCount,
                categoryRank = finalCategoryRank,
                categoryRankCount = yearRankCount,
                flunkCount = subjects.count { it.flunk },
            ),
            subjects = subjects,
            standards = subjectSpecs.map { it.standard },
            rawResult = JsonObject(emptyMap()),
        )
    }

    private fun subjectRank(score: Double, classAverage: Double, index: Int): Int {
        val base = when {
            score - classAverage >= 12.0 -> 4
            score - classAverage >= 6.0 -> 8
            score >= classAverage -> 15
            score >= classAverage - 6.0 -> 23
            else -> 31
        }
        return (base + index).coerceIn(1, 38)
    }
}

object FakeData {
    val session = AuthenticatedSession(
        studentNo = "demo-student",
        apiToken = "fake-token",
        cookies = emptyMap(),
    )

    val structure: List<YearTermOption> = listOf(
        YearTermOption(
            text = "113 學年度 第 2 學期",
            value = "113_2",
            exams = listOf(
                ExamOption("第一次段考", "113_2_E1"),
                ExamOption("第二次段考", "113_2_E2"),
                ExamOption("期末考", "113_2_E3"),
            ),
        ),
        YearTermOption(
            text = "114 學年度 第 1 學期",
            value = "114_1",
            exams = listOf(
                ExamOption("第一次段考", "114_1_E1"),
                ExamOption("第二次段考", "114_1_E2"),
                ExamOption("第三次段考", "114_1_E3"),
                ExamOption("期末考", "114_1_E4"),
            ),
        ),
    )

    val currentYearValue = "114_1"
    val currentExamValue = "114_1_E4"

    private val reports: Map<Pair<String, String>, GradeReport> by lazy {
        mapOf(
            ("114_1" to "114_1_E4") to MockGradeSystem.generateReport(
                year = 114, termText = "第 1 學期", examName = "期末考",
                customTotalScore = 566.0, customAverageScore = 80.9, customClassRank = 7.0, customCategoryRank = 38.0,
                customScores = listOf(84.0, 78.0, 92.0, 88.0, 73.0, 81.0, 70.0)
            ),
            ("114_1" to "114_1_E3") to MockGradeSystem.generateReport(
                year = 114, termText = "第 1 學期", examName = "第三次段考",
                customTotalScore = 543.0, customAverageScore = 77.6, customClassRank = 10.0, customCategoryRank = 52.0,
                customScores = listOf(80.0, 75.0, 86.0, 83.0, 70.0, 79.0, 70.0)
            ),
            ("114_1" to "114_1_E2") to MockGradeSystem.generateReport(
                year = 114, termText = "第 1 學期", examName = "第二次段考",
                customTotalScore = 523.0, customAverageScore = 74.7, customClassRank = 14.0, customCategoryRank = 71.0,
                customScores = listOf(76.0, 72.0, 82.0, 80.0, 68.0, 75.0, 70.0)
            ),
            ("114_1" to "114_1_E1") to MockGradeSystem.generateReport(
                year = 114, termText = "第 1 學期", examName = "第一次段考",
                customTotalScore = 506.0, customAverageScore = 72.3, customClassRank = 18.0, customCategoryRank = 90.0,
                customScores = listOf(72.0, 70.0, 78.0, 76.0, 66.0, 73.0, 71.0)
            ),
            ("113_2" to "113_2_E3") to MockGradeSystem.generateReport(
                year = 113, termText = "第 2 學期", examName = "期末考",
                customTotalScore = 514.0, customAverageScore = 73.4, customClassRank = 16.0, customCategoryRank = 82.0,
                customScores = listOf(74.0, 71.0, 80.0, 78.0, 67.0, 75.0, 69.0)
            ),
        )
    }

    fun reportFor(yearValue: String, examValue: String): GradeReport =
        reports[yearValue to examValue] ?: reports.getValue(currentYearValue to currentExamValue)

    fun latestReport(): GradeReport = reportFor(currentYearValue, currentExamValue)

    fun previousReport(): GradeReport = reportFor("114_1", "114_1_E3")

    fun trendReports(): List<Pair<String, GradeReport>> = listOf(
        "第一次段考" to reportFor("114_1", "114_1_E1"),
        "第二次段考" to reportFor("114_1", "114_1_E2"),
        "第三次段考" to reportFor("114_1", "114_1_E3"),
    )

    fun simulatorHistoryReports(): List<GradeReport> = listOf(
        reportFor("114_1", "114_1_E1"),
        reportFor("114_1", "114_1_E2"),
        reportFor("114_1", "114_1_E3"),
    )
}

class FakeGradeRepository : GradeRepository {
    private var activeSession: AuthenticatedSession? = FakeData.session

    override fun restoreSession(): AuthenticatedSession? = activeSession

    override suspend fun refreshCaptcha(): CaptchaChallenge = CaptchaChallenge(
        loginToken = "fake-login-token",
        shCaptchaGenCode = "fake-captcha",
        deviceToken = "fake-device",
        cookies = emptyMap(),
        imageBytes = ByteArray(0),
        contentType = "image/png",
    )

    override suspend fun login(
        username: String,
        password: String,
        captchaCode: String,
        challenge: CaptchaChallenge,
    ): AuthenticatedSession = FakeData.session.also { activeSession = it }

    override suspend fun loadStructure(session: AuthenticatedSession): List<YearTermOption> = FakeData.structure

    override suspend fun fetchGrades(
        session: AuthenticatedSession,
        yearValue: String,
        examValue: String,
    ): GradeReport = FakeData.reportFor(yearValue, examValue)

    override fun logout() {
        activeSession = null
    }

    override suspend fun loginWithCookies(
        studentNo: String,
        cookies: Map<String, String>,
    ): AuthenticatedSession = FakeData.session.also { activeSession = it }
}
