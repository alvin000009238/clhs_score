package com.clhs.score.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.clhs.score.data.SettingsRepository
import com.clhs.score.ui.schedule.WidgetSettingsScreen
import com.clhs.score.ui.theme.ScoreTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Default to CANCELED. If user backs out, the widget is not added.
        setResult(RESULT_CANCELED)

        val intentExtras = intent?.extras
        if (intentExtras != null) {
            appWidgetId = intentExtras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val settingsRepository = SettingsRepository(applicationContext)
            val settings = runBlocking { settingsRepository.settings.first() }

            ScoreTheme(
                themeMode = settings.themeMode,
                amoledBlack = settings.amoledBlack,
                dynamicColor = settings.dynamicColor
            ) {
                WidgetSettingsScreen(
                    isFromLauncher = true,
                    onDismiss = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onSaveCompleted = {
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}
