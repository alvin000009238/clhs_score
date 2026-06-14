package com.clhs.score.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.getSubjectBaseName
import com.clhs.score.data.shortenSubjectName
import com.clhs.score.viewmodel.ScoreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubjectTrendScreen(
    viewModel: ScoreViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.subjectTrendState.collectAsStateWithLifecycle()
    val gradesState by viewModel.gradesState.collectAsStateWithLifecycle()
    val structure = gradesState.structure
    var showSubjectBottomSheet by remember { mutableStateOf(false) }
    var selectedBaseName by remember { mutableStateOf<String?>(null) }

    // Extract all unique subjects from the fetched reports
    val availableSubjects = remember(state.reports) {
        state.reports.flatMap { report ->
            report.subjects.map { cleanSubjectName(it.subjectName).substringBefore("-") }
        }.toSet().sorted()
    }

    // Chart Colors mapped to subjects for legend
    val subjectColors = remember(state.selectedSubjectKeys) {
        val colors = listOf(
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5),
            Color(0xFF00BCD4), Color(0xFF4CAF50), Color(0xFFFF9800),
            Color(0xFF795548), Color(0xFF607D8B), Color(0xFFF44336),
            Color(0xFF009688), Color(0xFFCDDC39), Color(0xFFFFC107),
            Color(0xFFFF5722), Color(0xFF8BC34A), Color(0xFF673AB7),
            Color(0xFF03A9F4), Color(0xFFE040FB), Color(0xFF00E5FF)
        )
        val baseNameColors = mutableMapOf<String, Color>()
        var colorIndex = 0
        state.selectedSubjectKeys.toList().associateWith { key ->
            val baseName = getSubjectBaseName(key)
            baseNameColors.getOrPut(baseName) { colors[colorIndex++ % colors.size] }
        }
    }

    // Memoized legend groups to prevent recalculation during recomposition
    val groupedLegend = remember(state.selectedSubjectKeys, state.reports) {
        val baseGroup = state.selectedSubjectKeys.groupBy { getSubjectBaseName(it) }
        // Pre-extract all reported subject names to avoid flatMapping repeatedly
        val allReportedSubjects = state.reports.flatMap { report ->
            report.subjects.map { it.subjectName }
        }
        baseGroup.mapValues { (_, keys) ->
            val firstKey = allReportedSubjects.firstOrNull { it in keys } ?: keys.first()
            firstKey to keys
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成績折線圖") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Chart Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 400.dp)
                            .aspectRatio(1.5f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        if (state.reports.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (state.isLoading) "載入中..." else "請選擇學期與科目以顯示圖表")
                            }
                        } else {
                            SubjectTrendLineChart(
                                reports = state.reports,
                                selectedSubjectKeys = state.selectedSubjectKeys,
                                subjectColors = subjectColors,
                                selectedBaseName = selectedBaseName,
                                onBaseNameSelected = { selectedBaseName = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // 2. Legend Section
                if (state.selectedSubjectKeys.isNotEmpty()) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupedLegend.forEach { (baseName, pair) ->
                                val (firstKey, keys) = pair
                                
                                val color = subjectColors[firstKey] ?: Color.Black
                                val label = keys.map { shortenSubjectName(it) }.distinct().joinToString("/")
                                val isSelected = baseName == selectedBaseName
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            selectedBaseName = if (isSelected) null else baseName
                                        }
                                        .background(
                                            color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Semesters Filter
                item {
                    Text("選擇學期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        structure.forEach { yearTerm ->
                            FilterChip(
                                selected = state.selectedYearValues.contains(yearTerm.value),
                                onClick = { viewModel.toggleSubjectTrendYear(yearTerm.value) },
                                label = { Text(yearTerm.text) }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                // 4. Subjects Filter Button
                item {
                    ElevatedButton(
                        onClick = { showSubjectBottomSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("新增 / 變更對比科目")
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showSubjectBottomSheet) {
        SubjectSelectionSheet(
            subjects = availableSubjects,
            selectedSubjectKeys = state.selectedSubjectKeys,
            onToggleSubject = viewModel::toggleSubjectTrendSubject,
            onDismiss = { showSubjectBottomSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectSelectionSheet(
    subjects: List<String>,
    selectedSubjectKeys: Set<String>,
    onToggleSubject: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Text(
                text = "選擇要顯示的科目",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(subjects) { subjectKey ->
                    SubjectSelectionRow(
                        subjectKey = subjectKey,
                        selected = selectedSubjectKeys.contains(subjectKey),
                        onToggleSubject = onToggleSubject,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("確認", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SubjectSelectionRow(
    subjectKey: String,
    selected: Boolean,
    onToggleSubject: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSubject(subjectKey) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = subjectKey, style = MaterialTheme.typography.bodyLarge)
    }
}
