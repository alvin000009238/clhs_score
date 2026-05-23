package com.clhs.score.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clhs.score.data.GradeAnalysis
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeTrend
import com.clhs.score.data.SubjectScore
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.shortenSubjectName
import com.clhs.score.data.subjectWeight
import com.clhs.score.data.weightedAverageFor
import com.clhs.score.viewmodel.GradesUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun TrendChart(
    isLoadingTrend: Boolean,
    trendError: String?,
    trend: GradeTrend?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("歷次考試趨勢", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            when {
                isLoadingTrend -> ChartLoadingPlaceholder()
                trend != null && trend.points.size >= 2 -> {
                    trend.points.forEachIndexed { index, point ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (index == trend.points.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(999.dp),
                                    ),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(point.examName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "平均 ${"%.1f".format(point.weightedAverage)} ｜ 班排 ${point.classRank ?: "--"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            point.highestScore?.let {
                                Text(
                                    text = "最高 ${"%.0f".format(it)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                else -> EmptyAnalysisState(trendError ?: "目前沒有足夠歷次考試資料。")
            }
        }
    }
}

@Composable
internal fun ScoreSimulatorEntryCard(
    report: GradeReport,
    analysis: GradeAnalysis,
    isLoadingHistory: Boolean = false,
    historyLabel: String? = null,
    historyCount: Int = 0,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("成績模擬器", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "自訂採計科目與目標分數，快速試算調整後的平均成績。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(modifier = Modifier.weight(1f), label = "目前平均", value = "%.2f".format(analysis.weightedAverage))
                InfoChip(modifier = Modifier.weight(1f), label = "科目", value = "${report.subjects.size} 科")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScoreSimulatorScreen(
    state: GradesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val report = state.report
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成績模擬器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        OutlinedRoundedSymbol(
                            icon = "arrow_back",
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        if (report == null) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                EmptyAnalysisState("尚未載入成績資料。")
            }
            return@Scaffold
        }

        val coroutineScope = rememberCoroutineScope()

        val initialScores = remember(report) {
            report.subjects.associate { cleanSubjectName(it.subjectName) to it.scoreValue }
        }
        var adjustedScores by remember(report) { mutableStateOf(initialScores) }
        var isCustomGroupEnabled by remember { mutableStateOf(false) }
        var isTargetReversalEnabled by remember { mutableStateOf(false) }
        var checkedSubjects by remember(report) { mutableStateOf(report.subjects.map { cleanSubjectName(it.subjectName) }.toSet()) }
        var lockedSubjects by remember { mutableStateOf(emptySet<String>()) }
        var targetAverageStr by remember { mutableStateOf("") }

        val activeSubjects = if (isCustomGroupEnabled) checkedSubjects else report.subjects.map { cleanSubjectName(it.subjectName) }.toSet()

        val currentAverage = remember(report, activeSubjects) {
            weightedAverageFor(report.subjects, emptyMap(), activeSubjects)
        }
        val adjustedAverage = remember(report, adjustedScores, activeSubjects) {
            weightedAverageFor(report.subjects, adjustedScores, activeSubjects)
        }

        val allHistory = remember(state.trendReports, state.simulatorHistoryReports) {
            state.trendReports + state.simulatorHistoryReports
        }
        val historyMaxMin = remember(allHistory, report) {
            val map = mutableMapOf<String, Pair<Double, Double>>()
            report.subjects.forEach { subject ->
                val key = cleanSubjectName(subject.subjectName)
                val scores = allHistory.mapNotNull { r -> r.subjects.find { cleanSubjectName(it.subjectName) == key }?.scoreValue }
                if (scores.isNotEmpty()) {
                    map[key] = scores.min() to scores.max()
                }
            }
            map
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 110.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("自訂採計組合", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = isCustomGroupEnabled, onCheckedChange = {
                                isCustomGroupEnabled = it
                                if (it) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("已開啟自訂組合，請在下方取消勾選不計分的科目")
                                    }
                                }
                            })
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("目標反推模式", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = isTargetReversalEnabled, onCheckedChange = {
                                isTargetReversalEnabled = it
                                if (!it) lockedSubjects = emptySet()
                                else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("已開啟目標反推模式，請鎖定科目分數後按計算")
                                    }
                                }
                            })
                        }

                        if (isTargetReversalEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp),
                            ) {
                                OutlinedTextField(
                                    value = targetAverageStr,
                                    onValueChange = { targetAverageStr = it },
                                    label = { Text("目標平均") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                Button(
                                    onClick = {
                                        val targetAvg = targetAverageStr.toDoubleOrNull() ?: return@Button
                                        val activeList = report.subjects.filter { cleanSubjectName(it.subjectName) in activeSubjects }
                                        val totalWeights = activeList.sumOf { subjectWeight(it.subjectName).toDouble() }
                                        val targetTotalPoints = targetAvg * totalWeights

                                        val lockedList = activeList.filter { cleanSubjectName(it.subjectName) in lockedSubjects }
                                        val lockedPoints = lockedList.sumOf { (adjustedScores[cleanSubjectName(it.subjectName)] ?: it.scoreValue) * subjectWeight(it.subjectName) }

                                        val unlockedList = activeList.filter { cleanSubjectName(it.subjectName) !in lockedSubjects }
                                        val unlockedWeights = unlockedList.sumOf { subjectWeight(it.subjectName).toDouble() }

                                        if (unlockedWeights > 0) {
                                            val neededAvg = (targetTotalPoints - lockedPoints) / unlockedWeights
                                            val newScores = adjustedScores.toMutableMap()
                                            unlockedList.forEach { subject ->
                                                newScores[cleanSubjectName(subject.subjectName)] = neededAvg.coerceIn(0.0, 100.0)
                                            }
                                            adjustedScores = newScores
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("反推計算完成")
                                            }
                                        }
                                    },
                                ) {
                                    Text("開始計算")
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "科目分數",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TextButton(onClick = {
                                adjustedScores = initialScores
                                checkedSubjects = report.subjects.map { cleanSubjectName(it.subjectName) }.toSet()
                            }) {
                                Text("恢復全部")
                            }
                        }
                        report.subjects.forEachIndexed { index, subject ->
                            val key = cleanSubjectName(subject.subjectName)
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            SubjectScoreSlider(
                                subject = subject,
                                value = adjustedScores[key] ?: subject.scoreValue,
                                isChecked = checkedSubjects.contains(key),
                                onCheckedChange = { checked ->
                                    checkedSubjects = if (checked) checkedSubjects + key else checkedSubjects - key
                                },
                                showCheckbox = isCustomGroupEnabled,
                                isLocked = lockedSubjects.contains(key),
                                onLockedChange = { locked ->
                                    lockedSubjects = if (locked) lockedSubjects + key else lockedSubjects - key
                                },
                                showLock = isTargetReversalEnabled,
                                historyMin = historyMaxMin[key]?.first,
                                historyMax = historyMaxMin[key]?.second,
                                onValueChange = { score ->
                                    adjustedScores = adjustedScores + (key to score)
                                },
                                onResetSubject = {
                                    adjustedScores = adjustedScores + (key to subject.scoreValue)
                                },
                            )
                        }
                    }
                }
            }

            SimulatorSummaryCard(
                currentAverage = currentAverage,
                adjustedAverage = adjustedAverage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SimulatorSummaryCard(
    currentAverage: Double,
    adjustedAverage: Double,
    modifier: Modifier = Modifier,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val labelColor = contentColor.copy(alpha = 0.78f)
    val changedValueColor = MaterialTheme.colorScheme.tertiary
    val arrowContainerColor = contentColor.copy(alpha = 0.12f)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("原始平均", style = MaterialTheme.typography.bodyMedium, color = labelColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%.2f".format(currentAverage),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(arrowContainerColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedRoundedSymbol(
                    icon = "arrow_forward",
                    tint = contentColor,
                    contentDescription = null,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("調整後平均", style = MaterialTheme.typography.bodyMedium, color = labelColor)
                Spacer(modifier = Modifier.height(8.dp))
                val isAdjusted = kotlin.math.abs(adjustedAverage - currentAverage) > 0.01
                val color = if (isAdjusted) changedValueColor else contentColor
                Text(
                    text = "%.2f".format(adjustedAverage),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun SubjectScoreSlider(
    subject: SubjectScore,
    value: Double,
    isChecked: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
    showCheckbox: Boolean = false,
    isLocked: Boolean = false,
    onLockedChange: (Boolean) -> Unit = {},
    showLock: Boolean = false,
    historyMin: Double? = null,
    historyMax: Double? = null,
    onValueChange: (Double) -> Unit,
    onResetSubject: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val diff = value - subject.scoreValue
    val isAboveMax = historyMax != null && value > historyMax
    var lastDispatchedScore by remember(subject.subjectName, value.roundToInt()) {
        mutableStateOf(value.roundToInt())
    }
    fun updateScore(score: Double, haptic: Boolean = false) {
        val roundedScore = score.coerceIn(0.0, 100.0).roundToInt()
        if (roundedScore == lastDispatchedScore) {
            return
        }
        if (haptic) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastDispatchedScore = roundedScore
        onValueChange(roundedScore.toDouble())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCheckbox) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (showLock) {
                IconToggleButton(
                    checked = isLocked,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLockedChange(it)
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    OutlinedRoundedSymbol(
                        icon = if (isLocked) "lock" else "lock_open",
                        size = 20.dp,
                        contentDescription = if (isLocked) "解除鎖定" else "鎖定",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = shortenSubjectName(subject.subjectName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "原始 ${"%.0f".format(subject.scoreValue)}｜調整 ${signedDelta(diff)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                modifier = Modifier.clickable(onClick = onResetSubject),
                text = "%.0f".format(value),
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreStepButton(label = "-5", onClick = { updateScore(value - 5.0, haptic = true) })
            }
            FlameSlider(
                modifier = Modifier.weight(1f),
                value = value.toFloat(),
                onValueChange = { updateScore(it.roundToInt().toDouble(), haptic = true) },
                isFlameActive = isAboveMax,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScoreStepButton(label = "+5", onClick = { updateScore(value + 5.0, haptic = true) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlameSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isFlameActive: Boolean = false,
) {
    val trackColor by animateColorAsState(
        targetValue = if (isFlameActive) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(500), label = "trackColor",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (isFlameActive) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(500), label = "thumbColor",
    )

    Slider(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..100f,
        steps = 99,
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = trackColor,
            inactiveTrackColor = trackColor.copy(alpha = 0.24f),
        ),
    )
}

@Composable
private fun ScoreStepButton(
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

@Composable
private fun ChartLoadingPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun EmptyAnalysisState(message: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun signedDelta(value: Double): String = when {
    value > 0.05 -> "+${"%.1f".format(value)}"
    value < -0.05 -> "%.1f".format(value)
    else -> "0.0"
}
