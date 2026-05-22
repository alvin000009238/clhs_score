package com.clhs.score.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clhs.score.data.AppSettings
import com.clhs.score.data.ThemeMode
import com.clhs.score.viewmodel.GradesUiState
import com.clhs.score.viewmodel.LoginUiState
import com.clhs.score.viewmodel.SettingsUiState

private const val GradesRoute = "grades"
private const val ScoreSimulatorRoute = "score-simulator"
private const val SettingsRoute = "settings"
private const val DeveloperSettingsRoute = "developer-settings"

@Composable
fun ScoreApp(
    loginState: LoginUiState,
    gradesState: GradesUiState,
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    onWebViewLoginSuccess: (studentNo: String, cookieString: String) -> Unit,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
    onReload: () -> Unit,
    onLogout: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onDismissLoginError: () -> Unit,
    onDismissGradesError: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdateResult: () -> Unit,
    onVersionTap: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onSetDemoMode: (Boolean) -> Unit,
    onDismissRestartDialog: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showWebView by remember { mutableStateOf(false) }

    LaunchedEffect(loginState.errorMessage) {
        val message = loginState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissLoginError()
    }
    LaunchedEffect(gradesState.errorMessage) {
        val message = gradesState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissGradesError()
    }

    AnimatedContent(
        targetState = gradesState.isLoggedIn,
        transitionSpec = {
            (fadeIn(tween(400)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(400),
            )).togetherWith(fadeOut(tween(300)))
        },
        label = "loginTransition",
    ) { isLoggedIn ->
        if (isLoggedIn) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = GradesRoute) {
                composable(GradesRoute) {
                    GradesScreen(
                        state = gradesState,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        onSelectYear = onSelectYear,
                        onSelectExam = onSelectExam,
                        onReload = onReload,
                        onToggleSubject = onToggleSubject,
                        onOpenScoreSimulator = { navController.navigate(ScoreSimulatorRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable(ScoreSimulatorRoute) {
                    ScoreSimulatorScreen(
                        state = gradesState,
                        snackbarHostState = snackbarHostState,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(SettingsRoute) {
                    SettingsScreen(
                        settings = settings,
                        uiState = settingsUiState,
                        onBack = { navController.popBackStack() },
                        onSetThemeMode = onSetThemeMode,
                        onSetDynamicColor = onSetDynamicColor,
                        onSetAmoledBlack = onSetAmoledBlack,
                        onCheckUpdate = onCheckUpdate,
                        onDismissUpdateResult = onDismissUpdateResult,
                        onVersionTap = onVersionTap,
                        onDismissDeveloperToast = onDismissDeveloperToast,
                        onOpenDeveloperSettings = { navController.navigate(DeveloperSettingsRoute) },
                        onLogout = onLogout,
                    )
                }
                composable(DeveloperSettingsRoute) {
                    DeveloperSettingsScreen(
                        settings = settings,
                        showRestartDialog = settingsUiState.showRestartDialog,
                        onBack = { navController.popBackStack() },
                        onSetDemoMode = onSetDemoMode,
                        onDismissRestartDialog = onDismissRestartDialog,
                    )
                }
            }
        } else {
            AnimatedContent(
                targetState = showWebView,
                transitionSpec = {
                    fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                },
                label = "webViewTransition"
            ) { isWebViewVisible ->
                if (isWebViewVisible) {
                    WebViewLoginScreen(
                        isProcessingLogin = loginState.isWebViewLoginInProgress,
                        errorMessage = loginState.errorMessage,
                        onLoginSuccess = onWebViewLoginSuccess,
                        onBack = { showWebView = false },
                        onDismissError = onDismissLoginError,
                    )
                } else {
                    IntroScreen(
                        showSkipButton = settings.demoMode,
                        onSkipClick = { onWebViewLoginSuccess("demo-student", "fake=cookie") },
                        onLoginClick = { showWebView = true }
                    )
                }
            }
        }
    }
}
