package com.clhs.score.viewmodel

import com.clhs.score.data.SchoolCalendarEvent
import com.clhs.score.data.SchoolCalendarFeed
import com.clhs.score.data.SchoolCalendarSnapshot
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolCalendarViewModelTest {
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
    fun failedRefreshKeepsCachedAgendaAndShowsNotice() = runTest(dispatcher) {
        val cached = snapshot(event("future", LocalDateTime.of(2026, 8, 10, 0, 0)))
        val viewModel = SchoolCalendarViewModel(
            loadCached = { cached },
            loadSnapshot = { throw IOException("offline") },
            todayProvider = { LocalDate.of(2026, 8, 9) },
        )

        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf("future"), state.events.map(SchoolCalendarEvent::id))
        assertEquals("無法更新，暫時顯示已儲存的行事曆", state.noticeMessage)
        assertNull(state.errorMessage)
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun successfulLoadFiltersFinishedDatesAndKeepsToday() = runTest(dispatcher) {
        val loaded = SchoolCalendarSnapshot(
            feed = SchoolCalendarFeed(
                events = listOf(
                    event("past", LocalDateTime.of(2026, 8, 8, 0, 0)),
                    event("today", LocalDateTime.of(2026, 8, 9, 9, 0)),
                    event("future", LocalDateTime.of(2026, 8, 10, 0, 0)),
                ),
            ),
            fetchedAt = Instant.parse("2026-08-09T00:00:00Z"),
        )
        val viewModel = SchoolCalendarViewModel(
            loadCached = { null },
            loadSnapshot = { loaded },
            todayProvider = { LocalDate.of(2026, 8, 9) },
        )

        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf("today", "future"), state.events.map(SchoolCalendarEvent::id))
        assertFalse(state.isInitialLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun firstLoadFailureShowsActionableError() = runTest(dispatcher) {
        val viewModel = SchoolCalendarViewModel(
            loadCached = { null },
            loadSnapshot = { throw IOException("offline") },
        )

        runCurrent()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("請檢查網路") == true)
        assertTrue(viewModel.uiState.value.events.isEmpty())
    }

    private fun event(id: String, start: LocalDateTime): SchoolCalendarEvent =
        SchoolCalendarEvent(
            id = id,
            title = id,
            start = start,
            endExclusive = start.plusHours(1),
            isAllDay = false,
        )

    private fun snapshot(event: SchoolCalendarEvent): SchoolCalendarSnapshot =
        SchoolCalendarSnapshot(
            feed = SchoolCalendarFeed(events = listOf(event)),
            fetchedAt = Instant.parse("2026-08-08T00:00:00Z"),
        )

}
