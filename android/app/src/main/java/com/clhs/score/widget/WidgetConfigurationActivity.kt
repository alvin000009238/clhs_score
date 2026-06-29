package com.clhs.score.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.clhs.score.data.AppSettings
import com.clhs.score.data.SettingsRepository
import com.clhs.score.ui.schedule.WidgetSettingsScreen
import com.clhs.score.ui.theme.ScoreTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val launchSettings = mutableStateOf<AppSettings?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { launchSettings.value == null }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = runCatching { splashScreenView.iconView }.getOrNull()
            if (iconView == null) {
                splashScreenView.remove()
                return@setOnExitAnimationListener
            }
            iconView.animate()
                .alpha(0f)
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(200L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { splashScreenView.remove() }
                .start()
        }

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

        val settingsRepository = SettingsRepository(applicationContext)
        lifecycleScope.launch {
            launchSettings.value = settingsRepository.settings.first()
        }

        setContent {
            val initialSettings = launchSettings.value ?: return@setContent
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = initialSettings
            )

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
                        withContext(Dispatchers.IO) {
                            syncScheduleWidget(applicationContext, appWidgetId)
                        }
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
