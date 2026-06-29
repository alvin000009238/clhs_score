package com.clhs.score.ui.schedule

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.PERIOD_TIMES
import com.clhs.score.data.ScheduleItem
import com.clhs.score.widget.ScheduleWidget
import com.clhs.score.widget.syncAllScheduleWidgets
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    isFromLauncher: Boolean,
    onDismiss: () -> Unit,
    onSaveCompleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cacheStore = remember { GradeCacheStore(context) }

    var isLoading by remember { mutableStateOf(true) }
    var showTeacher by remember { mutableStateOf(true) }
    var showClassroom by remember { mutableStateOf(true) }
    var showTime by remember { mutableStateOf(true) }
    var scheduleItems by remember { mutableStateOf<List<ScheduleItem>?>(null) }

    LaunchedEffect(Unit) {
        val prefs = cacheStore.getWidgetPreferences()
        showTeacher = prefs.first
        showClassroom = prefs.second
        showTime = prefs.third

        val report = cacheStore.loadWidgetScheduleReport()
        scheduleItems = report?.items
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget 顯示設定") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isFromLauncher) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    cacheStore.saveWidgetPreferences(showTeacher, showClassroom, showTime)
                                    syncAllScheduleWidgets(context)
                                    onSaveCompleted()
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("完成")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "預覽效果",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Mock Widget Preview
                MockScheduleWidgetPreview(
                    items = scheduleItems,
                    showTeacher = showTeacher,
                    showClassroom = showClassroom,
                    showTime = showTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "顯示設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                fun updatePrefs(teacher: Boolean, classroom: Boolean, time: Boolean) {
                    showTeacher = teacher
                    showClassroom = classroom
                    showTime = time
                    coroutineScope.launch {
                        cacheStore.saveWidgetPreferences(teacher, classroom, time)
                        syncAllScheduleWidgets(context)
                    }
                }

                SettingSwitchRow(
                    title = "顯示任課教師",
                    checked = showTeacher,
                    onCheckedChange = { updatePrefs(it, showClassroom, showTime) }
                )
                SettingSwitchRow(
                    title = "顯示上課地點",
                    checked = showClassroom,
                    onCheckedChange = { updatePrefs(showTeacher, it, showTime) }
                )
                SettingSwitchRow(
                    title = "顯示上課時間",
                    checked = showTime,
                    onCheckedChange = { updatePrefs(showTeacher, showClassroom, it) }
                )
                
                if (!isFromLauncher) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "設定變更會自動儲存並更新桌面上的小工具。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun MockScheduleWidgetPreview(
    items: List<ScheduleItem>?,
    showTeacher: Boolean,
    showClassroom: Boolean,
    showTime: Boolean,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTotalMinutes = currentHour * 60 + currentMinute

    var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    if (dayOfWeek == 0) dayOfWeek = 7

    var displayDay = dayOfWeek
    var titleText = "今日課表"

    // Mock data fallback if real data is missing or empty
    val isUsingFakeData = items == null || items.isEmpty()
    val sourceItems = if (isUsingFakeData) {
        listOf(
            ScheduleItem(dayOfWeek = displayDay, period = 1, subjectName = "國文", teacherName = "王老師", classroom = "一年一班"),
            ScheduleItem(dayOfWeek = displayDay, period = 2, subjectName = "英文", teacherName = "李老師", classroom = "一年一班"),
            ScheduleItem(dayOfWeek = displayDay, period = 3, subjectName = "數學", teacherName = "張老師", classroom = "專科教室"),
            ScheduleItem(dayOfWeek = displayDay, period = 4, subjectName = "物理", teacherName = "林老師", classroom = "實驗室")
        )
    } else {
        items!!
    }

    var todayItems = sourceItems.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.period }

    // Logic identical to real widget
    if (!isUsingFakeData && todayItems.isEmpty()) {
        val dayNames = arrayOf("", "週一", "週二", "週三", "週四", "週五", "週六", "週日")
        for (i in 1..7) {
            val nextDay = (dayOfWeek + i - 1) % 7 + 1
            val nextItems = sourceItems.filter { it.dayOfWeek == nextDay }
            if (nextItems.isNotEmpty()) {
                displayDay = nextDay
                todayItems = nextItems.sortedBy { it.period }
                titleText = "${dayNames[displayDay]}課表"
                break
            }
        }
    }

    var currentPeriod = -1
    if (displayDay == dayOfWeek) {
        for (i in PERIOD_TIMES.indices) {
            val periodStr = PERIOD_TIMES[i]
            val startH = periodStr.start.substringBefore(":").toIntOrNull()
            val startM = periodStr.start.substringAfter(":").toIntOrNull()
            val endH = periodStr.end.substringBefore(":").toIntOrNull()
            val endM = periodStr.end.substringAfter(":").toIntOrNull()

            if (startH != null && startM != null && endH != null && endM != null) {
                val startMin = startH * 60 + startM
                val endMin = endH * 60 + endM
                if (currentTotalMinutes in startMin..endMin) {
                    currentPeriod = i + 1
                    break
                }
            }
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = titleText,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (todayItems.isEmpty()) {
                Text("今日無排課", color = MaterialTheme.colorScheme.onSurface)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(todayItems, key = { it.period }) { item ->
                        val isCurrentPeriod = item.period == currentPeriod

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrentPeriod) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isCurrentPeriod) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text(
                                    text = "第 ${item.period} 節",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                                if (showTime) {
                                    Text(
                                        text = PERIOD_TIMES.getOrNull(item.period - 1)?.singleLine ?: "",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.subjectName,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                val details = mutableListOf<String>()
                                if (showTeacher && item.teacherName.isNotBlank()) details.add(item.teacherName)
                                if (showClassroom && item.classroom.isNotBlank()) details.add(item.classroom)
                                if (details.isNotEmpty()) {
                                    Text(
                                        text = details.joinToString(" • "),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
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
