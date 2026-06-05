package com.clhs.score.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clhs.score.data.AppSettings
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.ThemeMode
import com.clhs.score.viewmodel.GradesUiState
import com.clhs.score.viewmodel.LoginUiState
import com.clhs.score.viewmodel.ScheduleViewModel
import com.clhs.score.viewmodel.SettingsUiState
import kotlinx.coroutines.launch

private const val GradesRoute = "grades"
private const val ScoreSimulatorRoute = "score-simulator"
private const val SubjectTrendRoute = "subject-trend"
private const val ScheduleRoute = "schedule"
private const val SettingsRoute = "settings"
private const val DeveloperSettingsRoute = "developer-settings"

@Composable
fun ScoreApp(
    scoreViewModel: com.clhs.score.viewmodel.ScoreViewModel,
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
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdateResult: () -> Unit,
    onVersionTap: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onSetDemoMode: (Boolean) -> Unit,
    onDismissRestartDialog: () -> Unit,
    onDismissNotificationPrompt: () -> Unit,
    onExportGrades: (List<ExamSelection>) -> Unit,
    onDismissExportResult: () -> Unit,
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

    UpdateResultDialog(
        result = settingsUiState.updateResult,
        onDismiss = onDismissUpdateResult,
    )

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
            NavHost(
                navController = navController,
                startDestination = GradesRoute,
                enterTransition = { fadeIn(tween(150)) + slideInHorizontally(tween(150)) { it / 8 } },
                exitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 8 } },
                popEnterTransition = { fadeIn(tween(150)) + slideInHorizontally(tween(150)) { -it / 8 } },
                popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 8 } },
            ) {
                composable(GradesRoute) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        NotificationPromptDialog(
                            settings = settings,
                            onEnableNotifications = onSetNotificationsEnabled,
                            onDismiss = onDismissNotificationPrompt,
                        )
                    }
                    GradesScreen(
                        state = gradesState,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        onSelectYear = onSelectYear,
                        onSelectExam = onSelectExam,
                        onReload = onReload,
                        onToggleSubject = onToggleSubject,
                        onOpenScoreSimulator = { navController.navigate(ScoreSimulatorRoute) },
                        onOpenSchedule = { navController.navigate(ScheduleRoute) },
                        onOpenSubjectTrend = {
                            navController.navigate(SubjectTrendRoute)
                        },
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
                composable(SubjectTrendRoute) {
                    LaunchedEffect(Unit) {
                        scoreViewModel.initSubjectTrend()
                    }
                    SubjectTrendScreen(
                        viewModel = scoreViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = ScheduleRoute,
                    deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "scoreapp://schedule" })
                ) {
                    val context = LocalContext.current
                    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ScheduleViewModel>(
                        factory = ScheduleViewModel.factory(context, settings.demoMode),
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val coroutineScope = rememberCoroutineScope()
                    com.clhs.score.ui.schedule.ScheduleScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onRefresh = { viewModel.refresh() },
                        onYearSelected = { viewModel.selectYear(it) },
                        onClassSelected = { viewModel.selectClass(it) },
                        onConfirmSelection = { viewModel.confirmSelection() },
                        onClearSelection = { viewModel.clearSelection() },
                        onSaveWidgetPreferences = { showTeacher, showClassroom, showTime ->
                            viewModel.saveWidgetPreferences(showTeacher, showClassroom, showTime)
                            coroutineScope.launch {
                                com.clhs.score.widget.ScheduleWidget().updateAll(context)
                            }
                        }
                    )
                }
                composable(SettingsRoute) {
                    SettingsScreen(
                        settings = settings,
                        uiState = settingsUiState,
                        structure = gradesState.structure,
                        isExporting = gradesState.isExporting,
                        exportResult = gradesState.exportResult,
                        onBack = { navController.popBackStack() },
                        onSetThemeMode = onSetThemeMode,
                        onSetDynamicColor = onSetDynamicColor,
                        onSetAmoledBlack = onSetAmoledBlack,
                        onSetNotificationsEnabled = onSetNotificationsEnabled,
                        onCheckUpdate = onCheckUpdate,
                        onDismissUpdateResult = onDismissUpdateResult,
                        onVersionTap = onVersionTap,
                        onDismissDeveloperToast = onDismissDeveloperToast,
                        onOpenDeveloperSettings = { navController.navigate(DeveloperSettingsRoute) },
                        onExportGrades = onExportGrades,
                        onDismissExportResult = onDismissExportResult,
                        onLogout = onLogout,
                    )
                }
                composable(DeveloperSettingsRoute) {
                    DeveloperSettingsScreen(
                        settings = settings,
                        showRestartDialog = settingsUiState.showRestartDialog,
                        isLoggedIn = gradesState.isLoggedIn,
                        loginErrorMessage = loginState.errorMessage,
                        gradesErrorMessage = gradesState.errorMessage,
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
                        onSkipClick = { onWebViewLoginSuccess("DEMO-000", "fake=cookie") },
                        onLoginClick = { showWebView = true }
                    )
                }
            }
        }
    }
}
