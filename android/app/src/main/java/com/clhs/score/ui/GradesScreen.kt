package com.clhs.score.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clhs.score.data.ExamSummary
import com.clhs.score.data.GradeAnalysis
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeTrend
import com.clhs.score.data.ScoreInsightSet
import com.clhs.score.data.StudentInfo
import com.clhs.score.data.SubjectAnalysis
import com.clhs.score.data.SubjectScore
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.shortenSubjectName
import com.clhs.score.viewmodel.GradesUiState

import com.clhs.score.ui.theme.ScoreTheme

private const val TAB_SLIDE_DURATION_MILLIS = 220

private enum class GradesDestination(
    val label: String,
    val icon: String,
) {
    Overview("總覽", "home"),
    Subjects("科目", "newsstand"),
    Advanced("更多", "more_horiz"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    state: GradesUiState,
    snackbarHost: @Composable () -> Unit,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onOpenScoreSimulator: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenSubjectTrend: () -> Unit,
) {
    var selectedDestination by rememberSaveable { mutableIntStateOf(GradesDestination.Overview.ordinal) }
    val pagerState = rememberPagerState(
        initialPage = GradesDestination.Overview.ordinal,
        pageCount = { GradesDestination.entries.size },
    )
    val overviewScrollState = rememberScrollState()
    val subjectsScrollState = rememberScrollState()
    val advancedScrollState = rememberScrollState()

    LaunchedEffect(selectedDestination) {
        if (pagerState.currentPage != selectedDestination || pagerState.targetPage != selectedDestination) {
            pagerState.animateScrollToPage(
                page = selectedDestination,
                animationSpec = tween(durationMillis = TAB_SLIDE_DURATION_MILLIS),
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "壢中成績",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = headerStudentText(state),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = !state.isLoadingStructure && !state.isLoadingGrades,
                            onClick = onReload,
                        ) {
                            OutlinedRoundedSymbol(
                                icon = "refresh",
                                tint = if (!state.isLoadingStructure && !state.isLoadingGrades) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                },
                                contentDescription = "重新整理",
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            OutlinedRoundedSymbol(
                                icon = "settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                contentDescription = "設定",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    scrollBehavior = scrollBehavior,
                )
                GradeSelectors(
                    state = state,
                    onSelectYear = onSelectYear,
                    onSelectExam = onSelectExam,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        snackbarHost = snackbarHost,
        bottomBar = {
            GradesBottomNavigation(
                selectedDestination = selectedDestination,
                onSelect = { selectedDestination = it.ordinal },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        val isRefreshing = state.isLoadingStructure || state.isLoadingGrades

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onReload,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.isLoadingStructure || state.isLoadingGrades || state.isLoadingComparison || state.isLoadingTrend) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val report = state.report
                    val analysis = state.analysis
                    when {
                        report == null && (state.isLoadingStructure || state.isLoadingGrades) -> GradesTabPage(
                            scrollState = overviewScrollState,
                        ) {
                            OverviewSkeleton()
                        }
                        report == null -> GradesTabPage(
                            scrollState = overviewScrollState,
                        ) {
                            EmptyPanel(
                                message = if (state.structure.isEmpty()) "尚未取得可查詢考試" else "請選擇考試",
                                onReload = onReload,
                            )
                        }
                        analysis == null -> GradesTabPage(
                            scrollState = overviewScrollState,
                        ) {
                            OverviewSkeleton()
                        }
                        else -> HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = false,
                        ) { destination ->
                            val tabScrollState = when (destination) {
                                GradesDestination.Overview.ordinal -> overviewScrollState
                                GradesDestination.Subjects.ordinal -> subjectsScrollState
                                else -> advancedScrollState
                            }
                            GradesTabPage(scrollState = tabScrollState) {
                                when (destination) {
                                    GradesDestination.Overview.ordinal -> OverviewTab(
                                        report = report,
                                        analysis = analysis,
                                        isLoadingComparison = state.isLoadingComparison,
                                        comparisonError = state.comparisonError,
                                        isLoadingTrend = state.isLoadingTrend,
                                        trendError = state.trendError,
                                        trend = state.trend,
                                        insights = state.insights,
                                    )
                                    GradesDestination.Subjects.ordinal -> SubjectsTab(
                                        analyses = analysis.subjects,
                                        expandedSubjectKeys = state.expandedSubjectKeys,
                                        onToggleSubject = onToggleSubject,
                                    )
                                    GradesDestination.Advanced.ordinal -> AdvancedTab(
                                        report = report,
                                        analysis = analysis,
                                        isLoadingTrend = state.isLoadingTrend,
                                        trendError = state.trendError,
                                        isLoadingSimulatorHistory = state.isLoadingSimulatorHistory,
                                        simulatorHistoryLabel = state.simulatorHistoryLabel,
                                        simulatorHistoryCount = state.simulatorHistoryReports.size,
                                        trend = state.trend,
                                        onOpenScoreSimulator = onOpenScoreSimulator,
                                        onOpenSchedule = onOpenSchedule,
                                        onOpenSubjectTrend = onOpenSubjectTrend,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradesTabPage(
    scrollState: ScrollState,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

private fun headerStudentText(state: GradesUiState): String {
    val student = state.report?.studentInfo
    if (student != null) {
        return listOf(
            student.studentName.takeIf { it.isNotBlank() },
            "${student.className.ifBlank { "--" }} ${student.seatNo.ifBlank { "--" }}",
            "${student.studentNo.ifBlank { state.studentNo.ifBlank { "--" } }}",
        ).filterNotNull().joinToString("｜")
    }
    return state.studentNo.takeIf { it.isNotBlank() }?.let { "學號 $it" } ?: "登入後顯示學生資訊"
}

@Composable
private fun GradesBottomNavigation(
    selectedDestination: Int,
    onSelect: (GradesDestination) -> Unit,
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        GradesDestination.entries.forEach { destination ->
            val selected = selectedDestination == destination.ordinal
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    if (selected) {
                        FilledRoundedSymbol(
                            icon = destination.icon,
                            contentDescription = destination.label,
                        )
                    } else {
                        OutlinedRoundedSymbol(
                            icon = destination.icon,
                            contentDescription = destination.label,
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun GradeSelectors(
    state: GradesUiState,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
) {
    val selectedYear = state.structure.firstOrNull { it.value == state.selectedYearValue }
    val exams = selectedYear?.exams.orEmpty()
    val selectedExam = exams.firstOrNull { it.value == state.selectedExamValue }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OptionDropdown(
            modifier = Modifier.weight(1f),
            label = selectedYear?.text ?: "學年學期",
            enabled = state.structure.isNotEmpty() && !state.isLoadingStructure,
            options = state.structure,
            optionLabel = { it.text },
            onSelect = { onSelectYear(it.value) },
        )
        OptionDropdown(
            modifier = Modifier.weight(1f),
            label = selectedExam?.text ?: "考試",
            enabled = exams.isNotEmpty() && !state.isLoadingGrades,
            options = exams,
            optionLabel = { it.text },
            onSelect = { onSelectExam(it.value) },
        )
    }
}

@Composable
private fun <T> OptionDropdown(
    modifier: Modifier,
    label: String,
    enabled: Boolean,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            onClick = { expanded = true },
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(
    report: GradeReport,
    analysis: GradeAnalysis,
    isLoadingComparison: Boolean,
    comparisonError: String?,
    isLoadingTrend: Boolean,
    trendError: String?,
    trend: GradeTrend?,
    insights: ScoreInsightSet?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroCard(report, analysis)
            HeroChipRow(report, analysis)
        }
        StrengthWeaknessCard(analysis)
        InsightCard(
            analysis = analysis,
            isLoadingComparison = isLoadingComparison,
            comparisonError = comparisonError,
            isLoadingTrend = isLoadingTrend,
            trendError = trendError,
            trend = trend,
            insights = insights,
        )
    }
}

@Composable
private fun AdvancedTab(
    report: GradeReport,
    analysis: GradeAnalysis,
    isLoadingTrend: Boolean,
    trendError: String?,
    isLoadingSimulatorHistory: Boolean,
    simulatorHistoryLabel: String?,
    simulatorHistoryCount: Int,
    trend: GradeTrend?,
    onOpenScoreSimulator: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenSubjectTrend: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "更多功能",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        TrendChart(
            isLoadingTrend = isLoadingTrend,
            trendError = trendError,
            trend = trend,
        )
        ScoreSimulatorEntryCard(
            report = report,
            analysis = analysis,
            isLoadingHistory = isLoadingSimulatorHistory,
            historyLabel = simulatorHistoryLabel,
            historyCount = simulatorHistoryCount,
            onOpen = onOpenScoreSimulator,
        )
        SubjectTrendEntryCard(
            onOpen = onOpenSubjectTrend,
        )
        ScheduleEntryCard(
            onOpen = onOpenSchedule,
        )
    }
}

@Composable
private fun HeroCard(report: GradeReport, analysis: GradeAnalysis) {
    val student = report.studentInfo
    val summary = report.examSummary
    val animatedAverage by animateFloatAsState(
        targetValue = analysis.weightedAverage.toFloat(),
        label = "weightedAverage",
    )
    val totalScore = summary?.totalScoreDisplay?.toDoubleOrNull()?.let { "%.0f".format(it) }
        ?: summary?.totalScoreDisplay?.takeIf { it.isNotBlank() }
        ?: "--"
    val rankLine = heroRankLine(summary, student)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = "加權平均",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "%.1f".format(animatedAverage),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                    )
                    val deltaText = heroAverageDeltaTextShort(analysis)
                    if (deltaText != null) {
                        val deltaColor = diffColor(analysis.comparison?.averageDelta ?: 0.0)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = deltaColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            Text(
                                text = deltaText,
                                color = deltaColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Text(
                text = "總分 $totalScore",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = rankLine,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun HeroChip(
    text: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HeroChipRow(report: GradeReport, analysis: GradeAnalysis) {
    val summary = report.examSummary
    val examName = summary?.examName?.takeIf { it.isNotBlank() } ?: "本次考試"

    val classPercent = analysis.classPercentile?.topPercent
    val categoryPercent = analysis.categoryPercentile?.topPercent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 考試名稱
        HeroChip(
            text = examName,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 2. 班級百分位
        if (classPercent != null) {
            HeroChip(
                text = "班級前 $classPercent%",
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 3. 類組百分位
        if (categoryPercent != null) {
            HeroChip(
                text = "類組前 $categoryPercent%",
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun heroRankLine(summary: ExamSummary?, student: StudentInfo): String {
    val classRank = formatRank(summary?.classRank, summary?.classCount, student.showClassRankCount)
    val categoryRank = formatRank(summary?.categoryRank, summary?.categoryRankCount, student.showCategoryRankCount)
    return "班排 ${if (student.showClassRank) classRank else "--"} ・ 類排 ${if (student.showCategoryRank) categoryRank else "--"}"
}

private fun heroAverageDeltaTextShort(analysis: GradeAnalysis): String? {
    val delta = analysis.comparison?.averageDelta ?: return null
    return when {
        delta > 0.05 -> "↑ +${"%.1f".format(delta)}"
        delta < -0.05 -> "↓ ${"%.1f".format(delta)}"
        else -> "→ 持平"
    }
}

@Composable
private fun InsightCard(
    analysis: GradeAnalysis,
    isLoadingComparison: Boolean,
    comparisonError: String?,
    isLoadingTrend: Boolean,
    trendError: String?,
    trend: GradeTrend?,
    insights: ScoreInsightSet?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("分析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            DashboardInsightRow(
                label = "需要注意",
                text = riskInsightText(analysis, insights),
                color = ScoreTheme.semanticColors.negative,
            )
            DashboardInsightRow(
                label = "優勢",
                text = advantageInsightText(analysis),
                color = ScoreTheme.semanticColors.positive,
            )
            DashboardInsightRow(
                label = "ROI",
                text = roiInsightText(analysis, insights),
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                isLoadingComparison -> InlineStatus("正在載入上一考比較...")
                analysis.comparison != null -> InlineStatus("${analysis.comparison.previousExamName}：${analysis.comparison.summaryText}")
                comparisonError != null -> InlineStatus(comparisonError)
            }
            when {
                isLoadingTrend -> InlineStatus("正在載入歷次趨勢...")
                trend != null && trend.points.size >= 2 -> InlineStatus("近 ${trend.points.size} 次平均：${trend.averageLine}")
                trendError != null -> InlineStatus(trendError)
            }
        }
    }
}

@Composable
private fun DashboardInsightRow(label: String, text: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun riskInsightText(analysis: GradeAnalysis, insights: ScoreInsightSet?): String {
    insights?.projection?.let { projection ->
        val subject = shortenSubjectName(projection.subjectName)
        return "$subject 目前為主要拉低科目，若提升至班均，加權平均約可提升 ${"%.1f".format(projection.weightedAverageGain)}。"
    }
    val weakness = analysis.weaknesses.firstOrNull()
    return weakness?.let { "${shortenSubjectName(it.subjectName)} 低於班平均 ${"%.1f".format(kotlin.math.abs(it.diffValue))} 分，建議優先處理。" }
        ?: "目前沒有明顯拉低科目，需要注意的科目集中度低。"
}

private fun advantageInsightText(analysis: GradeAnalysis): String {
    val strength = analysis.strengths.firstOrNull()
    return strength?.let {
        "${shortenSubjectName(it.subjectName)} ${subjectPercentLabel(it.classRank, it.classRankCount)}，高於班平均 ${"%.1f".format(it.diffValue)} 分，建議維持目前節奏。"
    } ?: "尚無明顯優勢科目，先把各科穩定在班平均附近。"
}

private fun roiInsightText(analysis: GradeAnalysis, insights: ScoreInsightSet?): String {
    val projection = insights?.projection
    return projection?.let {
        "目前投入效益最高科目為 ${shortenSubjectName(it.subjectName)}，每次小幅提升會直接推動加權平均。"
    } ?: analysis.weaknesses.firstOrNull()?.let {
        "目前投入效益最高科目為 ${shortenSubjectName(it.subjectName)}。"
    } ?: "目前各科差距接近，ROI 最高的方向是維持弱科不下滑。"
}

@Composable
private fun StrengthWeaknessCard(analysis: GradeAnalysis) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("摘要", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        SubjectHighlightRow(title = "優勢科目", subjects = analysis.strengths, color = ScoreTheme.semanticColors.positive, emptyText = "尚無明顯高於平均的科目")
        SubjectHighlightRow(title = "待加強科目", subjects = analysis.weaknesses, color = ScoreTheme.semanticColors.negative, emptyText = "尚無明顯低於平均的科目")
    }
}

@Composable
private fun SubjectHighlightRow(title: String, subjects: List<SubjectScore>, color: Color, emptyText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (subjects.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                subjects.forEach { subject ->
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .background(color.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = shortenSubjectName(subject.subjectName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = diffSentence(subject.diffValue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = subjectPercentLabel(subject.classRank, subject.classRankCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectsTab(
    analyses: List<SubjectAnalysis>,
    expandedSubjectKeys: Set<String>,
    onToggleSubject: (String) -> Unit,
) {
    var pendingBringIntoViewKey by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        analyses.forEach { analysis ->
            val subjectKey = cleanSubjectName(analysis.subject.subjectName)
            val expanded = subjectKey in expandedSubjectKeys
            SubjectCard(
                analysis = analysis,
                expanded = expanded,
                bringIntoViewOnExpand = pendingBringIntoViewKey == subjectKey,
                onBringIntoViewHandled = {
                    if (pendingBringIntoViewKey == subjectKey) {
                        pendingBringIntoViewKey = null
                    }
                },
                onToggle = {
                    pendingBringIntoViewKey = if (expanded) null else subjectKey
                    onToggleSubject(analysis.subject.subjectName)
                },
            )
        }
    }
}

@Composable
private fun InlineStatus(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OverviewSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) {
            SkeletonBlock(height = if (it == 1) 190.dp else 96.dp)
        }
    }
}

@Composable
private fun SkeletonBlock(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f), MaterialTheme.shapes.medium),
    )
}

@Composable
private fun EmptyPanel(message: String, onReload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(shape = RoundedCornerShape(14.dp), onClick = onReload) {
            Text("重新整理")
        }
    }
}

private fun signedValue(value: Double): String = "${if (value >= 0.0) "+" else ""}${"%.1f".format(value)}"

private fun Double?.formatCompactScore(): String = this?.let { "%.0f".format(it) } ?: "--"

private fun diffSentence(diff: Double): String = when {
    diff > 0.05 -> "高於平均 ${signedValue(diff)}"
    diff < -0.05 -> "低於平均 ${signedValue(diff)}"
    else -> "接近班級平均"
}

private fun trendGlyph(diff: Double?): String = when {
    diff == null -> "→"
    diff > 0.05 -> "↑"
    diff < -0.05 -> "↓"
    else -> "→"
}

@Composable
private fun diffColor(diff: Double): Color = when {
    diff > 0.05 -> ScoreTheme.semanticColors.positive
    diff < -0.05 -> ScoreTheme.semanticColors.negative
    else -> ScoreTheme.semanticColors.neutral
}
