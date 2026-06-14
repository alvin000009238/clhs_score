package com.clhs.score.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clhs.score.analytics.AnalyticsEvents
import com.clhs.score.analytics.AnalyticsLogger
import com.clhs.score.analytics.AnalyticsParams
import com.clhs.score.analytics.AnalyticsValues
import com.clhs.score.analytics.FirebaseAnalyticsLogger
import com.clhs.score.analytics.NoOpAnalyticsLogger
import com.clhs.score.data.AuthenticatedSession
import com.clhs.score.data.FakeScheduleRepository
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.NetworkScheduleRepository
import com.clhs.score.data.ScheduleReport
import com.clhs.score.data.ScheduleRepository
import com.clhs.score.data.ScheduleYearTermOption
import com.clhs.score.data.SchoolGradeClient
import com.clhs.score.data.SessionStore
import com.clhs.score.data.parseYearTerm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.clhs.score.data.ScheduleClassOption

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val availableYears: List<ScheduleYearTermOption> = emptyList(),
    val selectedYearValue: String? = null,
    val availableClasses: List<ScheduleClassOption> = emptyList(),
    val selectedClassValue: String? = null,
    val report: ScheduleReport? = null,
    val widgetShowTeacher: Boolean = true,
    val widgetShowClassroom: Boolean = true,
    val widgetShowTime: Boolean = true,
)

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val analyticsLogger: AnalyticsLogger = NoOpAnalyticsLogger,
) : ViewModel() {
    private var scheduleRequestId = 0

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isInitialLoading = true, isError = false) }
            try {
                val prefs = repository.getWidgetPreferences()
                _uiState.update {
                    it.copy(
                        widgetShowTeacher = prefs.first,
                        widgetShowClassroom = prefs.second,
                        widgetShowTime = prefs.third
                    )
                }

                val latest = repository.getLatestSchedule()
                if (latest != null) {
                    _uiState.update { it.copy(isLoading = false, isInitialLoading = false, report = latest) }
                } else {
                    _uiState.update { it.copy(isInitialLoading = false) }
                    loadYears()
                }
            } catch (e: Exception) {
                e.throwIfCancellation()
                _uiState.update { it.copy(isInitialLoading = false) }
                loadYears()
            }
        }
    }

    fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean): Job =
        viewModelScope.launch {
            repository.saveWidgetPreferences(showTeacher, showClassroom, showTime)
            analyticsLogger.logEvent(
                AnalyticsEvents.SCHEDULE_WIDGET_SETTINGS_SAVE,
                mapOf(
                    AnalyticsParams.SHOW_TEACHER to showTeacher,
                    AnalyticsParams.SHOW_CLASSROOM to showClassroom,
                    AnalyticsParams.SHOW_TIME to showTime,
                ),
            )
            _uiState.update {
                it.copy(
                    widgetShowTeacher = showTeacher,
                    widgetShowClassroom = showClassroom,
                    widgetShowTime = showTime
                )
            }
        }

    fun clearSelection() {
        scheduleRequestId++
        _uiState.update { it.copy(report = null) }
        if (_uiState.value.availableYears.isEmpty()) {
            loadYears()
        }
    }

    private fun loadYears() {
        val requestId = ++scheduleRequestId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            try {
                val years = repository.getScheduleYears()
                if (requestId != scheduleRequestId) return@launch
                val selected = years.firstOrNull()?.value
                _uiState.update { 
                    it.copy(
                        availableYears = years,
                        selectedYearValue = selected
                    )
                }
                if (selected != null) {
                    loadClasses(selected)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                e.throwIfCancellation()
                if (requestId != scheduleRequestId) return@launch
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = e.message ?: "載入學期清單失敗"
                    )
                }
            }
        }
    }

    fun selectYear(yearValue: String) {
        if (_uiState.value.selectedYearValue == yearValue) return
        _uiState.update {
            it.copy(
                selectedYearValue = yearValue,
                availableClasses = emptyList(),
                selectedClassValue = null,
                report = null,
            )
        }
        loadClasses(yearValue)
    }
    
    fun selectClass(classValue: String) {
        if (_uiState.value.selectedClassValue == classValue) return
        _uiState.update { it.copy(selectedClassValue = classValue) }
    }

    fun confirmSelection() {
        val yearValue = _uiState.value.selectedYearValue ?: return
        val classValue = _uiState.value.selectedClassValue ?: ""
        loadSchedule(yearValue, classValue)
    }

    private fun loadClasses(yearValue: String) {
        val requestId = ++scheduleRequestId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            try {
                val (year, term) = parseYearTerm(yearValue)
                
                val classes = repository.getScheduleClasses(year, term)
                if (requestId != scheduleRequestId) return@launch
                val selected = classes.firstOrNull()?.value
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        availableClasses = classes,
                        selectedClassValue = selected
                    )
                }
            } catch (e: Exception) {
                e.throwIfCancellation()
                if (requestId != scheduleRequestId) return@launch
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = e.message ?: "載入班級清單失敗"
                    )
                }
            }
        }
    }

    private fun loadSchedule(yearValue: String, classNo: String) {
        val requestId = ++scheduleRequestId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            try {
                val (year, term) = parseYearTerm(yearValue)
                
                val report = repository.fetchSchedule(yearValue, year, term, classNo)
                if (requestId != scheduleRequestId) return@launch
                analyticsLogger.logEvent(
                    AnalyticsEvents.SCHEDULE_QUERY,
                    mapOf(
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                        AnalyticsParams.MODE to AnalyticsValues.MODE_CLASS,
                    ),
                )
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        report = report
                    )
                }
            } catch (e: Exception) {
                e.throwIfCancellation()
                if (requestId != scheduleRequestId) return@launch
                analyticsLogger.logEvent(
                    AnalyticsEvents.SCHEDULE_QUERY,
                    mapOf(
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                        AnalyticsParams.MODE to AnalyticsValues.MODE_CLASS,
                    ),
                )
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMessage = e.message ?: "載入課表失敗"
                    )
                }
            }
        }
    }

    fun refresh() {
        val st = _uiState.value
        val y = st.selectedYearValue
        val c = st.selectedClassValue
        if (st.report != null && y != null && c != null) {
            loadSchedule(y, c)
        } else if (y != null) {
            loadClasses(y)
        } else {
            loadYears()
        }
    }

    private fun Throwable.throwIfCancellation() {
        if (this is CancellationException) throw this
    }

    companion object {
        fun factory(
            context: Context,
            useFakeData: Boolean,
            activeSessionProvider: () -> AuthenticatedSession? = { null },
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val repository = if (useFakeData) {
                    FakeScheduleRepository()
                } else {
                    NetworkScheduleRepository(
                        SchoolGradeClient(),
                        SessionStore(appContext),
                        GradeCacheStore(appContext),
                        activeSessionProvider = activeSessionProvider,
                    )
                }
                return ScheduleViewModel(repository, FirebaseAnalyticsLogger(appContext)) as T
            }
        }
    }
}
