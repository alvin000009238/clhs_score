package com.clhs.score.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clhs.score.analytics.AnalyticsEvents
import com.clhs.score.analytics.AnalyticsLogger
import com.clhs.score.analytics.AnalyticsParameterSanitizer
import com.clhs.score.analytics.AnalyticsParams
import com.clhs.score.analytics.AnalyticsValues
import com.clhs.score.analytics.NoOpAnalyticsLogger
import com.clhs.score.analytics.UsageStatisticsStore
import com.clhs.score.data.AppSettings
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.SCHOOL_CALENDAR_WEB_URL
import com.clhs.score.data.SCHOOL_ANNOUNCEMENTS_WEB_URL
import com.clhs.score.data.ThemeMode
import com.clhs.score.data.safeAnnouncementWebUrl
import com.clhs.score.data.schoolAnnouncementOfficialUrl
import com.clhs.score.notifications.canPostNotifications
import com.clhs.score.viewmodel.GradesUiState
import com.clhs.score.viewmodel.LoginUiState
import com.clhs.score.viewmodel.ScheduleViewModel
import com.clhs.score.viewmodel.SchoolCalendarViewModel
import com.clhs.score.viewmodel.SchoolAnnouncementDetailViewModel
import com.clhs.score.viewmodel.SchoolAnnouncementsViewModel
import com.clhs.score.viewmodel.SettingsUiState

private const val IntroRoute = "intro"
private const val WebViewLoginRoute = "web-view-login"
private const val GradesRoute = "grades"
private const val ScoreSimulatorRoute = "score-simulator"
private const val SubjectTrendRoute = "subject-trend"
private const val ScheduleRoute = "schedule"
private const val SchoolCalendarRoute = "school-calendar"
private const val SchoolAnnouncementsRoute = "school-announcements"
private const val SchoolAnnouncementDetailRoute =
    "school-announcement/{announcementId}?category={category}"
private const val SchoolWebsiteRoute = "school-website"
private const val SettingsRoute = "settings"
private const val AboutRoute = "about"
private const val UsageStatisticsRoute = "usage-statistics"
private const val OpenSourceLicensesRoute = "open-source-licenses"
private const val DeveloperSettingsRoute = "developer-settings"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScoreApp(
    scoreViewModel: com.clhs.score.viewmodel.ScoreViewModel,
    loginState: LoginUiState,
    gradesState: GradesUiState,
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    openScheduleRequested: Boolean,
    onScheduleOpenHandled: () -> Unit,
    onWebViewLoginSuccess: (studentNo: String, cookieString: String) -> Unit,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
    onReload: () -> Unit,
    onLogout: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onDismissLoginError: () -> Unit,
    onDismissGradesError: () -> Unit,
    onStartGradeReminder: () -> Unit,
    onStopGradeReminder: () -> Unit,
    onGradeReminderPrerequisiteFailed: (String) -> Unit,
    onDismissGradeReminderError: () -> Unit,
    onDismissGradeReminderChanges: () -> Unit,
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
    analyticsLogger: AnalyticsLogger = NoOpAnalyticsLogger,
    onSetBiometricEnabled: (Boolean, String?) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val expressiveMotion = MaterialTheme.motionScheme
    val standardMotion = remember { MotionScheme.standard() }
    SystemNotificationPermissionSync(
        settings = settings,
        onSetNotificationsEnabled = onSetNotificationsEnabled,
        analyticsLogger = analyticsLogger,
    )

    LaunchedEffect(gradesState.errorMessage) {
        val message = gradesState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissGradesError()
    }
    LaunchedEffect(gradesState.gradeReminderError) {
        val message = gradesState.gradeReminderError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissGradeReminderError()
    }

    UpdateResultDialog(
        result = settingsUiState.updateResult,
        onDismiss = onDismissUpdateResult,
    )

    AnimatedContent(
        targetState = gradesState.isLoggedIn,
        transitionSpec = {
            (fadeIn(expressiveMotion.defaultEffectsSpec()) + scaleIn(
                initialScale = 0.92f,
                animationSpec = expressiveMotion.defaultSpatialSpec(),
            )).togetherWith(fadeOut(expressiveMotion.defaultEffectsSpec()))
        },
        label = "loginTransition",
    ) { isLoggedIn ->
        if (isLoggedIn) {
            val navController = rememberNavController()
            LaunchedEffect(openScheduleRequested, navController) {
                if (openScheduleRequested) {
                    analyticsLogger.logEvent(
                        AnalyticsEvents.SCHEDULE_OPEN,
                        mapOf(AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_WIDGET_SCHEDULE),
                    )
                    if (navController.currentDestination?.route != ScheduleRoute) {
                        navController.navigate(ScheduleRoute) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    onScheduleOpenHandled()
                }
            }
            NavHost(
                navController = navController,
                startDestination = GradesRoute,
                enterTransition = {
                    fadeIn(expressiveMotion.defaultEffectsSpec()) +
                        slideInHorizontally(expressiveMotion.defaultSpatialSpec()) { it / 8 }
                },
                exitTransition = {
                    fadeOut(expressiveMotion.defaultEffectsSpec()) +
                        slideOutHorizontally(expressiveMotion.defaultSpatialSpec()) { -it / 8 }
                },
                popEnterTransition = {
                    fadeIn(expressiveMotion.defaultEffectsSpec()) +
                        slideInHorizontally(expressiveMotion.defaultSpatialSpec()) { -it / 8 }
                },
                popExitTransition = {
                    fadeOut(expressiveMotion.defaultEffectsSpec()) +
                        slideOutHorizontally(expressiveMotion.defaultSpatialSpec()) { it / 8 }
                },
            ) {
                composable(GradesRoute) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        NotificationPromptDialog(
                            settings = settings,
                            onEnableNotifications = onSetNotificationsEnabled,
                            onOpenSettings = {
                                analyticsLogger.logEvent(
                                    AnalyticsEvents.NOTIFICATION_PROMPT_ACTION,
                                    mapOf(AnalyticsParams.ACTION to AnalyticsValues.ACTION_OPEN_SETTINGS),
                                )
                            },
                            onDismiss = onDismissNotificationPrompt,
                        )
                    }
                    GradesScreen(
                        state = gradesState,
                        settings = settings,
                        settingsUiState = settingsUiState,
                        isExporting = gradesState.isExporting,
                        exportResult = gradesState.exportResult,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        onSelectYear = onSelectYear,
                        onSelectExam = onSelectExam,
                        onReload = onReload,
                        onToggleSubject = onToggleSubject,
                        onStartGradeReminder = onStartGradeReminder,
                        onStopGradeReminder = onStopGradeReminder,
                        onSetNotificationsEnabled = onSetNotificationsEnabled,
                        onGradeReminderPrerequisiteFailed = onGradeReminderPrerequisiteFailed,
                        onDismissGradeReminderChanges = onDismissGradeReminderChanges,
                        onSetThemeMode = onSetThemeMode,
                        onSetDynamicColor = onSetDynamicColor,
                        onSetAmoledBlack = onSetAmoledBlack,
                        onCheckUpdate = onCheckUpdate,
                        onOpenSchoolWebsite = { navController.navigate(SchoolWebsiteRoute) },
                        onOpenSchoolAnnouncements = { navController.navigate(SchoolAnnouncementsRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onDismissDeveloperToast = onDismissDeveloperToast,
                        onOpenDeveloperSettings = {
                            analyticsLogger.logEvent(
                                AnalyticsEvents.FEATURE_OPEN,
                                mapOf(AnalyticsParams.FEATURE to AnalyticsValues.FEATURE_SETTINGS),
                            )
                            navController.navigate(DeveloperSettingsRoute)
                        },
                        onExportGrades = onExportGrades,
                        onDismissExportResult = onDismissExportResult,
                        onLogout = onLogout,
                        onSetBiometricEnabled = onSetBiometricEnabled,
                        onOpenScoreSimulator = {
                            analyticsLogger.logEvent(
                                AnalyticsEvents.SCORE_SIMULATOR_USED,
                                mapOf(
                                    AnalyticsParams.SUBJECT_COUNT_BUCKET to AnalyticsParameterSanitizer.countBucket(
                                        gradesState.report?.subjects.orEmpty().size,
                                    ),
                                ),
                            )
                            navController.navigate(ScoreSimulatorRoute)
                        },
                        onOpenSchedule = {
                            analyticsLogger.logEvent(
                                AnalyticsEvents.SCHEDULE_OPEN,
                                mapOf(AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_TAB),
                            )
                            navController.navigate(ScheduleRoute)
                        },
                        onOpenSchoolCalendar = {
                            navController.navigate(SchoolCalendarRoute)
                        },
                        onOpenSubjectTrend = {
                            analyticsLogger.logEvent(
                                AnalyticsEvents.FEATURE_OPEN,
                                mapOf(AnalyticsParams.FEATURE to AnalyticsValues.FEATURE_SUBJECT_TREND),
                            )
                            navController.navigate(SubjectTrendRoute)
                        },
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
                    LaunchedEffect(gradesState.structure) {
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
                        factory = ScheduleViewModel.factory(
                            context = context,
                            useFakeData = settings.demoMode,
                            activeSessionProvider = scoreViewModel::getCurrentSession,
                        ),
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(uiState.report) {
                        if (uiState.report != null) {
                            com.clhs.score.widget.syncAllScheduleWidgets(context)
                        }
                    }
                    com.clhs.score.ui.schedule.ScheduleScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onRefresh = { viewModel.refresh() },
                        onYearSelected = { viewModel.selectYear(it) },
                        onClassSelected = { viewModel.selectClass(it) },
                        onScopeSelected = { viewModel.selectScope(it) },
                        onConfirmSelection = { viewModel.confirmSelection() },
                        onClearSelection = { viewModel.clearSelection() },
                        onNoticeShown = { viewModel.consumeNotice() },
                    )
                }
                composable(SchoolCalendarRoute) {
                    val context = LocalContext.current
                    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SchoolCalendarViewModel>(
                        factory = SchoolCalendarViewModel.factory(context),
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    com.clhs.score.ui.calendar.SchoolCalendarScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onRefresh = viewModel::refresh,
                        onOpenGoogleCalendar = {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        SCHOOL_CALENDAR_WEB_URL.toUri(),
                                    ),
                                )
                            }.onFailure {
                                android.widget.Toast.makeText(
                                    context,
                                    "無法開啟 Google 行事曆",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onNoticeShown = viewModel::consumeNotice,
                    )
                }
                composable(SchoolAnnouncementsRoute) {
                    val context = LocalContext.current
                    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SchoolAnnouncementsViewModel>(
                        factory = SchoolAnnouncementsViewModel.factory(context),
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    com.clhs.score.ui.announcements.SchoolAnnouncementsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onRefresh = viewModel::refresh,
                        onLoadMore = viewModel::loadMore,
                        onOpenAnnouncement = { announcement ->
                            if (announcement.contentType.equals("url", ignoreCase = true)) {
                                announcement.externalUrl?.let(context::openAnnouncementUrl)
                                    ?: android.widget.Toast.makeText(
                                        context,
                                        "這則消息的連結無法開啟",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                            } else {
                                navController.navigate(
                                    "school-announcement/${android.net.Uri.encode(announcement.id)}" +
                                        "?category=${android.net.Uri.encode(announcement.category)}",
                                )
                            }
                        },
                        onOpenOfficialWebsite = {
                            context.openAnnouncementUrl(SCHOOL_ANNOUNCEMENTS_WEB_URL)
                        },
                        onNoticeShown = viewModel::consumeNotice,
                    )
                }
                composable(
                    route = SchoolAnnouncementDetailRoute,
                    arguments = listOf(
                        navArgument("announcementId") { type = NavType.StringType },
                        navArgument("category") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) { backStackEntry ->
                    val context = LocalContext.current
                    val announcementId = backStackEntry.arguments?.getString("announcementId")
                        ?: return@composable
                    val category = backStackEntry.arguments?.getString("category").orEmpty()
                    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SchoolAnnouncementDetailViewModel>(
                        key = "school-announcement-detail-$announcementId",
                        factory = SchoolAnnouncementDetailViewModel.factory(context, announcementId, category),
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    com.clhs.score.ui.announcements.SchoolAnnouncementDetailScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onRetry = viewModel::retry,
                        onOpenUrl = context::openAnnouncementUrl,
                        officialUrl = schoolAnnouncementOfficialUrl(announcementId),
                    )
                }
                composable(SchoolWebsiteRoute) {
                    val session = scoreViewModel.getCurrentSession() ?: return@composable
                    SchoolWebsiteScreen(
                        session = session,
                        onBack = { navController.popBackStack() },
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
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onDismissDeveloperToast = onDismissDeveloperToast,
                        onOpenDeveloperSettings = { navController.navigate(DeveloperSettingsRoute) },
                        onExportGrades = onExportGrades,
                        onDismissExportResult = onDismissExportResult,
                        onLogout = onLogout,
                        onSetBiometricEnabled = onSetBiometricEnabled,
                    )
                }
                composable(AboutRoute) {
                    AboutScreen(
                        settings = settings,
                        uiState = settingsUiState,
                        onBack = { navController.popBackStack() },
                        onVersionTap = onVersionTap,
                        onDismissDeveloperToast = onDismissDeveloperToast,
                        onOpenUsageStatistics = { navController.navigate(UsageStatisticsRoute) },
                        onOpenSourceLicenses = { navController.navigate(OpenSourceLicensesRoute) },
                    )
                }
                composable(UsageStatisticsRoute) {
                    val context = LocalContext.current
                    val statistics = remember(context) {
                        UsageStatisticsStore(context).snapshot()
                    }
                    UsageStatisticsScreen(
                        statistics = statistics,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(OpenSourceLicensesRoute) {
                    OpenSourceLicensesScreen(
                        onBack = { navController.popBackStack() },
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
            val loginNavController = rememberNavController()
            NavHost(
                navController = loginNavController,
                startDestination = IntroRoute,
                enterTransition = { fadeIn(standardMotion.defaultEffectsSpec()) },
                exitTransition = { fadeOut(standardMotion.defaultEffectsSpec()) },
                popEnterTransition = { fadeIn(standardMotion.defaultEffectsSpec()) },
                popExitTransition = { fadeOut(standardMotion.defaultEffectsSpec()) },
            ) {
                composable(IntroRoute) {
                    IntroScreen(
                        showSkipButton = settings.demoMode,
                        onSkipClick = { onWebViewLoginSuccess("DEMO-000", "fake=cookie") },
                        onLoginClick = { loginNavController.navigate(WebViewLoginRoute) },
                    )
                }
                composable(WebViewLoginRoute) {
                    WebViewLoginScreen(
                        isProcessingLogin = loginState.isWebViewLoginInProgress,
                        errorMessage = loginState.errorMessage,
                        onLoginSuccess = onWebViewLoginSuccess,
                        onBack = { loginNavController.popBackStack() },
                        onDismissError = onDismissLoginError,
                    )
                }
            }
        }
    }
}

private fun android.content.Context.openAnnouncementUrl(value: String) {
    val url = safeAnnouncementWebUrl(value)
    if (url == null) {
        android.widget.Toast.makeText(this, "這個連結無法開啟", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()))
    }.onFailure {
        android.widget.Toast.makeText(this, "無法開啟連結", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SystemNotificationPermissionSync(
    settings: AppSettings,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    analyticsLogger: AnalyticsLogger,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationsEnabled by rememberUpdatedState(settings.notificationsEnabled)
    val currentOnSetNotificationsEnabled by rememberUpdatedState(onSetNotificationsEnabled)

    fun syncIfNeeded() {
        if (notificationsEnabled && !context.canPostNotifications()) {
            analyticsLogger.logEvent(
                AnalyticsEvents.NOTIFICATION_PROMPT_ACTION,
                mapOf(AnalyticsParams.ACTION to AnalyticsValues.ACTION_AUTO_DISABLED),
            )
            currentOnSetNotificationsEnabled(false)
        }
    }

    LaunchedEffect(settings.notificationsEnabled) {
        syncIfNeeded()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
