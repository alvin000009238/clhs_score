package com.clhs.score.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.clhs.score.data.ExamOption
import com.clhs.score.data.ExamSummary
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeStandard
import com.clhs.score.data.LocalScoreInsightProvider
import com.clhs.score.data.MockGradeSystem
import com.clhs.score.data.StudentInfo
import com.clhs.score.data.StudentScenario
import com.clhs.score.data.SubjectScore
import com.clhs.score.data.YearTermOption
import com.clhs.score.data.buildGradeAnalysis
import com.clhs.score.data.buildGradeTrend
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.ui.theme.ScoreTheme
import com.clhs.score.viewmodel.GradesUiState
import kotlinx.serialization.json.JsonObject
import org.junit.Rule
import org.junit.Test

class ScoreUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun introScreenExposesLoginEntryPoint() {
        var clicked = false
        composeRule.setContent {
            ScoreTheme {
                IntroScreen(onLoginClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("成績", substring = true).assertIsDisplayed()
        composeRule.onNode(hasClickAction()).performClick()
        composeRule.runOnIdle {
            assert(clicked)
        }
    }

    @Test
    fun gradesScreenUsesBottomNavigationAndSegmentedOverview() {
        composeRule.setContent {
            ScoreTheme {
                TestGradesScreen()
            }
        }

        composeRule.onNodeWithText("總覽").assertIsDisplayed()
        composeRule.onNodeWithText("科目").assertIsDisplayed()
        composeRule.onNodeWithText("進階").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("總覽").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("科目").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("進階").assertIsDisplayed()
        composeRule.onAllNodesWithText("全部科目").assertCountEquals(0)
        composeRule.onAllNodesWithText("圖表").assertCountEquals(0)
        composeRule.onNodeWithText("展示學生", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("加權平均").assertIsDisplayed()
        composeRule.onNodeWithText("班排 15/38（前 39%）").assertIsDisplayed()
        composeRule.onNodeWithText("類排 88/226（前 38%）").assertIsDisplayed()
        composeRule.onNodeWithText("科目數 7 ｜ 最高分 80").assertIsDisplayed()
        composeRule.onNodeWithText("重點解讀").assertIsDisplayed()
        composeRule.onNodeWithText("最值得補強").assertIsDisplayed()
        composeRule.onNodeWithText("最具優勢").assertIsDisplayed()
        composeRule.onNodeWithText("排名推估").assertIsDisplayed()
        composeRule.onNodeWithText("查看科目分析").assertIsDisplayed()
        composeRule.onNodeWithText("查看圖表分析").assertIsDisplayed()
        composeRule.onNodeWithText("查看排名分布").assertIsDisplayed()
        composeRule.onNodeWithText("強弱科摘要").assertIsDisplayed()
        composeRule.onAllNodesWithText("科目分析").assertCountEquals(0)
        composeRule.onAllNodesWithText("圖表分析").assertCountEquals(0)
        composeRule.onAllNodesWithText("進階資料").assertCountEquals(0)
    }

    @Test
    fun subjectCardExpandsDetails() {
        composeRule.setContent {
            ScoreTheme {
                TestGradesScreen()
            }
        }

        composeRule.onNodeWithText("科目").performClick()
        composeRule.onAllNodesWithText("五標落點").assertCountEquals(0)
        composeRule.onNodeWithText("國語文").performScrollTo().performClick()
        composeRule.onNodeWithText("五標落點").assertIsDisplayed()
        composeRule.onNodeWithText("分佈摘要").assertIsDisplayed()
        composeRule.onNodeWithText("上一考比較").assertIsDisplayed()
        composeRule.onAllNodesWithText("班級平均").assertCountEquals(0)
        composeRule.onAllNodesWithText("校排").assertCountEquals(0)

        composeRule.onNodeWithText("國語文").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("五標落點").assertCountEquals(0)
    }

    @Test
    fun quickActionsNavigateToSubjectsAndAdvanced() {
        composeRule.setContent {
            ScoreTheme {
                TestGradesScreen()
            }
        }

        composeRule.onNodeWithText("查看科目分析").performScrollTo().performClick()
        composeRule.onNodeWithText("國語文").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("總覽").performClick()
        composeRule.onNodeWithText("查看圖表分析").performScrollTo().performClick()
        composeRule.onNodeWithText("五標分析").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun overviewShowsTrendLoadingAndNoHistoryStates() {
        composeRule.setContent {
            ScoreTheme {
                TestGradesScreen(isLoadingTrend = true, trendError = null)
            }
        }
        composeRule.onNodeWithText("正在載入歷次趨勢...").assertIsDisplayed()

        composeRule.setContent {
            ScoreTheme {
                TestGradesScreen(showTrend = false, trendError = "尚無歷次趨勢可比較")
            }
        }
        composeRule.onNodeWithText("尚無歷次趨勢可比較").assertIsDisplayed()
    }

    @Test
    fun analysisSectionShowsDistributionCollapsedThenExpanded() {
        composeRule.setContent {
            ScoreTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val report = MockGradeSystem.generateReport()
                    AnalysisSection(report = report, analysis = buildGradeAnalysis(report))
                }
            }
        }

        composeRule.onNodeWithText("雷達分析").assertIsDisplayed()
        composeRule.onNodeWithText("成績比較").assertIsDisplayed()
        composeRule.onNodeWithText("五標分析").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("分數分布").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("展開完整分佈").performScrollTo().performClick()
        composeRule.onNodeWithText("收合完整分佈").assertIsDisplayed()
    }

    @Composable
    private fun TestGradesScreen(
        isLoadingTrend: Boolean = false,
        showTrend: Boolean = true,
        trendError: String? = null,
    ) {
        val report = MockGradeSystem.generateReport(StudentScenario.NORMAL)
        val analysis = buildGradeAnalysis(report)
        val trend = if (showTrend) buildGradeTrend(
            currentExamName = "期末考",
            currentReport = report,
            previousReports = listOf("期中考" to MockGradeSystem.generateReport(
                StudentScenario.NORMAL,
                customScores = listOf(78.0, 80.0, 61.0, 70.0, 60.0, 60.0, 60.0)
            )),
        ) else null
        var expanded by remember { mutableStateOf(emptySet<String>()) }
        GradesScreen(
            state = GradesUiState(
                isLoggedIn = true,
                studentNo = "110000",
                structure = listOf(
                    YearTermOption(
                        text = "114學年度 上學期",
                        value = "114_1",
                        exams = listOf(ExamOption("期中考", "E1"), ExamOption("期末考", "E2")),
                    ),
                ),
                selectedYearValue = "114_1",
                selectedExamValue = "E2",
                report = report,
                analysis = analysis,
                isLoadingTrend = isLoadingTrend,
                trendError = trendError,
                trend = trend,
                insights = LocalScoreInsightProvider().buildInsights(report, analysis, trend),
                expandedSubjectKeys = expanded,
            ),
            snackbarHost = {},
            onSelectYear = {},
            onSelectExam = {},
            onReload = {},
            onLogout = {},
            onToggleSubject = { subjectName ->
                val key = cleanSubjectName(subjectName)
                expanded = if (key in expanded) expanded - key else expanded + key
            },
            onOpenScoreSimulator = {},
        )
    }
}
