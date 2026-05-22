package com.clhs.score

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clhs.score.ui.ScoreApp
import com.clhs.score.ui.theme.ScoreTheme
import com.clhs.score.viewmodel.ScoreViewModel
import com.clhs.score.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(applicationContext),
            )
            val appSettings by settingsVm.settings.collectAsState()
            val settingsUi by settingsVm.uiState.collectAsState()
            val isReady by settingsVm.isReady.collectAsState()

            if (!isReady) {
                return@setContent
            }

            ScoreTheme(
                themeMode = appSettings.themeMode,
                dynamicColor = appSettings.dynamicColor,
                amoledBlack = appSettings.amoledBlack,
            ) {
                val useFakeData = BuildConfig.USE_FAKE_DATA || appSettings.demoMode
                val viewModel: ScoreViewModel = viewModel(
                    factory = ScoreViewModel.factory(
                        context = applicationContext,
                        useFakeData = useFakeData,
                    ),
                )
                val loginState by viewModel.loginState.collectAsState()
                val gradesState by viewModel.gradesState.collectAsState()
                ScoreApp(
                    loginState = loginState,
                    gradesState = gradesState,
                    settings = appSettings,
                    settingsUiState = settingsUi,
                    onWebViewLoginSuccess = viewModel::loginWithWebViewCookies,
                    onSelectYear = viewModel::selectYear,
                    onSelectExam = viewModel::selectExam,
                    onReload = viewModel::reloadStructure,
                    onLogout = {
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.CookieManager.getInstance().flush()
                        viewModel.logout()
                    },
                    onToggleSubject = viewModel::toggleSubjectExpanded,
                    onDismissLoginError = viewModel::clearLoginError,
                    onDismissGradesError = viewModel::clearGradesError,
                    onSetThemeMode = settingsVm::setThemeMode,
                    onSetDynamicColor = settingsVm::setDynamicColor,
                    onSetAmoledBlack = settingsVm::setAmoledBlack,
                    onCheckUpdate = settingsVm::checkUpdate,
                    onDismissUpdateResult = settingsVm::dismissUpdateResult,
                    onVersionTap = settingsVm::onVersionTap,
                    onDismissDeveloperToast = settingsVm::dismissDeveloperToast,
                    onSetDemoMode = settingsVm::setDemoMode,
                    onDismissRestartDialog = settingsVm::dismissRestartDialog,
                )
            }
        }
    }
}
