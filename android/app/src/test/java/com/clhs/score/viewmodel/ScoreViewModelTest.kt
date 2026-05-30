package com.clhs.score.viewmodel

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

    private class ControllableGradeRepository : GradeRepository {
        val structureDeferred = CompletableDeferred<List<YearTermOption>>()
        private val session = AuthenticatedSession("DEMO-000", "token", emptyMap())
        private val fetches = mutableMapOf<String, CompletableDeferred<GradeReport>>()

        override fun restoreSession(): AuthenticatedSession = session

        override suspend fun loadStructure(
            session: AuthenticatedSession,
            forceRefresh: Boolean,
        ): List<YearTermOption> = structureDeferred.await()

        override suspend fun fetchGrades(
            session: AuthenticatedSession,
            yearValue: String,
            examValue: String,
            forceRefresh: Boolean,
        ): GradeReport = fetches.getOrPut(examValue) { CompletableDeferred() }.await()

        override suspend fun logout() = Unit

        override suspend fun loginWithCookies(
            studentNo: String,
            cookies: Map<String, String>,
        ): AuthenticatedSession = session

        fun completeFetch(examValue: String, report: GradeReport) {
            fetches.getOrPut(examValue) { CompletableDeferred() }.complete(report)
        }
    }
}
