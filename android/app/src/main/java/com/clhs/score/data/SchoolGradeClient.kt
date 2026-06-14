package com.clhs.score.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class SchoolException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class SchoolGradeClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val cookieJar: SchoolCookieJar = SchoolCookieJar(),
    okHttpClient: OkHttpClient? = null,
) {
    private val baseUrl: HttpUrl = baseUrl.ensureTrailingSlash().toHttpUrl()
    private val origin: String = "${this.baseUrl.scheme}://${this.baseUrl.host}"
    private val client: OkHttpClient = okHttpClient ?: OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var currentStudentNo: String? = null
    private var cachedScheduleToken: String? = null
    private val sessionLock = Any()

    private fun prepareSession(session: AuthenticatedSession) {
        synchronized(sessionLock) {
            if (currentStudentNo != session.studentNo) {
                cookieJar.replace(session.cookies, domain = baseUrl.host)
                currentStudentNo = session.studentNo
                cachedScheduleToken = null
            }
        }
    }


    suspend fun loadStructure(session: AuthenticatedSession): List<YearTermOption> {
        prepareSession(session)
        val yearTerms = postOptions(
            path = "ICampus/CommonData/GetGradeCanQueryYearTermListByStudentNo",
            referer = gradesPageUrl().toString(),
            form = mapOf(
                "searchType" to "各次考試單科成績",
                "studentNo" to session.studentNo,
                "__RequestVerificationToken" to session.apiToken,
            ),
        )
        return coroutineScope {
            yearTerms.map { (text, value) ->
                async {
                    YearTermOption(
                        text = text,
                        value = value,
                        exams = loadExams(session, value),
                    )
                }
            }.awaitAll()
        }
    }

    suspend fun fetchGrades(
        session: AuthenticatedSession,
        yearValue: String,
        examValue: String,
    ): GradeReport = withContext(Dispatchers.IO) {
        prepareSession(session)
        val (year, term) = parseYearTerm(yearValue, defaultYear = "114", defaultTerm = "2")
        val body = postForm(
            path = "ICampus/TutorShGrade/GetScoreForStudentExamContent",
            referer = gradesPageUrl().toString(),
            form = mapOf(
                "StudentNo" to session.studentNo,
                "SearchType" to "單次考試所有成績",
                "__RequestVerificationToken" to session.apiToken,
                "Year" to year,
                "Term" to term,
                "ExamNo" to examValue,
            ),
        )
        parseGradeReport(body)
    }

    private suspend fun getSchedulePageToken(session: AuthenticatedSession): String {
        prepareSession(session)
        synchronized(sessionLock) {
            cachedScheduleToken?.let { return it }
        }
        val pageResponse = execute(
            Request.Builder()
                .url(resolve("ClassTableV2/ClassTable"))
                .headers(defaultHeaders(referer = resolve("ICampus/Home/Index2").toString()))
                .get()
                .build(),
        ).body.string()
        val token = hiddenInput(pageResponse, "__RequestVerificationToken")
            ?: throw SchoolException("找不到課表 API token")
        synchronized(sessionLock) {
            cachedScheduleToken = token
        }
        return token
    }

    private fun scheduleHeaders(): Headers =
        defaultHeaders(referer = resolve("ClassTableV2/ClassTable").toString()).newBuilder()
            .add("Origin", origin)
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

    private fun buildScheduleFormDefaults(token: String): FormBody.Builder =
        FormBody.Builder()
            .add("__RequestVerificationToken", token)
            .add("SchoolCode", "030305")
            .add("WeekNo", "")
            .add("ClassroomNo", "")
            .add("CrossName", "")
            .add("TeacherNo", "")
            .add("SubjectNo", "")
            .add("ShowWindow", "left")
            .add("IsReverse", "false")
            .add("教師超鐘點顯示", "顯示")
            .add("教師姓名", "正常顯示")
            .add("學生能檢視的課程", "學生能檢視整天的課程")
            .add("檢視權限設定", "")
            .add("是否顯示午休", "隱藏")
            .add("是否顯示早自習", "隱藏")
            .add("是否顯示節次時間", "顯示")
            .add("顯示科目名稱", "全名")
            .add("是否顯示總時數", "否")
            .add("是否顯示實施日期", "否")

    private fun parseOptionList(json: String): List<Pair<String, String>> {
        val root = runCatching { SchoolJson.parseToJsonElement(json) }.getOrNull()
        val rootArray = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                (root["Data"] ?: root["data"] ?: root["Result"] ?: root["result"] ?: root["items"]) as? JsonArray ?: JsonArray(emptyList())
            }
            else -> JsonArray(emptyList())
        }
        return rootArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val text = (obj["Text"] ?: obj["text"] ?: obj["DisplayText"])?.let { if (it is JsonPrimitive) it.content else null }
            val value = (obj["Value"] ?: obj["value"])?.let { if (it is JsonPrimitive) it.content else null }
            if (text != null && value != null) text to value else null
        }
    }

    suspend fun getScheduleYears(session: AuthenticatedSession): List<ScheduleYearTermOption> = withContext(Dispatchers.IO) {
        val token = getSchedulePageToken(session)
        val body = FormBody.Builder()
            .add("__RequestVerificationToken", token)
            .build()
        val json = execute(
            Request.Builder()
                .url(resolve("ClassTableV2/ClassTable/GetYearTermList"))
                .headers(scheduleHeaders())
                .post(body)
                .build(),
        ).body.string()
        parseOptionList(json).map { (text, value) -> ScheduleYearTermOption(text, value) }
    }

    suspend fun getScheduleClasses(
        session: AuthenticatedSession,
        year: String,
        term: String
    ): List<ScheduleClassOption> = withContext(Dispatchers.IO) {
        val token = getSchedulePageToken(session)
        val body = buildScheduleFormDefaults(token)
            .add("Year", year)
            .add("Term", term)
            .add("ClassNo", "")
            .add("TimetableType", "")
            .build()
        val json = execute(
            Request.Builder()
                .url(resolve("ClassTableV2/ClassTable/GetClassNoList"))
                .headers(scheduleHeaders())
                .post(body)
                .build(),
        ).body.string()
        parseOptionList(json).map { (text, value) -> ScheduleClassOption(text, value) }
    }

    suspend fun fetchSchedule(
        session: AuthenticatedSession,
        yearValue: String,
        year: String,
        term: String,
        classNo: String,
    ): ScheduleReport = withContext(Dispatchers.IO) {
        val token = getSchedulePageToken(session)
        val form = buildScheduleFormDefaults(token)
            .add("Year", year)
            .add("Term", term)
            .apply {
                if (classNo.isNotBlank()) {
                    add("ClassNo", classNo)
                    add("TimetableType", "Class")
                } else {
                    add("StudentNo", session.studentNo)
                    add("TimetableType", "Student")
                }
            }
            .build()

        val timetableJson = execute(
            Request.Builder()
                .url(resolve("ClassTableV2/ClassTable/GetTimeTable"))
                .headers(scheduleHeaders())
                .post(form)
                .build(),
        ).body.string()

        val items = parseScheduleItems(timetableJson)

        if (items.isEmpty()) {
            throw SchoolException("無法解析課表資料結構或無課表")
        }

        ScheduleReport(yearTermValue = yearValue, items = items)
    }

    fun restoreSession(session: AuthenticatedSession) {
        cookieJar.replace(session.cookies, domain = baseUrl.host)
        synchronized(sessionLock) {
            currentStudentNo = session.studentNo
            cachedScheduleToken = null
        }
    }

    suspend fun loginWithCookies(
        studentNo: String,
        cookies: Map<String, String>,
    ): AuthenticatedSession = withContext(Dispatchers.IO) {
        cookieJar.replace(cookies, domain = baseUrl.host)
        synchronized(sessionLock) {
            currentStudentNo = studentNo
            cachedScheduleToken = null
        }
        val gradesPage = execute(
            Request.Builder()
                .url(gradesPageUrl())
                .headers(defaultHeaders(referer = resolve("Auth/Auth/CloudLogin").toString()))
                .get()
                .build(),
        ).body.string()
        val apiToken = hiddenInput(gradesPage, "__RequestVerificationToken")
            ?: throw SchoolException("找不到成績 API token，登入狀態可能無效")
        AuthenticatedSession(
            studentNo = studentNo,
            apiToken = apiToken,
            cookies = cookieJar.snapshot(),
        )
    }

    fun clearSession() {
        cookieJar.clear()
        synchronized(sessionLock) {
            currentStudentNo = null
            cachedScheduleToken = null
        }
    }

    private suspend fun loadExams(session: AuthenticatedSession, yearValue: String): List<ExamOption> {
        val (year, term) = parseYearTerm(yearValue, defaultYear = "114", defaultTerm = "1")
        return postOptions(
            path = "ICampus/CommonData/GetGradeCanQueryExamNoListByStudentNo",
            referer = gradesPageUrl().toString(),
            form = mapOf(
                "searchType" to "單次考試所有成績",
                "studentNo" to session.studentNo,
                "year" to year,
                "term" to term,
                "__RequestVerificationToken" to session.apiToken,
            ),
        ).map { (text, value) -> ExamOption(text = text, value = value) }
    }

    private suspend fun postOptions(
        path: String,
        referer: String,
        form: Map<String, String>,
    ): List<Pair<String, String>> = parseOptions(postForm(path, referer, form))

    private suspend fun postForm(
        path: String,
        referer: String,
        form: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().apply {
            form.forEach { (name, value) -> add(name, value) }
        }.build()
        execute(
            Request.Builder()
                .url(resolve(path))
                .headers(
                    defaultHeaders(referer).newBuilder()
                        .add("Origin", origin)
                        .add("X-Requested-With", "XMLHttpRequest")
                        .build(),
                )
                .post(body)
                .build(),
        ).body.string()
    }

    private fun execute(request: Request): Response {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw SchoolException("學校系統回應異常 HTTP $code")
        }
        return response
    }

    private fun hiddenInput(html: String, name: String): String? {
        val doc = Jsoup.parse(html)
        val element = doc.selectFirst("""[name="$name"]""")
        return (element?.attr("value") ?: element?.text())?.trim()?.takeIf { it.isNotBlank() }
    }


    private fun gradesPageUrl(): HttpUrl = resolve("ICampus/StudentInfo/Index")
        .newBuilder()
        .addQueryParameter("page", "成績查詢")
        .build()

    private fun resolve(path: String): HttpUrl = baseUrl.resolve(path)
        ?: throw IllegalArgumentException("Invalid path: $path")

    private fun defaultHeaders(referer: String?): Headers = Headers.Builder()
        .add("Accept", "*/*")
        .add("User-Agent", USER_AGENT)
        .apply {
            if (!referer.isNullOrBlank()) add("Referer", referer)
        }
        .build()

    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"


    companion object {
        const val DEFAULT_BASE_URL = "https://shcloud2.k12ea.gov.tw/CLHSTYC"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
