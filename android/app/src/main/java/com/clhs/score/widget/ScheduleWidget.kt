package com.clhs.score.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import androidx.glance.layout.height
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
import com.clhs.score.data.AppSettings
import com.clhs.score.data.SettingsRepository
import com.clhs.score.data.ThemeMode
import com.clhs.score.ui.theme.AmoledDarkColors
import com.clhs.score.ui.theme.DarkColors
import com.clhs.score.ui.theme.LightColors
import kotlinx.coroutines.flow.first
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.material3.ColorProviders
import java.util.Calendar

data class ScheduleWidgetDisplayPreferences(
    val showTeacher: Boolean,
    val showClassroom: Boolean,
    val showTime: Boolean,
)

fun getWidgetColorProviders(context: Context, settings: AppSettings) = run {
    val dynamicColor = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    
    val light = if (dynamicColor) {
        dynamicLightColorScheme(context)
    } else {
        LightColors
    }
    
    val dark = if (dynamicColor) {
        val base = dynamicDarkColorScheme(context)
        if (settings.amoledBlack) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF141414),
            )
        } else base
    } else {
        if (settings.amoledBlack) AmoledDarkColors else DarkColors
    }

    when (settings.themeMode) {
        ThemeMode.LIGHT -> ColorProviders(light = light, dark = light)
        ThemeMode.DARK -> ColorProviders(light = dark, dark = dark)
        ThemeMode.SYSTEM -> ColorProviders(light = light, dark = dark)
    }
}

class ScheduleWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cacheStore = GradeCacheStore(context)
        val settingsRepository = SettingsRepository(context)

        val report = cacheStore.loadWidgetScheduleReport()
        val prefs = cacheStore.getWidgetPreferences()
        val displayPreferences = ScheduleWidgetDisplayPreferences(
            showTeacher = prefs.first,
            showClassroom = prefs.second,
            showTime = prefs.third,
        )
        val appSettings = settingsRepository.settings.first()

        provideContent {
            val colors = getWidgetColorProviders(context, appSettings)
            GlanceTheme(colors = colors) {
                ScheduleWidgetContent(report?.items, displayPreferences)
            }
        }
    }
}

@Composable
fun ScheduleWidgetContent(
    items: List<ScheduleItem>?,
    preferences: ScheduleWidgetDisplayPreferences = ScheduleWidgetDisplayPreferences(
        showTeacher = true,
        showClassroom = true,
        showTime = true,
    ),
) {
    val showTeacher = preferences.showTeacher
    val showClassroom = preferences.showClassroom
    val showTime = preferences.showTime
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)
    val currentTotalMinutes = currentHour * 60 + currentMinute

    var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday=0, Monday=1..
    if (dayOfWeek == 0) dayOfWeek = 7 // Adjust for standard week

    var displayDay = dayOfWeek
    var titleText = "今日課表"
    var todayItems = items?.filter { it.dayOfWeek == dayOfWeek }?.sortedBy { it.period } ?: emptyList()

    // Find next school day if today is empty
    if (items != null && todayItems.isEmpty()) {
        val dayNames = arrayOf("", "週一", "週二", "週三", "週四", "週五", "週六", "週日")
        for (i in 1..7) {
            val nextDay = (dayOfWeek + i - 1) % 7 + 1
            val nextItems = items.filter { it.dayOfWeek == nextDay }
            if (nextItems.isNotEmpty()) {
                displayDay = nextDay
                todayItems = nextItems.sortedBy { it.period }
                titleText = "${dayNames[displayDay]}課表"
                break
            }
        }
    }

    // Determine current period once outside the loop to save computation
    var currentPeriod = -1
    if (displayDay == dayOfWeek) {
        for (i in com.clhs.score.data.PERIOD_TIMES.indices) {
            val periodStr = com.clhs.score.data.PERIOD_TIMES[i]
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


    val context = LocalContext.current
    val intent = Intent(
        Intent.ACTION_VIEW,
        "scoreapp://schedule".toUri(),
        context,
        com.clhs.score.MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(16.dp)
            .clickable(actionStartActivity(intent)),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = titleText,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onBackground
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            if (items == null) {
                Text(text = "尚未登入或無課表資料", style = TextStyle(color = GlanceTheme.colors.onBackground))
            } else if (todayItems.isEmpty()) {
                Text(text = "今日無排課", style = TextStyle(color = GlanceTheme.colors.onBackground))
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(todayItems, itemId = { it.period.toLong() }) { item ->
                        val isCurrentPeriod = item.period == currentPeriod

                        val rowModifier = if (isCurrentPeriod) {
                            GlanceModifier
                                .fillMaxWidth()
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(8.dp)
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        } else {
                            GlanceModifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .cornerRadius(0.dp)
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        }

                        Row(
                            modifier = rowModifier,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            if (isCurrentPeriod) {
                                Spacer(modifier = GlanceModifier.width(4.dp).height(32.dp).background(GlanceTheme.colors.primary).cornerRadius(2.dp))
                            } else {
                                Spacer(modifier = GlanceModifier.width(4.dp).height(32.dp).background(Color.Transparent).cornerRadius(0.dp))
                            }
                            Spacer(modifier = GlanceModifier.width(8.dp))
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
        } // End of Column
    }
}


