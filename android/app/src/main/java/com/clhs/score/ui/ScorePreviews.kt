package com.clhs.score.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.clhs.score.data.FakeData
import com.clhs.score.data.LocalScoreInsightProvider
import com.clhs.score.data.MockGradeSystem
import com.clhs.score.data.StudentScenario
import com.clhs.score.data.buildGradeAnalysis
import com.clhs.score.data.buildGradeTrend
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.AppSettings
import com.clhs.score.ui.theme.ScoreTheme
import com.clhs.score.viewmodel.GradesUiState
import com.clhs.score.viewmodel.LoginUiState
import com.clhs.score.viewmodel.SettingsUiState

class ScenarioProvider : PreviewParameterProvider<StudentScenario> {
    override val values = sequenceOf(
        StudentScenario.NORMAL,
        StudentScenario.EXCELLENT,
        StudentScenario.STRUGGLING,
        StudentScenario.SPECIAL
    )
}

@Preview(name = "Score App - Fake Data", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScoreAppFakePreview(
    @PreviewParameter(ScenarioProvider::class) scenario: StudentScenario
) {
    ScoreTheme {
        ScoreApp(
            loginState = LoginUiState(),
            gradesState = fakeGradesState(scenario = scenario),
            settings = AppSettings(),
            settingsUiState = SettingsUiState(),
            onWebViewLoginSuccess = { _, _ -> },
            onSelectYear = {},
            onSelectExam = {},
            onReload = {},
            onLogout = {},
            onToggleSubject = {},
            onDismissLoginError = {},
            onDismissGradesError = {},
            onSetThemeMode = {},
            onSetDynamicColor = {},
            onSetAmoledBlack = {},
            onCheckUpdate = {},
            onDismissUpdateResult = {},
            onVersionTap = {},
            onDismissDeveloperToast = {},
            onSetDemoMode = {},
            onDismissRestartDialog = {},
        )
    }
}

@Preview(name = "Grades Screen - Fake Data", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GradesScreenFakePreview(
    @PreviewParameter(ScenarioProvider::class) scenario: StudentScenario
) {
    val state = fakeGradesState(scenario = scenario)
    ScoreTheme {
        GradesScreen(
            state = state.copy(
                expandedSubjectKeys = state.report?.subjects
                    ?.take(1)
                    ?.map { cleanSubjectName(it.subjectName) }
                    ?.toSet() ?: emptySet()
            ),
            snackbarHost = {},
            onSelectYear = {},
            onSelectExam = {},
            onReload = {},
            onOpenSettings = {},
            onToggleSubject = {},
            onOpenScoreSimulator = {},
        )
    }
}

@Preview(name = "Score Simulator - Fake Data", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScoreSimulatorFakePreview(
    @PreviewParameter(ScenarioProvider::class) scenario: StudentScenario
) {
    ScoreTheme {
        ScoreSimulatorScreen(
            state = fakeGradesState(scenario = scenario),
            snackbarHost = {},
            onBack = {},
        )
    }
}

@Preview(name = "Subject Card - Fake Data", showBackground = true, widthDp = 390)
@Composable
private fun SubjectCardFakePreview(
    @PreviewParameter(ScenarioProvider::class) scenario: StudentScenario
) {
    val report = MockGradeSystem.generateReport(scenario)
    val analysis = buildGradeAnalysis(report)
    val subjectAnalysis = analysis.subjects.firstOrNull() ?: return
    
    ScoreTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SubjectCard(
                analysis = subjectAnalysis,
                expanded = true,
                onToggle = {}
            )
        }
    }
}

private fun fakeGradesState(
    scenario: StudentScenario = StudentScenario.NORMAL,
    expandedSubjectKeys: Set<String> = emptySet(),
): GradesUiState {
    val report = MockGradeSystem.generateReport(scenario)
    val previous = FakeData.previousReport()
    val trend = buildGradeTrend(
        currentExamName = report.examSummary?.examName.orEmpty().ifBlank { "期末考" },
        currentReport = report,
        previousReports = FakeData.trendReports(),
    )
    val analysis = buildGradeAnalysis(
        report = report,
        comparisonReport = previous,
        previousExamName = previous.examSummary?.examName,
    )
    return GradesUiState(
        isLoggedIn = true,
        studentNo = FakeData.session.studentNo,
        structure = FakeData.structure,
        selectedYearValue = FakeData.currentYearValue,
        selectedExamValue = FakeData.currentExamValue,
        report = report,
        comparisonReport = previous,
        comparisonExamName = previous.examSummary?.examName,
        trendReports = FakeData.trendReports().map { it.second },
        trendHistoryLabel = "近 3 次段考",
        trend = trend,
        simulatorHistoryReports = FakeData.simulatorHistoryReports(),
        simulatorHistoryLabel = "近 3 次段考",
        insights = LocalScoreInsightProvider().buildInsights(report, analysis, trend),
        analysis = analysis,
        expandedSubjectKeys = expandedSubjectKeys,
    )
}
