package com.clhs.score

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clhs.score.notifications.NotificationChannels
import com.clhs.score.notifications.ScoreFirebaseMessagingService
import com.clhs.score.ui.ScoreApp
import com.clhs.score.ui.theme.ScoreTheme
import com.clhs.score.viewmodel.ScoreViewModel
import com.clhs.score.viewmodel.SettingsViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val checkUpdateChannel = Channel<Unit>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.ensureCreated(this)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(applicationContext),
            )
            val appSettings by settingsVm.settings.collectAsState()
            val settingsUi by settingsVm.uiState.collectAsState()
            val isReady by settingsVm.isReady.collectAsState()

            LaunchedEffect(Unit) {
                checkUpdateChannel.receiveAsFlow().collect {
                    settingsVm.checkUpdate()
                }
            }

            if (!isReady) {
                return@setContent
            }

            ScoreTheme(
                themeMode = appSettings.themeMode,
                dynamicColor = appSettings.dynamicColor,
                amoledBlack = appSettings.amoledBlack,
            ) {
                val useFakeData = BuildConfig.USE_FAKE_DATA || appSettings.demoMode
                val scoreVm: ScoreViewModel = viewModel(
                    factory = ScoreViewModel.factory(
                        context = applicationContext,
                        useFakeData = useFakeData,
                    ),
                )
                val loginState by scoreVm.loginState.collectAsState()
                val gradesState by scoreVm.gradesState.collectAsState()
                ScoreApp(
                    scoreViewModel = scoreVm,
                    loginState = loginState,
                    gradesState = gradesState,
                    settings = appSettings,
                    settingsUiState = settingsUi,
                    onWebViewLoginSuccess = scoreVm::loginWithWebViewCookies,
                    onSelectYear = scoreVm::selectYear,
                    onSelectExam = scoreVm::selectExam,
                    onReload = scoreVm::reloadStructure,
                    onLogout = {
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.CookieManager.getInstance().flush()
                        scoreVm.logout()
                    },
                    onToggleSubject = scoreVm::toggleSubjectExpanded,
                    onDismissLoginError = scoreVm::clearLoginError,
                    onDismissGradesError = scoreVm::clearGradesError,
                    onSetThemeMode = settingsVm::setThemeMode,
                    onSetDynamicColor = settingsVm::setDynamicColor,
                    onSetAmoledBlack = settingsVm::setAmoledBlack,
                    onSetNotificationsEnabled = settingsVm::setNotificationsEnabled,
                    onCheckUpdate = settingsVm::checkUpdate,
                    onDismissUpdateResult = settingsVm::dismissUpdateResult,
                    onVersionTap = settingsVm::onVersionTap,
                    onDismissDeveloperToast = settingsVm::dismissDeveloperToast,
                    onSetDemoMode = settingsVm::setDemoMode,
                    onDismissRestartDialog = settingsVm::dismissRestartDialog,
                    onDismissNotificationPrompt = settingsVm::dismissNotificationPrompt,
                    onExportGrades = { selections -> scoreVm.exportGrades(selections, applicationContext) },
                    onDismissExportResult = scoreVm::dismissExportResult,
                )
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val isUpdateTopic = intent.getStringExtra("from") == "/topics/app_updates" ||
            intent.getStringExtra(ScoreFirebaseMessagingService.EXTRA_CHECK_UPDATE) == "true" ||
            intent.getStringExtra("action") == "check_update" ||
            intent.getBooleanExtra(ScoreFirebaseMessagingService.EXTRA_CHECK_UPDATE, false)

        if (isUpdateTopic) {
            checkUpdateChannel.trySend(Unit)
            intent.removeExtra("from")
            intent.removeExtra("action")
            intent.removeExtra(ScoreFirebaseMessagingService.EXTRA_CHECK_UPDATE)
        }
    }
}
