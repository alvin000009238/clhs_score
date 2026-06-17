package com.clhs.score.viewmodel

import com.clhs.score.analytics.AnalyticsEvents
import com.clhs.score.analytics.AnalyticsLogger
import com.clhs.score.analytics.AnalyticsParams
import com.clhs.score.data.ScheduleClassOption
import com.clhs.score.data.ScheduleReport
import com.clhs.score.data.ScheduleRepository
import com.clhs.score.data.ScheduleYearTermOption
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
class ScheduleViewModelTest {
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
    fun staleClassListDoesNotOverrideNewerYearSelection() = runTest(dispatcher) {
        val repository = ControllableScheduleRepository()
        val viewModel = ScheduleViewModel(repository)
        runCurrent()

        repository.yearsDeferred.complete(
            listOf(
                ScheduleYearTermOption(text = "114 學年度 第 1 學期", value = "114_1"),
                ScheduleYearTermOption(text = "113 學年度 第 2 學期", value = "113_2"),
            ),
        )
        runCurrent()

        viewModel.selectYear("113_2")
        runCurrent()

        repository.completeClasses(
            year = "114",
            term = "1",
            classes = listOf(ScheduleClassOption(text = "一年甲班", value = "101")),
        )
        runCurrent()

        assertEquals("113_2", viewModel.uiState.value.selectedYearValue)
        assertEquals(emptyList<ScheduleClassOption>(), viewModel.uiState.value.availableClasses)

        repository.completeClasses(
            year = "113",
            term = "2",
            classes = listOf(ScheduleClassOption(text = "二年乙班", value = "202")),
        )
        runCurrent()

        assertEquals("113_2", viewModel.uiState.value.selectedYearValue)
        assertEquals(listOf(ScheduleClassOption(text = "二年乙班", value = "202")), viewModel.uiState.value.availableClasses)
        assertEquals("202", viewModel.uiState.value.selectedClassValue)
    }

    @Test
    fun loadYearsErrorUpdatesState() = runTest(dispatcher) {
        val repository = ControllableScheduleRepository()
        val viewModel = ScheduleViewModel(repository)
        runCurrent()

        repository.yearsDeferred.completeExceptionally(RuntimeException("Network error"))
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isError)
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun savingWidgetPreferencesLogsAnonymousAnalytics() = runTest(dispatcher) {
        val repository = ControllableScheduleRepository()
        val analytics = RecordingAnalyticsLogger()
        val viewModel = ScheduleViewModel(repository, analytics)
        runCurrent()

        viewModel.saveWidgetPreferences(showTeacher = false, showClassroom = true, showTime = false)
        runCurrent()

        val event = analytics.events.single { it.name == AnalyticsEvents.SCHEDULE_WIDGET_SETTINGS_SAVE }
        assertEquals(false, event.parameters[AnalyticsParams.SHOW_TEACHER])
        assertEquals(true, event.parameters[AnalyticsParams.SHOW_CLASSROOM])
        assertEquals(false, event.parameters[AnalyticsParams.SHOW_TIME])
        assertFalse(analytics.containsSensitiveKey())
    }

    private class ControllableScheduleRepository : ScheduleRepository {
        val yearsDeferred = CompletableDeferred<List<ScheduleYearTermOption>>()
        private val classes = mutableMapOf<Pair<String, String>, CompletableDeferred<List<ScheduleClassOption>>>()

        override suspend fun getScheduleYears(): List<ScheduleYearTermOption> = yearsDeferred.await()

        override suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption> =
            classes.getOrPut(year to term) { CompletableDeferred() }.await()

        override suspend fun fetchSchedule(
            yearValue: String,
            year: String,
            term: String,
            classNo: String,
        ): ScheduleReport = ScheduleReport(yearValue, emptyList())

        override suspend fun getLatestSchedule(): ScheduleReport? = null

        override suspend fun getWidgetPreferences(): Triple<Boolean, Boolean, Boolean> = Triple(true, true, true)

        override suspend fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean) = Unit

        fun completeClasses(year: String, term: String, classes: List<ScheduleClassOption>) {
            this.classes.getOrPut(year to term) { CompletableDeferred() }.complete(classes)
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
            val forbidden = setOf("studentNo", "classNo", "cookies", "apiToken", "rawResult", "url")
            return events.any { event -> event.parameters.keys.any { it in forbidden } }
        }
    }
}
