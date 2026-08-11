package com.clhs.score.viewmodel

import com.clhs.score.analytics.AnalyticsEvents
import com.clhs.score.analytics.AnalyticsLogger
import com.clhs.score.analytics.AnalyticsParams
import com.clhs.score.analytics.AnalyticsValues
import com.clhs.score.data.AuthenticatedSession
import com.clhs.score.data.ExamOption
import com.clhs.score.data.FakeData
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeRepository
import com.clhs.score.data.YearTermOption
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sessionRestoreRemainsPendingUntilRepositoryReturns() = runTest(dispatcher) {
        val restoreDeferred = CompletableDeferred<AuthenticatedSession?>()
        val viewModel = ScoreViewModel(ControllableGradeRepository(restoreDeferred))
        runCurrent()

        assertTrue(viewModel.gradesState.value.isRestoringSession)

        restoreDeferred.complete(null)
        runCurrent()

        assertFalse(viewModel.gradesState.value.isRestoringSession)
    }

    @Test
    fun selectingAnotherExamInvalidatesStaleFetchResult() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(
                        ExamOption("第一次段考", "E1"),
                        ExamOption("期末考", "E2"),
                    ),
                ),
            ),
        )
        runCurrent()

        viewModel.selectExam("E1")
        runCurrent()
        viewModel.selectExam("E2")
        runCurrent()

        repository.completeFetch("E1", FakeData.previousReport())
        runCurrent()
        assertEquals("E2", viewModel.gradesState.value.selectedExamValue)
        assertEquals(null, viewModel.gradesState.value.report)

        repository.completeFetch("E2", FakeData.latestReport())
        runCurrent()
        assertEquals("期末考", viewModel.gradesState.value.report?.examSummary?.examName)
    }

    @Test
    fun selectingExamKeepsCurrentReportUntilFetchCompletes() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(FakeData.structure)
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.selectExam("114_1_E2")
        runCurrent()

        assertEquals("114_1_E4", viewModel.gradesState.value.selectedExamValue)
        assertEquals("期末考", viewModel.gradesState.value.report?.examSummary?.examName)
        assertEquals(true, viewModel.gradesState.value.isLoadingGrades)

        repository.completeFetch("114_1_E2", FakeData.previousReport())
        runCurrent()

        assertEquals("114_1_E2", viewModel.gradesState.value.selectedExamValue)
        assertEquals("第二次段考", viewModel.gradesState.value.report?.examSummary?.examName)
        assertEquals(false, viewModel.gradesState.value.isLoadingGrades)
    }

    @Test
    fun refreshingStructureKeepsSelectedExam() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(FakeData.structure)
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.selectExam("114_1_E2")
        runCurrent()
        repository.completeFetch("114_1_E2", FakeData.previousReport())
        runCurrent()

        viewModel.reloadStructure()
        runCurrent()

        assertEquals("114_1", viewModel.gradesState.value.selectedYearValue)
        assertEquals("114_1_E2", viewModel.gradesState.value.selectedExamValue)
        assertEquals("第二次段考", viewModel.gradesState.value.report?.examSummary?.examName)
    }

    @Test
    fun refreshingWhileExamLoadsDoesNotCancelSelection() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(FakeData.structure)
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.selectExam("114_1_E2")
        runCurrent()
        viewModel.reloadStructure()
        runCurrent()
        repository.completeFetch("114_1_E2", FakeData.previousReport())
        runCurrent()

        assertEquals("114_1_E2", viewModel.gradesState.value.selectedExamValue)
        assertEquals("第二次段考", viewModel.gradesState.value.report?.examSummary?.examName)
    }

    @Test
    fun refreshingOnlyForcesSelectedExam() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(FakeData.structure)
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()
        repository.completeFetch("114_1_E1", FakeData.reportFor("114_1", "114_1_E1"))
        repository.completeFetch("114_1_E2", FakeData.previousReport())
        repository.completeFetch("113_2_E3", FakeData.reportFor("113_2", "113_2_E3"))
        runCurrent()

        repository.fetchCalls.clear()
        viewModel.reloadStructure()
        runCurrent()

        val selectedCalls = repository.fetchCalls.filter { it.second == "114_1_E4" }
        val historicalCalls = repository.fetchCalls.filterNot { it.second == "114_1_E4" }
        assertEquals(listOf(true), selectedCalls.map { it.third })
        assertFalse(historicalCalls.isEmpty())
        assertEquals(setOf(false), historicalCalls.map { it.third }.toSet())
    }

    @Test
    fun selectingFirstExamStillBuildsSameTermTrend() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(
                        ExamOption("第一次段考", "114_1_E1"),
                        ExamOption("第二次段考", "114_1_E2"),
                        ExamOption("期末考", "114_1_E4"),
                    ),
                ),
            ),
        )
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.selectExam("114_1_E1")
        runCurrent()
        repository.completeFetch("114_1_E1", FakeData.reportFor("114_1", "114_1_E1"))
        runCurrent()
        repository.completeFetch("114_1_E2", FakeData.previousReport())
        runCurrent()

        assertEquals(null, viewModel.gradesState.value.trendError)
        assertEquals(
            listOf("第一次段考", "第二次段考", "期末考"),
            viewModel.gradesState.value.trend?.points?.map { it.examName },
        )
    }

    @Test
    fun logoutProvidesActiveSessionToRepository() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)

        repository.structureDeferred.complete(emptyList())
        runCurrent()

        viewModel.logout()
        runCurrent()

        assertEquals("DEMO-000", repository.loggedOutSession?.studentNo)
    }

    @Test
    fun selectingYearWithoutExamsClearsCurrentReport() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(ExamOption("期末考", "114_1_E4")),
                ),
                YearTermOption(
                    text = "113 學年度 第 2 學期",
                    value = "113_2",
                    exams = emptyList(),
                ),
            ),
        )
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        assertEquals("期末考", viewModel.gradesState.value.report?.examSummary?.examName)

        viewModel.selectYear("113_2")
        runCurrent()

        assertEquals("113_2", viewModel.gradesState.value.selectedYearValue)
        assertEquals(null, viewModel.gradesState.value.selectedExamValue)
        assertEquals(null, viewModel.gradesState.value.report)
        assertEquals(false, viewModel.gradesState.value.isLoadingGrades)
    }

    @Test
    fun selectingYearWithExamsKeepsCurrentReportUntilFetchCompletes() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(FakeData.structure)
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        assertEquals(114, viewModel.gradesState.value.report?.examSummary?.year)

        viewModel.selectYear("113_2")
        runCurrent()

        assertEquals("114_1", viewModel.gradesState.value.selectedYearValue)
        assertEquals("114_1_E4", viewModel.gradesState.value.selectedExamValue)
        assertEquals(114, viewModel.gradesState.value.report?.examSummary?.year)
        assertEquals(true, viewModel.gradesState.value.isLoadingGrades)

        repository.completeFetch("113_2_E3", FakeData.reportFor("113_2", "113_2_E3"))
        runCurrent()

        assertEquals("113_2", viewModel.gradesState.value.selectedYearValue)
        assertEquals("113_2_E3", viewModel.gradesState.value.selectedExamValue)
        assertEquals(113, viewModel.gradesState.value.report?.examSummary?.year)
        assertEquals(false, viewModel.gradesState.value.isLoadingGrades)
    }

    @Test
    fun subjectTrendIgnoresStaleFetchResultAfterYearSelectionChanges() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "113 學年度 第 2 學期",
                    value = "113_2",
                    exams = listOf(ExamOption("第一次段考", "113_2_E1")),
                ),
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(ExamOption("期末考", "114_1_E4")),
                ),
            ),
        )
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.initSubjectTrend()
        runCurrent()
        viewModel.setSubjectTrendYears(setOf("113_2"))
        runCurrent()

        repository.completeFetch("113_2_E1", FakeData.reportFor("113_2", "113_2_E1"))
        runCurrent()
        assertEquals(listOf("第一次段考"), viewModel.subjectTrendState.value.reports.mapNotNull { it.examSummary?.examName })

        assertEquals(listOf("第一次段考"), viewModel.subjectTrendState.value.reports.mapNotNull { it.examSummary?.examName })
    }

    @Test
    fun subjectTrendYearSelectionLoadsOnceAndSkipsUnchangedSelection() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "113 學年度 第 2 學期",
                    value = "113_2",
                    exams = listOf(ExamOption("第一次段考", "113_2_E1")),
                ),
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(ExamOption("期末考", "114_1_E4")),
                ),
            ),
        )
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.initSubjectTrend()
        runCurrent()
        val callsBeforeApply = repository.fetchCalls.size

        viewModel.setSubjectTrendYears(setOf("113_2"))
        runCurrent()
        assertEquals(callsBeforeApply + 1, repository.fetchCalls.size)

        viewModel.setSubjectTrendYears(setOf("113_2"))
        runCurrent()
        assertEquals(callsBeforeApply + 1, repository.fetchCalls.size)

        viewModel.setSubjectTrendYears(emptySet())
        runCurrent()
        assertEquals(emptySet<String>(), viewModel.subjectTrendState.value.selectedYearValues)
        assertEquals(emptyList<GradeReport>(), viewModel.subjectTrendState.value.reports)
        assertEquals(callsBeforeApply + 1, repository.fetchCalls.size)
    }

    @Test
    fun logoutClearsSubjectTrendState() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(ExamOption("期末考", "114_1_E4")),
                ),
            ),
        )
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.initSubjectTrend()
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        viewModel.logout()
        runCurrent()

        assertEquals(emptyList<GradeReport>(), viewModel.subjectTrendState.value.reports)
        assertEquals(emptySet<String>(), viewModel.subjectTrendState.value.selectedYearValues)
    }

    @Test
    fun logoutIgnoresStaleStructureResult() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val viewModel = ScoreViewModel(repository)
        runCurrent()

        viewModel.logout()
        repository.structureDeferred.complete(FakeData.structure)
        runCurrent()

        assertFalse(viewModel.gradesState.value.isLoggedIn)
        assertEquals(emptyList<YearTermOption>(), viewModel.gradesState.value.structure)
    }

    @Test
    fun loginWithWebViewCookiesLogsAnonymousAnalytics() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val analytics = RecordingAnalyticsLogger()
        val viewModel = ScoreViewModel(repository, analyticsLogger = analytics)
        runCurrent()

        viewModel.loginWithWebViewCookies("SENSITIVE-STUDENT", "SESSION_ID=secret")
        runCurrent()

        val startEvent = analytics.events.first { it.name == AnalyticsEvents.LOGIN_START }
        val resultEvent = analytics.events.first { it.name == AnalyticsEvents.LOGIN_RESULT }

        assertEquals(AnalyticsValues.METHOD_WEBVIEW, startEvent.parameters[AnalyticsParams.METHOD])
        assertEquals(AnalyticsValues.RESULT_SUCCESS, resultEvent.parameters[AnalyticsParams.RESULT])
        assertFalse(analytics.containsSensitiveKey())
    }

    @Test
    fun gradeQuerySuccessLogsTriggerAndSubjectBucket() = runTest(dispatcher) {
        val repository = ControllableGradeRepository()
        val analytics = RecordingAnalyticsLogger()
        val viewModel = ScoreViewModel(repository, analyticsLogger = analytics)
        runCurrent()

        repository.structureDeferred.complete(
            listOf(
                YearTermOption(
                    text = "114 學年度 第 1 學期",
                    value = "114_1",
                    exams = listOf(ExamOption("期末考", "114_1_E4")),
                ),
            ),
        )
        runCurrent()
        repository.completeFetch("114_1_E4", FakeData.latestReport())
        runCurrent()

        val gradeEvent = analytics.events.first { it.name == AnalyticsEvents.GRADE_QUERY }
        assertEquals(AnalyticsValues.RESULT_SUCCESS, gradeEvent.parameters[AnalyticsParams.RESULT])
        assertEquals(AnalyticsValues.TRIGGER_INITIAL, gradeEvent.parameters[AnalyticsParams.TRIGGER])
        assertEquals("7_10", gradeEvent.parameters[AnalyticsParams.SUBJECT_COUNT_BUCKET])
        assertFalse(analytics.containsSensitiveKey())
    }

    private class ControllableGradeRepository(
        private val restoreDeferred: CompletableDeferred<AuthenticatedSession?>? = null,
    ) : GradeRepository {
        val structureDeferred = CompletableDeferred<List<YearTermOption>>()
        val fetchCalls = mutableListOf<Triple<String, String, Boolean>>()
        var loggedOutSession: AuthenticatedSession? = null
        private val session = AuthenticatedSession("DEMO-000", "token", emptyMap())
        private val fetches = mutableMapOf<Pair<String, String>, CompletableDeferred<GradeReport>>()

        override suspend fun restoreSession(): AuthenticatedSession? = restoreDeferred?.await() ?: session

        override fun activateSession(session: AuthenticatedSession) = Unit

        override suspend fun loadStructure(
            session: AuthenticatedSession,
            forceRefresh: Boolean,
        ): List<YearTermOption> = structureDeferred.await()

        override suspend fun fetchGrades(
            session: AuthenticatedSession,
            yearValue: String,
            examValue: String,
            forceRefresh: Boolean,
        ): GradeReport {
            fetchCalls += Triple(yearValue, examValue, forceRefresh)
            return fetches.getOrPut(yearValue to examValue) { CompletableDeferred() }.await()
        }

        override suspend fun logout(currentSession: AuthenticatedSession?) {
            loggedOutSession = currentSession
        }

        override suspend fun loginWithCookies(
            studentNo: String,
            cookies: Map<String, String>,
        ): AuthenticatedSession = session

        fun completeFetch(examValue: String, report: GradeReport) {
            val matchingKey = fetches.keys.firstOrNull { it.second == examValue }
            val key = matchingKey ?: ("" to examValue)
            fetches.getOrPut(key) { CompletableDeferred() }.complete(report)
        }
    }

    private data class AnalyticsEventRecord(
        val name: String,
        val parameters: Map<String, Any?>,
    )

    private class RecordingAnalyticsLogger : AnalyticsLogger {
        val events = mutableListOf<AnalyticsEventRecord>()

        override fun logEvent(name: String, parameters: Map<String, Any?>) {
            events += AnalyticsEventRecord(name, parameters)
        }

        fun containsSensitiveKey(): Boolean {
            val forbidden = setOf("studentNo", "cookies", "apiToken", "rawResult", "scoreValue", "url")
            return events.any { event -> event.parameters.keys.any { it in forbidden } }
        }
    }
}
