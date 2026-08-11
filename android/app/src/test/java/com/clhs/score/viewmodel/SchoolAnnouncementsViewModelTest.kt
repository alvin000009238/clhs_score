package com.clhs.score.viewmodel

import com.clhs.score.data.SchoolAnnouncement
import com.clhs.score.data.SchoolAnnouncementPage
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

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolAnnouncementsViewModelTest {
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
    fun failedUpdateKeepsCachedMessagesAndShowsNotice() = runTest(dispatcher) {
        val viewModel = SchoolAnnouncementsViewModel(
            loadCached = { page(announcement("cached"), totalPages = 2) },
            loadPage = { throw IOException("offline") },
        )

        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf("cached"), state.announcements.map(SchoolAnnouncement::id))
        assertEquals("無法更新，暫時顯示已儲存的學校消息", state.noticeMessage)
        assertNull(state.errorMessage)
        assertFalse(state.isInitialLoading)
    }

    @Test
    fun loadMoreAppendsInServerOrderAndRemovesDuplicateIds() = runTest(dispatcher) {
        val viewModel = SchoolAnnouncementsViewModel(
            loadCached = { null },
            loadPage = { index ->
                if (index == 0) {
                    page(announcement("first"), announcement("duplicate"), totalPages = 2)
                } else {
                    page(announcement("duplicate"), announcement("last"), pageIndex = 1, totalPages = 2)
                }
            },
        )
        runCurrent()

        viewModel.loadMore()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf("first", "duplicate", "last"), state.announcements.map(SchoolAnnouncement::id))
        assertFalse(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun firstLoadFailureShowsFixedActionableError() = runTest(dispatcher) {
        val viewModel = SchoolAnnouncementsViewModel(
            loadCached = { null },
            loadPage = { throw IOException("server response with secret details") },
        )

        runCurrent()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("請檢查網路") == true)
        assertFalse(viewModel.uiState.value.errorMessage.orEmpty().contains("secret"))
    }

    @Test
    fun loadMoreFailureKeepsExistingMessagesAndShowsInlineRetry() = runTest(dispatcher) {
        val viewModel = SchoolAnnouncementsViewModel(
            loadCached = { null },
            loadPage = { index ->
                if (index == 0) page(announcement("first"), totalPages = 2)
                else throw IOException("offline")
            },
        )
        runCurrent()

        viewModel.loadMore()
        runCurrent()

        assertEquals(listOf("first"), viewModel.uiState.value.announcements.map(SchoolAnnouncement::id))
        assertEquals("無法載入更多消息，請稍後再試。", viewModel.uiState.value.loadMoreError)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun detailFailureUsesFixedMessageWithoutLeakingException() = runTest(dispatcher) {
        val viewModel = SchoolAnnouncementDetailViewModel {
            throw IOException("private server response")
        }

        runCurrent()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("請檢查網路") == true)
        assertFalse(viewModel.uiState.value.errorMessage.orEmpty().contains("private"))
    }

    private fun announcement(id: String) = SchoolAnnouncement(
        id = id,
        title = id,
        date = "2026/08/09",
        category = "公告",
        unit = "教務處",
        issuer = "承辦人",
        isPinned = false,
        contentType = "content",
    )

    private fun page(
        vararg announcements: SchoolAnnouncement,
        pageIndex: Int = 0,
        totalPages: Int = 1,
    ) = SchoolAnnouncementPage(
        announcements = announcements.toList(),
        pageIndex = pageIndex,
        totalPages = totalPages,
        fetchedAt = Instant.parse("2026-08-09T00:00:00Z"),
    )
}
