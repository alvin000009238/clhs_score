package com.clhs.score.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SchoolGradeClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: SchoolGradeClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = SchoolGradeClient(baseUrl = server.url("/CLHSTYC/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
    @Test
    fun loadStructureAndFetchGradesMapResponses() = runTest {
        val session = AuthenticatedSession(
            studentNo = "DEMO-001",
            apiToken = "api-token",
            cookies = mapOf("ASP.NET_SessionId" to "abc"),
        )
        server.enqueue(jsonResponse("""[{"DisplayText":"114學年度 上學期","Value":"114_1"}]"""))
        server.enqueue(jsonResponse("""[{"DisplayText":"期末考","Value":"期末考"}]"""))
        server.enqueue(jsonResponse(gradeJson))

        val structure = client.loadStructure(session)
        val report = client.fetchGrades(session, "114_1", "期末考")

        assertEquals("114學年度 上學期", structure.single().text)
        assertEquals("期末考", structure.single().exams.single().text)
        assertEquals("範例學生", report.studentInfo.studentName)
        assertEquals("DEMO-001", report.studentInfo.studentNo)
        assertEquals(114, report.examSummary?.year)
        assertEquals("下", report.examSummary?.termText)
        assertEquals("第二次段考", report.examSummary?.examName)
        assertEquals("國語文", report.subjects.single().subjectName)
        assertEquals(78.0, report.subjects.single().scoreValue, 0.001)
        assertEquals(80.0, report.standards.single().top ?: 0.0, 0.001)
    }

    @Test
    fun parseYearTermMatchesFetcherBehavior() {
        assertEquals("114" to "1", parseYearTerm("114_1"))
        assertEquals("114" to "1", parseYearTerm("1141"))
        assertEquals("114" to "1", parseYearTerm("bad"))
    }

    @Test
    fun parseScheduleItemsRecursesThroughNestedAliasesAndDeduplicatesSlots() {
        val items = parseScheduleItems(scheduleJson)

        assertEquals(2, items.size)
        assertEquals(
            ScheduleItem(
                dayOfWeek = 1,
                period = 2,
                subjectName = "數學",
                teacherName = "範例教師",
                classroom = "示範教室",
            ),
            items[0].copy(rawData = null),
        )
        assertEquals(
            ScheduleItem(
                dayOfWeek = 3,
                period = 4,
                subjectName = "英語文",
                teacherName = "代理教師",
                classroom = "語言教室",
            ),
            items[1].copy(rawData = null),
        )
    }

    @Test
    fun fetchSchedulePostsSelectionAndParsesRecursiveTimetableResponse() = runTest {
        val session = AuthenticatedSession(
            studentNo = "DEMO-001",
            apiToken = "api-token",
            cookies = mapOf("ASP.NET_SessionId" to "abc"),
        )
        server.enqueue(htmlResponse("""<input name="__RequestVerificationToken" value="schedule-token" />"""))
        server.enqueue(jsonResponse(scheduleJson))

        val report = client.fetchSchedule(
            session = session,
            yearValue = "114_1",
            year = "114",
            term = "1",
            classNo = "230",
        )

        assertEquals("114_1", report.yearTermValue)
        assertEquals(2, report.items.size)
        assertEquals("數學", report.items.first().subjectName)

        assertEquals("/CLHSTYC/ClassTableV2/ClassTable", server.takeRequest().path)
        val timetableRequest = server.takeRequest()
        assertEquals("/CLHSTYC/ClassTableV2/ClassTable/GetTimeTable", timetableRequest.path)
        val form = timetableRequest.body.readUtf8()
        assertTrue(form.contains("__RequestVerificationToken=schedule-token"))
        assertTrue(form.contains("Year=114"))
        assertTrue(form.contains("Term=1"))
        assertTrue(form.contains("ClassNo=230"))
        assertTrue(form.contains("TimetableType=Class"))
    }

    @Test
    fun fetchScheduleRefreshesTokenAfterSessionRestore() = runTest {
        val firstSession = AuthenticatedSession(
            studentNo = "DEMO-001",
            apiToken = "api-token",
            cookies = mapOf("ASP.NET_SessionId" to "abc"),
        )
        val restoredSession = AuthenticatedSession(
            studentNo = "DEMO-002",
            apiToken = "api-token-2",
            cookies = mapOf("ASP.NET_SessionId" to "def"),
        )
        server.enqueue(htmlResponse("""<input name="__RequestVerificationToken" value="schedule-token-1" />"""))
        server.enqueue(jsonResponse(scheduleJson))
        server.enqueue(htmlResponse("""<input name="__RequestVerificationToken" value="schedule-token-2" />"""))
        server.enqueue(jsonResponse(scheduleJson))

        client.fetchSchedule(firstSession, "114_1", "114", "1", "230")
        client.restoreSession(restoredSession)
        client.fetchSchedule(restoredSession, "114_1", "114", "1", "230")

        server.takeRequest()
        server.takeRequest()
        assertEquals("/CLHSTYC/ClassTableV2/ClassTable", server.takeRequest().path)
        val secondTimetableRequest = server.takeRequest()
        assertEquals("/CLHSTYC/ClassTableV2/ClassTable/GetTimeTable", secondTimetableRequest.path)
        assertTrue(secondTimetableRequest.body.readUtf8().contains("__RequestVerificationToken=schedule-token-2"))
    }

    @Test
    fun clearSessionClearsCookieJar() {
        val jar = SchoolCookieJar()
        val clientWithJar = SchoolGradeClient(
            baseUrl = server.url("/CLHSTYC/").toString(),
            cookieJar = jar,
        )
        jar.replace(mapOf("ASP.NET_SessionId" to "abc"), domain = server.url("/").host)

        assertFalse(jar.snapshot().isEmpty())
        clientWithJar.clearSession()
        assertTrue(jar.snapshot().isEmpty())
    }

    private fun htmlResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "text/html; charset=utf-8")
        .setBody(body)

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private val gradeJson = """
        {
          "Message": "",
          "Result": {
            "StudentNo": "DEMO-001",
            "StudentName": "範例學生",
            "StudentClassName": "示範班級",
            "StudentSeatNo": "00",
            "Show班級排名": true,
            "Show班級排名人數": true,
            "Show類組排名": true,
            "Show類組排名人數": true,
            "ExamItem": {
              "Year": 114,
              "Term": "上",
              "ExamName": "期末考",
              "ClassRank": 15,
              "ClassCount": 37,
              "類組排名": 78,
              "類組排名Count": 221
            },
            "SubjectExamInfoList": [
              {
                "Year": 114,
                "Term": 2,
                "ExamName": "第二次段考",
                "YearTermItem": {
                  "Year": 114,
                  "Term": 2,
                  "TermText": "下"
                },
                "SubjectName": "國語文",
                "Score": 78,
                "ScoreDisplay": "78.00",
                "ClassAVGScore": 70.11,
                "ClassAVGScoreDisplay": "70.11",
                "ClassRank": 6,
                "ClassRankCount": 37,
                "YearTermDisplay": "114學年度 上學期"
              }
            ],
            "成績五標List": [
              {
                "SubjectName": "國語文",
                "頂標": 80,
                "前標": 75,
                "均標": 70,
                "後標": 60,
                "底標": 50,
                "標準差": 12,
                "大於90Count": 1,
                "大於80Count": 3,
                "大於70Count": 10,
                "大於60Count": 12,
                "大於50Count": 5,
                "大於40Count": 2,
                "大於30Count": 1,
                "大於20Count": 1,
                "大於10Count": 1,
                "大於0Count": 1
              }
            ]
          }
        }
    """.trimIndent()

    private val scheduleJson = """
        {
          "payload": {
            "weeks": [
              {
                "items": [
                  {
                    "SubjectName": "數學",
                    "WeekDay": "1",
                    "SectionSeq": "2",
                    "TeacherNameDisplay": "範例教師",
                    "ClassroomName": "示範教室"
                  },
                  {
                    "SubjectName": "重複資料",
                    "WeekDay": "1",
                    "SectionSeq": "2"
                  },
                  {
                    "CourseName": "英語文",
                    "DayOfWeek": "3",
                    "Period": "4",
                    "FirstTeacherName": "代理教師",
                    "ClassroomDisplay": "語言教室"
                  },
                  {
                    "SubjectDisplay": "",
                    "DayOfWeek": "5",
                    "Period": "6"
                  }
                ]
              }
            ]
          }
        }
    """.trimIndent()
}
