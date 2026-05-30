package com.clhs.score.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.ScheduleItem
import com.clhs.score.data.SessionStore
import java.util.Calendar

class ScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sessionStore = SessionStore(context)
        val cacheStore = GradeCacheStore(context)

        val session = sessionStore.loadSession()
        val report = if (session != null) {
            cacheStore.loadLatestScheduleReport(session.studentNo)
        } else {
            null
        }
        
        val prefs = cacheStore.getWidgetPreferences()
        val showTeacher = prefs.first
        val showClassroom = prefs.second
        val showTime = prefs.third

        provideContent {
            GlanceTheme {
                ScheduleWidgetContent(report?.items, showTeacher, showClassroom, showTime)
            }
        }
    }
}

@Composable
fun ScheduleWidgetContent(items: List<ScheduleItem>?, showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean) {
    val calendar = Calendar.getInstance()
    var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday=0, Monday=1..
    if (dayOfWeek == 0) dayOfWeek = 7 // Adjust for standard week

    val todayItems = items?.filter { it.dayOfWeek == dayOfWeek }?.sortedBy { it.period } ?: emptyList()
    


    val context = LocalContext.current
    val intent = Intent(
        Intent.ACTION_VIEW,
        "scoreapp://schedule".toUri(),
        context,
        com.clhs.score.MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(16.dp)
            .clickable(actionStartActivity(intent)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = "今日課表",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onBackground
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )

            if (items == null) {
                Text(text = "尚未登入或無課表資料", style = TextStyle(color = GlanceTheme.colors.onBackground))
            } else if (todayItems.isEmpty()) {
                Text(text = "今日無排課", style = TextStyle(color = GlanceTheme.colors.onBackground))
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(todayItems) { item ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.padding(end = 8.dp)) {
                                Text(
                                    text = "第 ${item.period} 節",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.primary
                                    )
                                )
                                if (showTime) {
                                    Text(
                                        text = com.clhs.score.data.PERIOD_TIMES.getOrNull(item.period - 1)?.singleLine ?: "",
                                        style = TextStyle(color = GlanceTheme.colors.primary)
                                    )
                                }
                            }
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                if (item.subjectName.isNotBlank() && item.subjectName != "null") {
                                    Text(
                                        text = item.subjectName,
                                        style = TextStyle(color = GlanceTheme.colors.onBackground)
                                    )
                                }
                                if (showTeacher && item.teacherName.isNotBlank() && item.teacherName != "null") {
                                    Text(
                                        text = item.teacherName,
                                        style = TextStyle(color = GlanceTheme.colors.onBackground)
                                    )
                                }
                                if (showClassroom && item.classroom.isNotBlank() && item.classroom != "null") {
                                    Text(
                                        text = item.classroom,
                                        style = TextStyle(color = GlanceTheme.colors.onBackground)
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
