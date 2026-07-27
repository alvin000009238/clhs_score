package com.clhs.score.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.PERIOD_TIMES
import com.clhs.score.data.ScheduleItem
import com.clhs.score.data.ScheduleReport
import com.clhs.score.data.ScheduleScope
import com.clhs.score.widget.ScheduleWidgetPreferences
import com.clhs.score.widget.classifyWidgetScheduleItems
import com.clhs.score.widget.selectWidgetSchedule
import com.clhs.score.widget.widgetScheduleDateLabel
import com.clhs.score.widget.widgetScheduleDayLabel
import com.clhs.score.widget.widgetScheduleShortDateLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetSettingsScreen(
    initialPreferences: ScheduleWidgetPreferences,
    previewSize: DpSize,
    onDismiss: () -> Unit,
    onSaveCompleted: suspend (ScheduleWidgetPreferences) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cacheStore = remember { GradeCacheStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scheduleReport by cacheStore.widgetScheduleReportFlow().collectAsStateWithLifecycle(
        initialValue = null
    )

    var isSaving by remember { mutableStateOf(false) }
    var showTeacher by remember { mutableStateOf(initialPreferences.showTeacher) }
    var showClassroom by remember { mutableStateOf(initialPreferences.showClassroom) }
    var showTime by remember { mutableStateOf(initialPreferences.showTime) }
    var afterLastClass by remember { mutableStateOf(initialPreferences.afterLastClass) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("此 Widget 的設定") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        onSaveCompleted(
                                            ScheduleWidgetPreferences(
                                                showTeacher = showTeacher,
                                                showClassroom = showClassroom,
                                                showTime = showTime,
                                                afterLastClass = afterLastClass,
                                            ),
                                        )
                                    } catch (error: Exception) {
                                        if (error is CancellationException) throw error
                                        snackbarHostState.showSnackbar("儲存失敗，請再試一次")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (isSaving) "儲存中" else "完成")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "即時預覽",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            Card(
                modifier = Modifier
                    .width(previewSize.width)
                    .height(previewSize.height)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                ScheduleWidgetPreview(
                    report = scheduleReport,
                    showTeacher = showTeacher,
                    showClassroom = showClassroom,
                    showTime = showTime,
                    afterLastClass = afterLastClass,
                    widgetSize = previewSize,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            if (previewSize.height < 160.dp) 8.dp
                            else if (previewSize.height < 260.dp) 12.dp
                            else 16.dp,
                        ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "顯示內容",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                SettingSwitchRow(
                    title = "任課教師",
                    subtitle = "顯示授課教師；小尺寸會自動隱藏",
                    checked = showTeacher,
                    onCheckedChange = { showTeacher = it },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingSwitchRow(
                    title = "上課地點",
                    subtitle = "顯示上課地點；小尺寸會自動隱藏",
                    checked = showClassroom,
                    onCheckedChange = { showClassroom = it },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingSwitchRow(
                    title = "上課時間",
                    subtitle = "顯示每節課的起訖時間",
                    checked = showTime,
                    onCheckedChange = { showTime = it },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "課表切換時機",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                SettingSwitchRow(
                    title = "放學後顯示下一個上課日",
                    subtitle = "開啟後，最後一節下課即切換；關閉則於午夜切換",
                    checked = afterLastClass,
                    onCheckedChange = { afterLastClass = it },
                )
            }

        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ScheduleWidgetPreview(
    report: ScheduleReport?,
    showTeacher: Boolean,
    showClassroom: Boolean,
    showTime: Boolean,
    afterLastClass: Boolean,
    widgetSize: DpSize,
    modifier: Modifier = Modifier
) {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val currentTotalMinutes = now.hour * 60 + now.minute

    val sourceItems = report?.items.orEmpty()
    val validFrom = report
        ?.takeIf { it.scope == ScheduleScope.CURRENT_WEEK }
        ?.weekStartDate
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: today
    val selection = selectWidgetSchedule(sourceItems, today, currentTotalMinutes, afterLastClass, validFrom)
    val isExpiredCurrentWeek =
        report?.scope == ScheduleScope.CURRENT_WEEK && !report.isValidOn(selection.date)
    val sections = classifyWidgetScheduleItems(
        items = selection.items,
        selectionDate = selection.date,
        today = today,
        currentMinutes = currentTotalMinutes,
    )
    val isShort = widgetSize.height < 160.dp
    val isNarrow = widgetSize.width < 220.dp
    val isCompact = widgetSize.height < 260.dp
    val prioritizedItems = if (isShort) sections.prioritized.take(1) else sections.prioritized

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = widgetScheduleDayLabel(selection.date, today),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isShort || isNarrow) {
                        widgetScheduleShortDateLabel(selection.date)
                    } else {
                        widgetScheduleDateLabel(selection.date)
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                if (!isShort &&
                    !isNarrow &&
                    !isExpiredCurrentWeek &&
                    report?.scope == ScheduleScope.CURRENT_WEEK &&
                    !report.weekNo.isNullOrBlank()
                ) {
                    Text(
                        text = "第 ${report.weekNo} 週",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            if (report == null) {
                Text(
                    "尚未登入或無課表資料",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (isExpiredCurrentWeek) {
                Text(
                    "本週課表已過期\n開啟 App 更新",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (selection.items.isEmpty()) {
                Text(
                    "今日無排課",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (sections.prioritized.isEmpty() && sections.completed.isNotEmpty()) {
                        item(key = -1) {
                            Text(
                                "今日課程已結束",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    bottom = if (isCompact) 0.dp else 8.dp,
                                ),
                            )
                        }
                    }
                    items(prioritizedItems, key = { it.period }) { item ->
                        PreviewScheduleItemRow(
                            item = item,
                            isCurrent = item == sections.current,
                            isCompleted = false,
                            showTeacher = showTeacher && !isShort && !isNarrow,
                            showClassroom = showClassroom && !isShort && !isNarrow,
                            showTime = showTime,
                            isSmall = isShort,
                        )
                    }
                    if (!isCompact && sections.completed.isNotEmpty()) {
                        item(key = -2) {
                            Text(
                                text = "已下課",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items(sections.completed, key = { 100 + it.period }) { item ->
                            PreviewScheduleItemRow(
                                item = item,
                                isCurrent = false,
                                isCompleted = true,
                                showTeacher = showTeacher,
                                showClassroom = showClassroom,
                                showTime = showTime,
                                isSmall = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewScheduleItemRow(
    item: ScheduleItem,
    isCurrent: Boolean,
    isCompleted: Boolean,
    showTeacher: Boolean,
    showClassroom: Boolean,
    showTime: Boolean,
    isSmall: Boolean,
) {
    val contentColor = when {
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val periodColor = when {
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val details = buildList {
        if (showTeacher && item.teacherName.isMeaningfulPreviewText()) add(item.teacherName)
        if (showClassroom && item.classroom.isMeaningfulPreviewText()) add(item.classroom)
    }.joinToString(" • ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isCurrent) 12.dp else 0.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .padding(horizontal = 8.dp, vertical = if (isSmall) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = 10.dp)) {
            Text(
                text = "第 ${item.period} 節",
                fontWeight = FontWeight.Bold,
                color = periodColor,
                fontSize = 12.sp,
            )
            if (showTime) {
                PERIOD_TIMES.getOrNull(item.period - 1)?.singleLine?.let { periodTime ->
                    Text(
                        text = periodTime,
                        color = periodColor,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.subjectName,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 14.sp,
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun String.isMeaningfulPreviewText(): Boolean = isNotBlank() && this != "null"
