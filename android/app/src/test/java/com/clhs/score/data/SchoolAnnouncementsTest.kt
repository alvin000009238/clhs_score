package com.clhs.score.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.TimeUnit

class SchoolAnnouncementsTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun parserReadsRecordedListShapeWithoutFilteringOrReordering() {
        val page = SchoolAnnouncementParser.parsePage(
            RECORDED_LIST_JSON,
            Instant.parse("2026-08-09T00:00:00Z"),
        )

        assertEquals(0, page.pageIndex)
        assertEquals(7532, page.totalPages)
        assertEquals(listOf("45072", "45068", "external"), page.announcements.map { it.id })
        assertEquals(listOf("公告", "最新消息", "通知"), page.announcements.map { it.category })
        assertEquals(listOf("讀者服務組", "設備組", "學務處"), page.announcements.map { it.unit })
        assertTrue(page.announcements.first().isPinned)
        assertEquals("https://example.org/activity", page.announcements.last().externalUrl)
    }

    @Test
    fun repositoryPostsEmptyFlockCachesFirstPageAndNeverStoresCookies() = runTest {
        server.enqueue(jsonResponse(RECORDED_LIST_JSON).setHeader("Set-Cookie", "session=must-not-return"))
        server.enqueue(jsonResponse(RECORDED_LIST_JSON))
        val cacheDirectory = Files.createTempDirectory("school-announcement-test").toFile()
        val fetchedAt = Instant.parse("2026-08-09T00:00:00Z")
        val repository = NetworkSchoolAnnouncementsRepository(
            cacheDirectory = cacheDirectory,
            baseUrl = server.url("/"),
            nowProvider = { fetchedAt },
        )

        try {
            val network = repository.loadPage(0)
            val cached = repository.loadCached()
            repository.loadPage(1)

            assertEquals(network.announcements, cached?.announcements)
            assertEquals(fetchedAt, cached?.fetchedAt)
            val first = server.takeRequest()
            val second = server.takeRequest()
            val firstBody = first.body.readUtf8()
            assertEquals("POST", first.method)
            assertTrue(firstBody.contains("flock="))
            assertTrue(firstBody.contains("maxRows=20"))
            assertNull(first.getHeader("Cookie"))
            assertNull(second.getHeader("Cookie"))
            assertTrue(second.body.readUtf8().contains("pageNum=1"))
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun cancellingPageLoadStopsTheBlockingHttpCall() = runBlocking {
        server.enqueue(jsonResponse(RECORDED_LIST_JSON).setBodyDelay(5, TimeUnit.SECONDS))
        val cacheDirectory = Files.createTempDirectory("school-announcement-cancel-test").toFile()
        val repository = NetworkSchoolAnnouncementsRepository(
            cacheDirectory = cacheDirectory,
            baseUrl = server.url("/"),
        )

        try {
            val load = launch(Dispatchers.Default) { repository.loadPage(0) }
            assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)
            load.cancel()

            withTimeout(1_000L) { load.join() }
            assertTrue(load.isCancelled)
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun detailParserSanitizesActiveContentAndDecodesOfficialAttachments() {
        val detail = SchoolAnnouncementParser.parseDetail(RECORDED_DETAIL_JSON, "45072", "公告")

        assertEquals("公告", detail.category)
        assertTrue(detail.htmlContent.contains("閉館日期"))
        assertTrue(detail.htmlContent.contains("https://www.clhs.tyc.edu.tw/notice"))
        assertFalse(detail.htmlContent.contains("script", ignoreCase = true))
        assertFalse(detail.htmlContent.contains("javascript:", ignoreCase = true))
        assertTrue(detail.htmlContent.contains("⚠️ 此內容包含表格"))
        assertEquals(2, detail.images.size)
        assertEquals(1, detail.images.count { it.canPreview })
        assertTrue(detail.images.first { it.canPreview }.url.startsWith("https://www.clhs.tyc.edu.tw/"))
        assertEquals("八月開館時間.pdf", detail.attachments.single().name)
        assertEquals(245_760L, detail.attachments.single().sizeBytes)
        assertTrue(detail.attachments.single().url.endsWith("/%E5%85%AB%E6%9C%88%E9%96%8B%E9%A4%A8%E6%99%82%E9%96%93.pdf"))
        assertNull(safeAnnouncementWebUrl("javascript:alert(1)"))
    }

    @Test
    fun duplicateAttachmentsAreCollapsedByUrl() {
        val detail = SchoolAnnouncementParser.parseDetail(
            """
            [{
              "rcode":200,
              "newsId":"45072",
              "content":"",
              "attachedfile":"[[\"opaque\",123,\"same.pdf\"],[\"opaque\",123,\"same.pdf\"]]"
            }]
            """.trimIndent(),
            "45072",
            "公告",
        )

        assertEquals("same.pdf", detail.attachments.single().name)
    }

    @Test
    fun oversizedSchoolHeadingsBecomeBoldBodyParagraphs() {
        val detail = SchoolAnnouncementParser.parseDetail(HEADING_DETAIL_JSON, "45049", "公告")

        assertFalse(detail.htmlContent.contains("<h1", ignoreCase = true))
        assertTrue(detail.htmlContent.contains("<p><strong>1.學生名單請參閱附加檔案</strong></p>"))
    }

    @Test
    fun repositoryUsesTwoStepUidFlowForDetail() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<script>var g_news_unique_id = \"public-detail-uid\";</script>"),
        )
        server.enqueue(jsonResponse(RECORDED_DETAIL_JSON))
        val cacheDirectory = Files.createTempDirectory("school-announcement-detail-test").toFile()
        val repository = NetworkSchoolAnnouncementsRepository(
            cacheDirectory = cacheDirectory,
            baseUrl = server.url("/"),
        )

        try {
            val detail = repository.loadDetail("45072", "公告")

            assertEquals("45072", detail.id)
            assertEquals("/ischool/public/news_view/show.php?nid=45072", server.takeRequest().path)
            assertEquals(
                "/ischool/widget/site_news/news_query_json_content.php?nid=45072&dir=0&uid=public-detail-uid",
                server.takeRequest().path,
            )
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private companion object {
        val RECORDED_LIST_JSON = """
            [
              {"pageNum":0,"maxRows":2,"totalPages":7532},
              {"newsId":"45072","top":1,"time":"2026/08/06","attr":"1","attr_name":"公告","title":"圖書館八月閉館時間異動公告","unit":"399","unit_name":"圖書館官網","issuer":"100103","name":"讀者服務組","content_type":"content","content":null},
              {"newsId":"45068","top":"0","time":"2026/08/06","attr_name":"最新消息","title":"中央研究院高中生命科學研究人才培育計畫","unit_name":"設備組官網","name":"設備組","content_type":"content"},
              {"newsId":"external","top":false,"time":"","attr_name":"通知","title":"校外活動","unit_name":"學務處","content_type":"url","content":"https://example.org/activity","future_field":"ignored"}
            ]
        """.trimIndent()

        val RECORDED_DETAIL_JSON = """
            [{
              "rcode":200,
              "newsId":"45072",
              "time":"2026-08-06 11:38:16",
              "title":"圖書館八月閉館時間異動公告",
              "unit":"圖書館官網",
              "issuer":"讀者服務組",
              "content":"%3Cscript%3Ealert(1)%3C%2Fscript%3E%3Cp%3E%3Cstrong%3E%E9%96%89%E9%A4%A8%E6%97%A5%E6%9C%9F%3C%2Fstrong%3E%3Ca%20href%3D%22%2Fnotice%22%3E%E8%A9%B3%E6%83%85%3C%2Fa%3E%3Ca%20href%3D%22javascript%3Aalert(1)%22%3Ebad%3C%2Fa%3E%3C%2Fp%3E%3Cimg%20src%3D%22%2Fischool%2Fstatic%2Fimage%2Fnews.jpg%22%20alt%3D%22%E5%85%AC%E5%91%8A%E5%9C%96%E7%89%87%22%3E%3Cimg%20src%3D%22https%3A%2F%2Ftracker.example%2Fpixel.png%22%3E%3Ctable%3E%3Ctr%3E%3Ctd%3Edata%3C%2Ftd%3E%3C%2Ftr%3E%3C%2Ftable%3E",
              "content_type":"content",
              "attachedfile":"[[\"opaque\",245760,\"%u516B%u6708%u958B%u9928%u6642%u9593.pdf\"]]"
            }]
        """.trimIndent()

        val HEADING_DETAIL_JSON = """
            [{
              "rcode":200,
              "newsId":"45049",
              "title":"重補修課程開課時間及學生名單公告",
              "content":"%3Ch1%3E%3Cspan%20style%3D%22font-size%3A%2020px%3B%22%3E1.%E5%AD%B8%E7%94%9F%E5%90%8D%E5%96%AE%E8%AB%8B%E5%8F%83%E9%96%B1%E9%99%84%E5%8A%A0%E6%AA%94%E6%A1%88%3C%2Fspan%3E%3C%2Fh1%3E",
              "attachedfile":"[]"
            }]
        """.trimIndent()
    }
}
