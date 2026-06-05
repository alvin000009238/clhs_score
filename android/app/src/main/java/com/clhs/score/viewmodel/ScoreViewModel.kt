package com.clhs.score.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clhs.score.data.AuthenticatedSession
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.GradeExporter

import com.clhs.score.data.GradeAnalysis
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeRepository
import com.clhs.score.data.GradeTrend
import com.clhs.score.data.LocalScoreInsightProvider
import com.clhs.score.data.SchoolException
import com.clhs.score.data.SchoolGradeClient
import com.clhs.score.data.SchoolGradeRepository
import com.clhs.score.data.ScoreInsightProvider
import com.clhs.score.data.ScoreInsightSet
import com.clhs.score.data.SessionStore
import com.clhs.score.data.SimulationHistorySource
import com.clhs.score.data.YearTermOption
import com.clhs.score.data.buildGradeAnalysis
import com.clhs.score.data.buildGradeTrend
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.latestExam
import com.clhs.score.data.latestYearTerm
import com.clhs.score.data.sameTermHistorySource
import com.clhs.score.data.simulationHistorySource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class HistoricalExamRequest(
    val yearValue: String,
    val examValue: String,
    val examName: String,
)

data class LoginUiState(
    val isWebViewLoginInProgress: Boolean = false,
    val errorMessage: String? = null,
)

data class GradesUiState(
    val isLoggedIn: Boolean = false,
    val studentNo: String = "",
    val isLoadingStructure: Boolean = false,
    val isLoadingGrades: Boolean = false,
    val isLoadingComparison: Boolean = false,
    val isLoadingTrend: Boolean = false,
    val structure: List<YearTermOption> = emptyList(),
    val selectedYearValue: String? = null,
    val selectedExamValue: String? = null,
    val report: GradeReport? = null,
    val comparisonReport: GradeReport? = null,
    val comparisonExamName: String? = null,
    val comparisonError: String? = null,
    val trendReports: List<GradeReport> = emptyList(),
    val trendError: String? = null,
    val trendHistoryLabel: String? = null,
    val trend: GradeTrend? = null,
    val isLoadingSimulatorHistory: Boolean = false,
    val simulatorHistoryReports: List<GradeReport> = emptyList(),
    val simulatorHistoryLabel: String? = null,
    val insights: ScoreInsightSet? = null,
    val analysis: GradeAnalysis? = null,
    val expandedSubjectKeys: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val exportResult: String? = null,
)

data class SubjectTrendUiState(
    val selectedYearValues: Set<String> = emptySet(),
    val selectedSubjectKeys: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val reports: List<GradeReport> = emptyList(),
    val errorMessage: String? = null,
)

class ScoreViewModel(
    private val repository: GradeRepository,
    private val insightProvider: ScoreInsightProvider = LocalScoreInsightProvider(),
) : ViewModel() {
    private var session: AuthenticatedSession? = null
    private var gradeRequestId = 0

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState

    private val _gradesState = MutableStateFlow(GradesUiState())
    val gradesState: StateFlow<GradesUiState> = _gradesState

    private val _subjectTrendState = MutableStateFlow(SubjectTrendUiState())
    val subjectTrendState: StateFlow<SubjectTrendUiState> = _subjectTrendState

    init {
        restoreSession()
    }



    fun selectYear(value: String) {
        val year = _gradesState.value.structure.firstOrNull { it.value == value } ?: return
        val latestExam = year.latestExam()
        gradeRequestId++
        _gradesState.update {
            it.copy(
                selectedYearValue = value,
                selectedExamValue = latestExam?.value,
                comparisonReport = null,
                comparisonExamName = null,
                comparisonError = null,
                isLoadingTrend = false,
                isLoadingSimulatorHistory = false,
                trendReports = emptyList(),
                trendError = null,
                trendHistoryLabel = null,
                trend = null,
                simulatorHistoryReports = emptyList(),
                simulatorHistoryLabel = null,
                insights = null,
                expandedSubjectKeys = emptySet(),
                errorMessage = null,
            )
        }
        if (latestExam != null) {
            fetchGrades(value, latestExam.value)
        }
    }

    fun selectExam(value: String) {
        val yearValue = _gradesState.value.selectedYearValue ?: return
        gradeRequestId++
        _gradesState.update {
            it.copy(
                selectedExamValue = value,
                comparisonReport = null,
                comparisonExamName = null,
                comparisonError = null,
                isLoadingTrend = false,
                isLoadingSimulatorHistory = false,
                trendReports = emptyList(),
                trendError = null,
                trendHistoryLabel = null,
                trend = null,
                simulatorHistoryReports = emptyList(),
                simulatorHistoryLabel = null,
                insights = null,
                expandedSubjectKeys = emptySet(),
                errorMessage = null,
            )
        }
        fetchGrades(yearValue, value)
    }

    fun toggleSubjectExpanded(subjectName: String) {
        val key = cleanSubjectName(subjectName)
        _gradesState.update { state ->
            val next = if (key in state.expandedSubjectKeys) {
                state.expandedSubjectKeys - key
            } else {
                state.expandedSubjectKeys + key
            }
            state.copy(expandedSubjectKeys = next)
        }
    }

    fun reloadStructure() {
        loadStructure(forceRefresh = true)
    }

    fun exportGrades(selections: List<ExamSelection>, context: Context) {
        val currentSession = session ?: return
        viewModelScope.launch {
            _gradesState.update { it.copy(isExporting = true, exportResult = null) }
            runCatching {
                val reports = coroutineScope {
                    selections.map { sel ->
                        async {
                            sel to repository.fetchGrades(
                                currentSession, sel.yearValue, sel.examValue, false,
                            )
                        }
                    }.awaitAll()
                }
                val csvPairs = reports.map { (sel, report) -> sel.displayName to report }
                val csv = GradeExporter.buildCsvContent(csvPairs)
                GradeExporter.saveCsvToDownloads(
                    context = context,
                    csv = csv,
                    studentNo = currentSession.studentNo,
                ).getOrThrow()
            }.onSuccess { fileName ->
                _gradesState.update {
                    it.copy(isExporting = false, exportResult = "已儲存至 Downloads/$fileName")
                }
            }.onFailure { error ->
                _gradesState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "匯出失敗：${error.message ?: "未知錯誤"}",
                    )
                }
            }
        }
    }

    fun dismissExportResult() {
        _gradesState.update { it.copy(exportResult = null) }
    }

    fun logout() {
        gradeRequestId++
        viewModelScope.launch {
            repository.logout()
        }
        session = null
        _gradesState.value = GradesUiState()
        _loginState.value = LoginUiState()
    }

    fun loginWithWebViewCookies(studentNo: String, cookieString: String) {
        viewModelScope.launch {
            _loginState.update { it.copy(isWebViewLoginInProgress = true, errorMessage = null) }
            runCatching {
                val cookies = parseCookieString(cookieString)
                if (cookies.isEmpty()) throw SchoolException("未取得有效的登入 cookies")
                repository.loginWithCookies(studentNo, cookies)
            }.onSuccess { authenticatedSession ->
                session = authenticatedSession
                _loginState.update {
                    it.copy(isWebViewLoginInProgress = false, errorMessage = null)
                }
                _gradesState.update {
                    it.copy(
                        isLoggedIn = true,
                        studentNo = authenticatedSession.studentNo,
                        errorMessage = null,
                    )
                }
                loadStructure()
            }.onFailure { error ->
                _loginState.update {
                    it.copy(
                        isWebViewLoginInProgress = false,
                        errorMessage = error.message ?: "WebView 登入後處理失敗",
                    )
                }
            }
        }
    }

    private fun parseCookieString(cookieString: String): Map<String, String> {
        if (cookieString.isBlank()) return emptyMap()
        return cookieString.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate { part ->
                val idx = part.indexOf('=')
                part.substring(0, idx).trim() to part.substring(idx + 1).trim()
            }
            .filterKeys { it.isNotBlank() }
    }

    fun clearLoginError() {
        _loginState.update { it.copy(errorMessage = null) }
    }

    fun clearGradesError() {
        _gradesState.update { it.copy(errorMessage = null) }
    }

    private fun restoreSession() {
        val restored = repository.restoreSession()
        if (restored == null) {
            return
        }
        session = restored
        _gradesState.update {
            it.copy(isLoggedIn = true, studentNo = restored.studentNo)
        }
        loadStructure()
    }

    private fun loadStructure(forceRefresh: Boolean = false) {
        val currentSession = session ?: return
        viewModelScope.launch {
            _gradesState.update { it.copy(isLoadingStructure = true, errorMessage = null) }
            runCatching { repository.loadStructure(currentSession, forceRefresh) }
                .onSuccess { structure ->
                    val selectedYear = structure.latestYearTerm()
                    val selectedExam = selectedYear?.latestExam()
                    _gradesState.update {
                        it.copy(
                            isLoadingStructure = false,
                            structure = structure,
                            selectedYearValue = selectedYear?.value,
                            selectedExamValue = selectedExam?.value,
                            errorMessage = null,
                        )
                    }
                    if (selectedYear != null && selectedExam != null) {
                        fetchGrades(selectedYear.value, selectedExam.value, forceRefresh)
                    }
                }
                .onFailure { error ->
                    _gradesState.update {
                        it.copy(
                            isLoadingStructure = false,
                            errorMessage = error.message ?: "載入可查詢考試失敗",
                        )
                    }
                }
        }
    }

    private fun fetchGrades(yearValue: String, examValue: String, forceRefresh: Boolean = false) {
        val currentSession = session ?: return
        val requestId = ++gradeRequestId
        viewModelScope.launch {
            _gradesState.update {
                it.copy(
                    isLoadingGrades = true,
                    isLoadingComparison = false,
                    isLoadingTrend = false,
                    isLoadingSimulatorHistory = false,
                    comparisonReport = null,
                    comparisonExamName = null,
                    comparisonError = null,
                    trendReports = emptyList(),
                    trendError = null,
                    trendHistoryLabel = null,
                    trend = null,
                    simulatorHistoryReports = emptyList(),
                    simulatorHistoryLabel = null,
                    insights = null,
                    errorMessage = null,
                )
            }
            runCatching { repository.fetchGrades(currentSession, yearValue, examValue, forceRefresh) }
                .onSuccess { report ->
                    if (requestId != gradeRequestId) return@onSuccess
                    val analysis = buildGradeAnalysis(report)
                    _gradesState.update {
                        it.copy(
                            isLoadingGrades = false,
                            report = report,
                            comparisonReport = null,
                            comparisonExamName = null,
                            comparisonError = null,
                            trendReports = emptyList(),
                            trendError = null,
                            trendHistoryLabel = null,
                            trend = null,
                            simulatorHistoryReports = emptyList(),
                            simulatorHistoryLabel = null,
                            analysis = analysis,
                            insights = insightProvider.buildInsights(report, analysis),
                            errorMessage = null,
                        )
                    }
                    loadHistoricalGrades(
                        requestId = requestId,
                        session = currentSession,
                        yearValue = yearValue,
                        examValue = examValue,
                        report = report,
                        forceRefresh = forceRefresh,
                    )
                }
                .onFailure { error ->
                    if (requestId != gradeRequestId) return@onFailure
                    _gradesState.update {
                        it.copy(
                            isLoadingGrades = false,
                            isLoadingComparison = false,
                            isLoadingTrend = false,
                            isLoadingSimulatorHistory = false,
                            errorMessage = error.message ?: "查詢成績失敗",
                        )
                    }
                }
        }
    }

    private fun loadHistoricalGrades(
        requestId: Int,
        session: AuthenticatedSession,
        yearValue: String,
        examValue: String,
        report: GradeReport,
        forceRefresh: Boolean = false,
    ) {
        val structure = _gradesState.value.structure
        val year = structure.firstOrNull { it.value == yearValue }
        val currentExamName = year?.exams
            ?.firstOrNull { it.value == examValue }
            ?.text
            ?: report.examSummary?.examName.orEmpty().ifBlank { "本次考試" }
        val trendSource = structure.sameTermHistorySource(yearValue, examValue)
        val simulatorSource = structure.simulationHistorySource(yearValue, examValue)
        if (trendSource == null && simulatorSource == null) {
            _gradesState.update {
                val analysis = it.analysis ?: buildGradeAnalysis(report)
                if (requestId != gradeRequestId) it else it.copy(
                    isLoadingComparison = false,
                    isLoadingTrend = false,
                    isLoadingSimulatorHistory = false,
                    comparisonError = "尚無上一考可比較",
                    trendReports = emptyList(),
                    trend = null,
                    trendError = "尚無當學期歷次趨勢可比較",
                    trendHistoryLabel = null,
                    simulatorHistoryReports = emptyList(),
                    simulatorHistoryLabel = null,
                    insights = insightProvider.buildInsights(report, analysis, null),
                )
            }
            return
        }

        viewModelScope.launch {
            _gradesState.update {
                if (requestId != gradeRequestId) it else it.copy(
                    isLoadingComparison = trendSource != null,
                    isLoadingTrend = trendSource != null,
                    isLoadingSimulatorHistory = simulatorSource != null,
                    comparisonError = null,
                    trendError = if (trendSource == null) "尚無當學期歷次趨勢可比較" else null,
                )
            }
            runCatching {
                val requests = historicalRequests(trendSource, simulatorSource)
                coroutineScope {
                    requests.map { request ->
                        async {
                            request to repository.fetchGrades(session, request.yearValue, request.examValue, forceRefresh)
                        }
                    }.awaitAll().toMap()
                }
            }.onSuccess { reportsByRequest ->
                if (requestId != gradeRequestId) return@onSuccess
                val trendPairs = trendSource?.historyExams.orEmpty().mapNotNull { historyExam ->
                    val request = HistoricalExamRequest(historyExam.yearValue, historyExam.examValue, historyExam.examName)
                    reportsByRequest[request]?.let { historyExam.examName to it }
                }
                val simulatorReports = simulatorSource?.historyExams.orEmpty().mapNotNull { historyExam ->
                    val request = HistoricalExamRequest(historyExam.yearValue, historyExam.examValue, historyExam.examName)
                    reportsByRequest[request]
                }
                val comparison = trendPairs.lastOrNull()
                val trend = trendPairs.takeIf { it.isNotEmpty() }?.let {
                    buildGradeTrend(
                        currentExamName = currentExamName,
                        currentReport = report,
                        previousReports = it,
                    )
                }
                _gradesState.update {
                    val analysis = if (comparison != null) {
                        buildGradeAnalysis(
                            report = report,
                            comparisonReport = comparison.second,
                            previousExamName = comparison.first,
                        )
                    } else {
                        buildGradeAnalysis(report)
                    }
                    it.copy(
                        isLoadingComparison = false,
                        isLoadingTrend = false,
                        isLoadingSimulatorHistory = false,
                        comparisonReport = comparison?.second,
                        comparisonExamName = comparison?.first,
                        comparisonError = if (comparison == null) "尚無上一考可比較" else null,
                        trendReports = trendPairs.map { pair -> pair.second },
                        trendError = if (trend == null) "尚無當學期歷次趨勢可比較" else null,
                        trendHistoryLabel = trendSource?.label,
                        trend = trend,
                        simulatorHistoryReports = simulatorReports,
                        simulatorHistoryLabel = simulatorSource?.label,
                        insights = insightProvider.buildInsights(report, analysis, trend),
                        analysis = analysis,
                    )
                }
            }.onFailure { error ->
                if (requestId != gradeRequestId) return@onFailure
                _gradesState.update {
                    val analysis = it.analysis ?: buildGradeAnalysis(report)
                    it.copy(
                        isLoadingComparison = false,
                        isLoadingTrend = false,
                        isLoadingSimulatorHistory = false,
                        comparisonError = error.message ?: "歷次資料載入失敗",
                        trendReports = emptyList(),
                        trend = null,
                        trendError = error.message ?: "歷次趨勢載入失敗",
                        trendHistoryLabel = null,
                        simulatorHistoryReports = emptyList(),
                        simulatorHistoryLabel = null,
                        insights = insightProvider.buildInsights(report, analysis, null),
                    )
                }
            }
        }
    }

    private fun historicalRequests(
        trendSource: SimulationHistorySource?,
        simulatorSource: SimulationHistorySource?,
    ): List<HistoricalExamRequest> {
        val requests = buildList {
            trendSource?.historyExams?.forEach { historyExam ->
                add(HistoricalExamRequest(historyExam.yearValue, historyExam.examValue, historyExam.examName))
            }
            simulatorSource?.historyExams?.forEach { historyExam ->
                add(HistoricalExamRequest(historyExam.yearValue, historyExam.examValue, historyExam.examName))
            }
        }
        return requests.distinctBy { it.yearValue to it.examValue }
    }

    private var isSubjectTrendInitialized = false

    fun initSubjectTrend() {
        if (isSubjectTrendInitialized) return
        isSubjectTrendInitialized = true
        
        val structure = _gradesState.value.structure
        val allYears = structure.map { it.value }.toSet()
        val defaultSubjects = emptySet<String>()
        _subjectTrendState.update {
            it.copy(
                selectedYearValues = allYears,
                selectedSubjectKeys = defaultSubjects,
            )
        }
        fetchSubjectTrendGrades()
    }

    fun toggleSubjectTrendYear(yearValue: String) {
        _subjectTrendState.update { state ->
            val next = if (yearValue in state.selectedYearValues) {
                state.selectedYearValues - yearValue
            } else {
                state.selectedYearValues + yearValue
            }
            state.copy(selectedYearValues = next)
        }
        fetchSubjectTrendGrades()
    }

    fun toggleSubjectTrendSubject(subjectKey: String) {
        _subjectTrendState.update { state ->
            val next = if (subjectKey in state.selectedSubjectKeys) {
                state.selectedSubjectKeys - subjectKey
            } else {
                state.selectedSubjectKeys + subjectKey
            }
            state.copy(selectedSubjectKeys = next)
        }
    }

    private fun fetchSubjectTrendGrades() {
        val currentSession = session ?: return
        val structure = _gradesState.value.structure
        val selectedYears = _subjectTrendState.value.selectedYearValues
        
        val requests = buildList {
            structure.filter { it.value in selectedYears }
                .sortedWith(compareBy({ com.clhs.score.data.parseYearTerm(it.value, "0", "0").first.toIntOrNull() ?: 0 }, { com.clhs.score.data.parseYearTerm(it.value, "0", "0").second.toIntOrNull() ?: 0 }))
                .forEach { yearTerm ->
                    yearTerm.exams.forEach { exam ->
                        add(HistoricalExamRequest(yearTerm.value, exam.value, exam.text))
                    }
                }
        }
        
        if (requests.isEmpty()) {
            _subjectTrendState.update { it.copy(reports = emptyList(), errorMessage = null) }
            return
        }

        viewModelScope.launch {
            _subjectTrendState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                coroutineScope {
                    requests.map { request ->
                        async {
                            repository.fetchGrades(currentSession, request.yearValue, request.examValue, false)
                        }
                    }.awaitAll()
                }
            }.onSuccess { reports ->
                _subjectTrendState.update {
                    it.copy(
                        isLoading = false,
                        reports = reports,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _subjectTrendState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "載入折線圖資料失敗",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(context: Context, useFakeData: Boolean = false): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = if (useFakeData) {
                    com.clhs.score.data.FakeGradeRepository()
                } else {
                    val cookieJar = com.clhs.score.data.SchoolCookieJar()
                    val client = SchoolGradeClient(cookieJar = cookieJar)
                    SchoolGradeRepository(client, SessionStore(context), com.clhs.score.data.GradeCacheStore(context))
                }
                return ScoreViewModel(repository) as T
            }
        }
    }
}
