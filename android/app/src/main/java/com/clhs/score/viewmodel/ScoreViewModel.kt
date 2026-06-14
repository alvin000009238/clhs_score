package com.clhs.score.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clhs.score.analytics.AnalyticsEvents
import com.clhs.score.analytics.AnalyticsLogger
import com.clhs.score.analytics.AnalyticsParameterSanitizer
import com.clhs.score.analytics.AnalyticsParams
import com.clhs.score.analytics.AnalyticsValues
import com.clhs.score.analytics.FirebaseAnalyticsLogger
import com.clhs.score.analytics.NoOpAnalyticsLogger
import com.clhs.score.data.AuthenticatedSession
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.GradeExporter

import com.clhs.score.data.GradeAnalysis
import com.clhs.score.data.GradeChangeSet
import com.clhs.score.data.GradeReminderRepository
import com.clhs.score.data.GradeReminderState
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeRepository
import com.clhs.score.data.GradeReportDiffer
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
import com.clhs.score.data.parseYearTerm
import com.clhs.score.data.sameTermHistorySource
import com.clhs.score.data.simulationHistorySource
import com.clhs.score.reminders.GradeReminderScheduler
import kotlinx.coroutines.CancellationException
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
    val gradeReminderState: GradeReminderState = GradeReminderState(),
    val isStartingGradeReminder: Boolean = false,
    val gradeReminderError: String? = null,
    val gradeReminderChangeSet: GradeChangeSet? = null,
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
    private val appContext: Context? = null,
    private val sessionStore: SessionStore? = null,
    private val gradeReminderRepository: GradeReminderRepository? = null,
    private val gradeReminderScheduler: GradeReminderScheduler? = null,
    private val analyticsLogger: AnalyticsLogger = NoOpAnalyticsLogger,
) : ViewModel() {
    private var session: AuthenticatedSession? = null
    private var gradeRequestId = 0
    private var subjectTrendRequestId = 0
    private var pendingReminderTarget: Pair<String, String>? = null
    private var ensuredGradeReminderWorkKey: String? = null

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState

    private val _gradesState = MutableStateFlow(GradesUiState())
    val gradesState: StateFlow<GradesUiState> = _gradesState

    private val _subjectTrendState = MutableStateFlow(SubjectTrendUiState())
    val subjectTrendState: StateFlow<SubjectTrendUiState> = _subjectTrendState

    init {
        observeGradeReminderState()
        restoreSession()
    }

    private fun observeGradeReminderState() {
        val reminderRepository = gradeReminderRepository ?: return
        viewModelScope.launch {
            reminderRepository.state.collect { reminderState ->
                val now = System.currentTimeMillis()
                if (reminderState.enabled && !reminderState.isActive(now)) {
                    ensuredGradeReminderWorkKey = null
                    reminderRepository.stop("段考提醒已超過 48 小時")
                    gradeReminderScheduler?.cancel()
                    sessionStore?.clearReminderSession()
                    _gradesState.update {
                        it.copy(
                            gradeReminderState = GradeReminderState(stoppedReason = "段考提醒已超過 48 小時"),
                            gradeReminderChangeSet = null,
                        )
                    }
                    return@collect
                }

                val activeWorkKey = reminderState.activeWorkKey(now)
                if (activeWorkKey != null && activeWorkKey != ensuredGradeReminderWorkKey) {
                    gradeReminderScheduler?.schedule()
                    ensuredGradeReminderWorkKey = activeWorkKey
                } else if (activeWorkKey == null) {
                    ensuredGradeReminderWorkKey = null
                }
                _gradesState.update { it.copy(gradeReminderState = reminderState) }
            }
        }
    }



    fun selectYear(value: String) {
        val year = _gradesState.value.structure.firstOrNull { it.value == value } ?: return
        val latestExam = year.latestExam()
        gradeRequestId++
        _gradesState.update {
            it.copy(
                selectedYearValue = value,
                selectedExamValue = latestExam?.value,
                isLoadingGrades = false,
                isLoadingComparison = false,
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
                report = null,
                analysis = null,
                insights = null,
                expandedSubjectKeys = emptySet(),
                errorMessage = null,
                gradeReminderChangeSet = null,
            )
        }
        if (latestExam != null) {
            fetchGrades(value, latestExam.value, analyticsTrigger = AnalyticsValues.TRIGGER_YEAR_SELECT)
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
                gradeReminderChangeSet = null,
            )
        }
        fetchGrades(yearValue, value, analyticsTrigger = AnalyticsValues.TRIGGER_EXAM_SELECT)
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
                analyticsLogger.logEvent(
                    AnalyticsEvents.EXPORT_GRADES,
                    mapOf(
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                        AnalyticsParams.SELECTION_COUNT_BUCKET to AnalyticsParameterSanitizer.countBucket(selections.size),
                    ),
                )
                _gradesState.update {
                    it.copy(isExporting = false, exportResult = "已儲存至 Downloads/$fileName")
                }
            }.onFailure { error ->
                error.throwIfCancellation()
                analyticsLogger.logEvent(
                    AnalyticsEvents.EXPORT_GRADES,
                    mapOf(
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                        AnalyticsParams.SELECTION_COUNT_BUCKET to AnalyticsParameterSanitizer.countBucket(selections.size),
                    ),
                )
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

    fun logout(source: String = AnalyticsValues.SOURCE_SETTINGS) {
        analyticsLogger.logEvent(
            AnalyticsEvents.LOGOUT,
            mapOf(AnalyticsParams.SOURCE to source),
        )
        gradeRequestId++
        ensuredGradeReminderWorkKey = null
        resetSubjectTrendState()
        val sessionToClear = session
        viewModelScope.launch {
            repository.logout(sessionToClear)
            gradeReminderRepository?.stop("使用者登出")
            gradeReminderScheduler?.cancel()
            sessionStore?.clearReminderSession()
        }
        session = null
        _gradesState.value = GradesUiState()
        _loginState.value = LoginUiState()
    }

    fun getCurrentSession(): AuthenticatedSession? = session

    fun loginWithBiometricSession(restored: AuthenticatedSession) {
        session = restored
        analyticsLogger.logEvent(
            AnalyticsEvents.LOGIN_RESULT,
            mapOf(
                AnalyticsParams.METHOD to AnalyticsValues.METHOD_BIOMETRIC,
                AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
            ),
        )
        _gradesState.update {
            it.copy(
                isLoggedIn = true,
                studentNo = restored.studentNo,
                errorMessage = null,
            )
        }
        repository.activateSession(restored)
        loadStructure()
    }

    fun loginWithWebViewCookies(studentNo: String, cookieString: String) {
        val method = if (cookieString == "fake=cookie") {
            AnalyticsValues.METHOD_DEMO
        } else {
            AnalyticsValues.METHOD_WEBVIEW
        }
        analyticsLogger.logEvent(
            AnalyticsEvents.LOGIN_START,
            mapOf(AnalyticsParams.METHOD to method),
        )
        viewModelScope.launch {
            _loginState.update { it.copy(isWebViewLoginInProgress = true, errorMessage = null) }
            runCatching {
                val cookies = parseCookieString(cookieString)
                if (cookies.isEmpty()) throw SchoolException("未取得有效的登入 cookies")
                repository.loginWithCookies(studentNo, cookies)
            }.onSuccess { authenticatedSession ->
                resetSubjectTrendState()
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
                analyticsLogger.logEvent(
                    AnalyticsEvents.LOGIN_RESULT,
                    mapOf(
                        AnalyticsParams.METHOD to method,
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                    ),
                )
            }.onFailure { error ->
                error.throwIfCancellation()
                analyticsLogger.logEvent(
                    AnalyticsEvents.LOGIN_RESULT,
                    mapOf(
                        AnalyticsParams.METHOD to method,
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                        AnalyticsParams.ERROR_TYPE to error.toLoginErrorType(),
                    ),
                )
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

    fun clearGradeReminderError() {
        _gradesState.update { it.copy(gradeReminderError = null) }
    }

    fun reportGradeReminderPrerequisiteError(message: String) {
        analyticsLogger.logEvent(
            AnalyticsEvents.GRADE_REMINDER_START,
            mapOf(
                AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                AnalyticsParams.FAILURE_REASON to message.toGradeReminderFailureReason(),
            ),
        )
        setGradeReminderError(message)
    }

    fun dismissGradeReminderChanges() {
        viewModelScope.launch {
            gradeReminderRepository?.clearLatestChangeSet()
        }
        _gradesState.update { it.copy(gradeReminderChangeSet = null) }
    }

    fun startGradeReminder() {
        val currentSession = session ?: run {
            logGradeReminderStartFailure(AnalyticsValues.REASON_UNKNOWN)
            setGradeReminderError("請先登入後再啟用段考提醒")
            return
        }
        val context = appContext ?: run {
            logGradeReminderStartFailure(AnalyticsValues.REASON_UNKNOWN)
            setGradeReminderError("目前環境不支援背景段考提醒")
            return
        }
        val reminderRepository = gradeReminderRepository ?: run {
            logGradeReminderStartFailure(AnalyticsValues.REASON_UNKNOWN)
            setGradeReminderError("目前環境不支援背景段考提醒")
            return
        }
        val scheduler = gradeReminderScheduler ?: run {
            logGradeReminderStartFailure(AnalyticsValues.REASON_UNKNOWN)
            setGradeReminderError("目前環境不支援背景段考提醒")
            return
        }
        val state = _gradesState.value
        val yearValue = state.selectedYearValue ?: run {
            logGradeReminderStartFailure(AnalyticsValues.REASON_NO_EXAM)
            setGradeReminderError("請先選擇學期")
            return
        }
        val examValue = state.selectedExamValue ?: run {
            logGradeReminderStartFailure(AnalyticsValues.REASON_NO_EXAM)
            setGradeReminderError("請先選擇考試")
            return
        }
        val selectedYear = state.structure.firstOrNull { it.value == yearValue }
        val selectedExam = selectedYear?.exams?.firstOrNull { it.value == examValue }
        val requestId = ++gradeRequestId
        viewModelScope.launch {
            _gradesState.update {
                it.copy(
                    isStartingGradeReminder = true,
                    isLoadingGrades = true,
                    gradeReminderError = null,
                    gradeReminderChangeSet = null,
                    errorMessage = null,
                )
            }
            runCatching {
                val cacheStore = GradeCacheStore(context)
                val oldReport = cacheStore.loadGradeReport(currentSession.studentNo, yearValue, examValue)
                val report = repository.fetchGrades(currentSession, yearValue, examValue, forceRefresh = true)
                check(requestId == gradeRequestId) { "提醒啟用請求已過期" }
                val now = System.currentTimeMillis()
                val oldSnapshot = oldReport?.let(GradeReportDiffer::snapshot)
                val newSnapshot = GradeReportDiffer.snapshot(report)
                val changeSet = oldSnapshot?.let { before ->
                    GradeReportDiffer.diff(
                        before = before,
                        after = newSnapshot,
                        studentNo = currentSession.studentNo,
                        yearValue = yearValue,
                        examValue = examValue,
                        examName = selectedExam?.text ?: report.examSummary?.examName.orEmpty().ifBlank { "本次考試" },
                        checkedAtMillis = now,
                    ).takeIf { it.hasChanges }
                }
                val expiresAtMillis = now + GRADE_REMINDER_DURATION_MILLIS
                sessionStore?.saveReminderSession(currentSession, expiresAtMillis)
                reminderRepository.saveState(
                    GradeReminderState(
                        enabled = true,
                        studentNo = currentSession.studentNo,
                        yearValue = yearValue,
                        yearLabel = selectedYear?.text.orEmpty(),
                        examValue = examValue,
                        examName = selectedExam?.text ?: report.examSummary?.examName.orEmpty().ifBlank { "本次考試" },
                        activatedAtMillis = now,
                        expiresAtMillis = expiresAtMillis,
                        lastCheckedAtMillis = now,
                        snapshot = newSnapshot,
                        latestChangeSet = changeSet,
                    ),
                )
                scheduler.schedule()
                ensuredGradeReminderWorkKey = gradeReminderWorkKey(
                    studentNo = currentSession.studentNo,
                    yearValue = yearValue,
                    examValue = examValue,
                    expiresAtMillis = expiresAtMillis,
                )
                report to changeSet
            }.onSuccess { (report, changeSet) ->
                if (requestId != gradeRequestId) return@onSuccess
                analyticsLogger.logEvent(
                    AnalyticsEvents.GRADE_REMINDER_START,
                    mapOf(AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS),
                )
                applyFetchedReportAndLoadHistory(
                    requestId = requestId,
                    currentSession = currentSession,
                    yearValue = yearValue,
                    examValue = examValue,
                    report = report,
                    forceRefresh = true,
                    gradeReminderChangeSet = changeSet,
                )
                _gradesState.update {
                    it.copy(
                        isStartingGradeReminder = false,
                        gradeReminderError = null,
                    )
                }
            }.onFailure { error ->
                error.throwIfCancellation()
                if (requestId != gradeRequestId) return@onFailure
                logGradeReminderStartFailure(AnalyticsValues.REASON_UNKNOWN)
                _gradesState.update {
                    it.copy(
                        isStartingGradeReminder = false,
                        isLoadingGrades = false,
                        gradeReminderError = error.message ?: "啟用段考提醒失敗",
                    )
                }
            }
        }
    }

    fun stopGradeReminder() {
        analyticsLogger.logEvent(
            AnalyticsEvents.GRADE_REMINDER_STOP,
            mapOf(AnalyticsParams.REASON to AnalyticsValues.REASON_USER),
        )
        ensuredGradeReminderWorkKey = null
        viewModelScope.launch {
            gradeReminderRepository?.stop("使用者關閉")
            gradeReminderScheduler?.cancel()
            sessionStore?.clearReminderSession()
        }
        _gradesState.update {
            it.copy(
                gradeReminderState = GradeReminderState(stoppedReason = "使用者關閉"),
                gradeReminderChangeSet = null,
            )
        }
    }

    fun openGradeReminderTarget(yearValue: String, examValue: String) {
        analyticsLogger.logEvent(AnalyticsEvents.GRADE_REMINDER_NOTIFICATION_OPEN)
        pendingReminderTarget = yearValue to examValue
        _gradesState.update {
            it.copy(gradeReminderChangeSet = it.gradeReminderState.latestChangeSet)
        }
        openPendingReminderTargetOrLoadStructure(forceRefreshStructure = true)
    }

    private fun openPendingReminderTargetOrLoadStructure(forceRefreshStructure: Boolean = false) {
        val target = pendingReminderTarget ?: return
        val structure = _gradesState.value.structure
        val year = structure.firstOrNull { it.value == target.first }
        val exam = year?.exams?.firstOrNull { it.value == target.second }
        if (year != null && exam != null) {
            pendingReminderTarget = null
            gradeRequestId++
            _gradesState.update {
                it.copy(
                    selectedYearValue = year.value,
                    selectedExamValue = exam.value,
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
            fetchGrades(
                year.value,
                exam.value,
                forceRefresh = true,
                analyticsTrigger = AnalyticsValues.TRIGGER_REMINDER_TARGET,
            )
        } else {
            loadStructure(forceRefresh = forceRefreshStructure)
        }
    }

    private fun setGradeReminderError(message: String) {
        _gradesState.update { it.copy(gradeReminderError = message) }
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
                    analyticsLogger.logEvent(
                        AnalyticsEvents.GRADE_STRUCTURE_LOAD,
                        mapOf(
                            AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                            AnalyticsParams.YEAR_COUNT to structure.size,
                            AnalyticsParams.EXAM_COUNT_BUCKET to AnalyticsParameterSanitizer.countBucket(
                                structure.sumOf { it.exams.size },
                            ),
                        ),
                    )
                    val pendingTarget = pendingReminderTarget
                    val pendingYear = pendingTarget?.let { target ->
                        structure.firstOrNull { it.value == target.first }
                    }
                    val pendingExam = if (pendingTarget != null) {
                        pendingYear?.exams?.firstOrNull { it.value == pendingTarget.second }
                    } else {
                        null
                    }
                    val selectedYear = pendingYear ?: structure.latestYearTerm()
                    val selectedExam = pendingExam ?: selectedYear?.latestExam()
                    if (pendingYear != null && pendingExam != null) {
                        pendingReminderTarget = null
                    }
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
                        val trigger = when {
                            pendingYear != null && pendingExam != null -> AnalyticsValues.TRIGGER_REMINDER_TARGET
                            forceRefresh -> AnalyticsValues.TRIGGER_REFRESH
                            else -> AnalyticsValues.TRIGGER_INITIAL
                        }
                        fetchGrades(
                            selectedYear.value,
                            selectedExam.value,
                            forceRefresh || (pendingYear != null && pendingExam != null),
                            analyticsTrigger = trigger,
                        )
                    }
                }
                .onFailure { error ->
                    error.throwIfCancellation()
                    analyticsLogger.logEvent(
                        AnalyticsEvents.GRADE_STRUCTURE_LOAD,
                        mapOf(AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE),
                    )
                    _gradesState.update {
                        it.copy(
                            isLoadingStructure = false,
                            errorMessage = error.message ?: "載入可查詢考試失敗",
                        )
                    }
                }
        }
    }

    private fun fetchGrades(
        yearValue: String,
        examValue: String,
        forceRefresh: Boolean = false,
        analyticsTrigger: String = if (forceRefresh) {
            AnalyticsValues.TRIGGER_REFRESH
        } else {
            AnalyticsValues.TRIGGER_INITIAL
        },
    ) {
        val currentSession = session ?: return
        val requestId = ++gradeRequestId
        viewModelScope.launch {
            _gradesState.update {
                it.copy(
                    isLoadingGrades = true,
                    isLoadingComparison = false,
                    isLoadingTrend = false,
                    isLoadingSimulatorHistory = false,
                    report = null,
                    comparisonReport = null,
                    comparisonExamName = null,
                    comparisonError = null,
                    trendReports = emptyList(),
                    trendError = null,
                    trendHistoryLabel = null,
                    trend = null,
                    simulatorHistoryReports = emptyList(),
                    simulatorHistoryLabel = null,
                    analysis = null,
                    insights = null,
                    errorMessage = null,
                )
            }
            runCatching { repository.fetchGrades(currentSession, yearValue, examValue, forceRefresh) }
                .onSuccess { report ->
                    applyFetchedReportAndLoadHistory(
                        requestId = requestId,
                        currentSession = currentSession,
                        yearValue = yearValue,
                        examValue = examValue,
                        report = report,
                        forceRefresh = forceRefresh,
                        analyticsTrigger = analyticsTrigger,
                    )
                }
                .onFailure { error ->
                    error.throwIfCancellation()
                    if (requestId != gradeRequestId) return@onFailure
                    analyticsLogger.logEvent(
                        AnalyticsEvents.GRADE_QUERY,
                        mapOf(
                            AnalyticsParams.TRIGGER to analyticsTrigger,
                            AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                            AnalyticsParams.CACHED to !forceRefresh,
                        ),
                    )
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

    private fun applyFetchedReportAndLoadHistory(
        requestId: Int,
        currentSession: AuthenticatedSession,
        yearValue: String,
        examValue: String,
        report: GradeReport,
        forceRefresh: Boolean = false,
        gradeReminderChangeSet: GradeChangeSet? = _gradesState.value.gradeReminderChangeSet,
        analyticsTrigger: String = if (forceRefresh) {
            AnalyticsValues.TRIGGER_REFRESH
        } else {
            AnalyticsValues.TRIGGER_INITIAL
        },
    ) {
        if (requestId != gradeRequestId) return
        analyticsLogger.logEvent(
            AnalyticsEvents.GRADE_QUERY,
            mapOf(
                AnalyticsParams.TRIGGER to analyticsTrigger,
                AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                AnalyticsParams.CACHED to !forceRefresh,
                AnalyticsParams.SUBJECT_COUNT_BUCKET to AnalyticsParameterSanitizer.countBucket(report.subjects.size),
            ),
        )
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
                gradeReminderChangeSet = gradeReminderChangeSet,
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
                error.throwIfCancellation()
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

        val structure = _gradesState.value.structure
        if (structure.isEmpty()) {
            _subjectTrendState.update {
                it.copy(
                    isLoading = false,
                    reports = emptyList(),
                    errorMessage = null,
                )
            }
            return
        }
        isSubjectTrendInitialized = true

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
        val requestId = ++subjectTrendRequestId
        
        val requests = buildList {
            structure.filter { it.value in selectedYears }
                .sortedBy { yt ->
                    val (y, t) = parseYearTerm(yt.value, "0", "0")
                    (y.toIntOrNull() ?: 0) * 10 + (t.toIntOrNull() ?: 0)
                }
                .forEach { yearTerm ->
                    yearTerm.exams.forEach { exam ->
                        add(HistoricalExamRequest(yearTerm.value, exam.value, exam.text))
                    }
                }
        }
        
        if (requests.isEmpty()) {
            analyticsLogger.logEvent(
                AnalyticsEvents.SUBJECT_TREND_LOAD,
                mapOf(
                    AnalyticsParams.RESULT to AnalyticsValues.RESULT_EMPTY,
                    AnalyticsParams.YEAR_COUNT to selectedYears.size,
                    AnalyticsParams.SUBJECT_COUNT to _subjectTrendState.value.selectedSubjectKeys.size,
                ),
            )
            _subjectTrendState.update {
                if (requestId != subjectTrendRequestId) {
                    it
                } else {
                    it.copy(isLoading = false, reports = emptyList(), errorMessage = null)
                }
            }
            return
        }

        viewModelScope.launch {
            _subjectTrendState.update {
                if (requestId != subjectTrendRequestId) it else it.copy(isLoading = true, errorMessage = null)
            }
            runCatching {
                coroutineScope {
                    requests.map { request ->
                        async {
                            repository.fetchGrades(currentSession, request.yearValue, request.examValue, false)
                        }
                    }.awaitAll()
                }
            }.onSuccess { reports ->
                if (requestId != subjectTrendRequestId) return@onSuccess
                analyticsLogger.logEvent(
                    AnalyticsEvents.SUBJECT_TREND_LOAD,
                    mapOf(
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                        AnalyticsParams.YEAR_COUNT to selectedYears.size,
                        AnalyticsParams.SUBJECT_COUNT to _subjectTrendState.value.selectedSubjectKeys.size,
                    ),
                )
                _subjectTrendState.update {
                    it.copy(
                        isLoading = false,
                        reports = reports,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                error.throwIfCancellation()
                if (requestId != subjectTrendRequestId) return@onFailure
                analyticsLogger.logEvent(
                    AnalyticsEvents.SUBJECT_TREND_LOAD,
                    mapOf(
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                        AnalyticsParams.YEAR_COUNT to selectedYears.size,
                        AnalyticsParams.SUBJECT_COUNT to _subjectTrendState.value.selectedSubjectKeys.size,
                    ),
                )
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
        private const val GRADE_REMINDER_DURATION_MILLIS = 48L * 60L * 60L * 1000L

        private fun GradeReminderState.activeWorkKey(nowMillis: Long): String? =
            if (isActive(nowMillis)) {
                gradeReminderWorkKey(studentNo, yearValue, examValue, expiresAtMillis)
            } else {
                null
            }

        private fun gradeReminderWorkKey(
            studentNo: String,
            yearValue: String,
            examValue: String,
            expiresAtMillis: Long,
        ): String = listOf(studentNo, yearValue, examValue, expiresAtMillis).joinToString("|")

        fun factory(context: Context, useFakeData: Boolean = false): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                if (useFakeData) {
                    return ScoreViewModel(
                        repository = com.clhs.score.data.FakeGradeRepository(),
                        appContext = appContext,
                        analyticsLogger = FirebaseAnalyticsLogger(appContext),
                    ) as T
                }
                val sessionStore = SessionStore(appContext)
                val cookieJar = com.clhs.score.data.SchoolCookieJar()
                val client = SchoolGradeClient(cookieJar = cookieJar)
                val repository = SchoolGradeRepository(client, sessionStore, com.clhs.score.data.GradeCacheStore(appContext))
                return ScoreViewModel(
                    repository = repository,
                    appContext = appContext,
                    sessionStore = sessionStore,
                    gradeReminderRepository = GradeReminderRepository(appContext),
                    gradeReminderScheduler = GradeReminderScheduler(appContext),
                    analyticsLogger = FirebaseAnalyticsLogger(appContext),
                ) as T
            }
        }
    }

    private fun resetSubjectTrendState() {
        subjectTrendRequestId++
        isSubjectTrendInitialized = false
        _subjectTrendState.value = SubjectTrendUiState()
    }

    private fun logGradeReminderStartFailure(reason: String) {
        analyticsLogger.logEvent(
            AnalyticsEvents.GRADE_REMINDER_START,
            mapOf(
                AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                AnalyticsParams.FAILURE_REASON to reason,
            ),
        )
    }

    private fun Throwable.toLoginErrorType(): String = when (this) {
        is SchoolException -> AnalyticsValues.RESULT_ERROR
        else -> AnalyticsValues.REASON_UNKNOWN
    }

    private fun String.toGradeReminderFailureReason(): String = when {
        contains("通知") -> AnalyticsValues.REASON_PERMISSION
        contains("電池") -> AnalyticsValues.REASON_BATTERY
        contains("考試") || contains("學期") -> AnalyticsValues.REASON_NO_EXAM
        else -> AnalyticsValues.REASON_UNKNOWN
    }

    private fun Throwable.throwIfCancellation() {
        if (this is CancellationException) throw this
    }
}
