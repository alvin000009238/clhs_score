package com.clhs.score.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clhs.score.data.AppSettings
import com.clhs.score.data.ExamSelection
import com.clhs.score.data.ExamSummary
import com.clhs.score.data.GradeAnalysis
import com.clhs.score.ui.theme.OutfitFontFamily
import com.clhs.score.data.GradeChangeSet
import com.clhs.score.data.GradeReminderState
import com.clhs.score.data.GradeReminderText
import com.clhs.score.data.GradeReport
import com.clhs.score.data.GradeTrend
import com.clhs.score.data.ScoreInsightSet
import com.clhs.score.data.StudentInfo
import com.clhs.score.data.SubjectAnalysis
import com.clhs.score.data.SubjectScore
import com.clhs.score.data.ThemeMode
import com.clhs.score.data.YearTermOption
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.parseYearTerm
import com.clhs.score.data.shortenSubjectName
import com.clhs.score.reminders.BatteryOptimizationHelper
import com.clhs.score.ui.theme.ScoreTheme
import com.clhs.score.viewmodel.GradesUiState
import com.clhs.score.viewmodel.SettingsUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val TAB_SLIDE_DURATION_MILLIS = 220

internal fun pagerNeedsSettling(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    destination: Int,
): Boolean = currentPage != destination || currentPageOffsetFraction != 0f

internal enum class GradesAdaptiveLayout {
    SingleColumn,
    TwoColumn,
    ListDetail,
}

internal fun gradesAdaptiveLayoutForWidth(width: Dp): GradesAdaptiveLayout = when {
    width < 600.dp -> GradesAdaptiveLayout.SingleColumn
    width < 840.dp -> GradesAdaptiveLayout.TwoColumn
    else -> GradesAdaptiveLayout.ListDetail
}

private enum class GradesDestination(
    val label: String,
    val icon: String,
) {
    Overview("總覽", "home"),
    Subjects("科目", "newsstand"),
    Advanced("更多", "more_horiz"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GradesScreen(
    state: GradesUiState,
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    isExporting: Boolean,
    exportResult: String?,
    snackbarHost: @Composable () -> Unit,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
    onReload: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onStartGradeReminder: () -> Unit,
    onStopGradeReminder: () -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onGradeReminderPrerequisiteFailed: (String) -> Unit,
    onDismissGradeReminderChanges: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onExportGrades: (List<ExamSelection>) -> Unit,
    onDismissExportResult: () -> Unit,
    onLogout: () -> Unit,
    onSetBiometricEnabled: (Boolean, String?) -> Unit,
    onOpenScoreSimulator: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenSubjectTrend: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStartGradeReminder by rememberUpdatedState(onStartGradeReminder)
    val currentOnSetNotificationsEnabled by rememberUpdatedState(onSetNotificationsEnabled)
    val currentOnGradeReminderPrerequisiteFailed by rememberUpdatedState(onGradeReminderPrerequisiteFailed)
    var selectedDestination by rememberSaveable { mutableIntStateOf(GradesDestination.Overview.ordinal) }
    val waitingForNotificationGrant = rememberSaveable { mutableStateOf(false) }
    val waitingForBatteryOptimizationGrant = rememberSaveable { mutableStateOf(false) }
    var isNotificationPermissionGranted by remember { mutableStateOf(context.arePostNotificationsGranted()) }
    var isBatteryOptimizationIgnored by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    var showStudentSheet by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = GradesDestination.Overview.ordinal,
        pageCount = { GradesDestination.entries.size },
    )
    val isRefreshing = state.isLoadingStructure || state.isLoadingGrades
    val pullToRefreshState = rememberPullToRefreshState()
    val topControlsContentPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val overviewScrollState = rememberScrollState()
    val subjectsScrollState = rememberScrollState()
    val advancedScrollState = rememberScrollState()

    LaunchedEffect(selectedDestination) {
        if (pagerNeedsSettling(pagerState.currentPage, pagerState.currentPageOffsetFraction, selectedDestination)) {
            pagerState.animateScrollToPage(selectedDestination)
        }
    }

    fun refreshReminderPrerequisites() {
        isNotificationPermissionGranted = context.arePostNotificationsGranted()
        isBatteryOptimizationIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    fun requestBatteryOptimizationOrStart() {
        refreshReminderPrerequisites()
        if (isBatteryOptimizationIgnored) {
            currentOnStartGradeReminder()
            return
        }
        val activity = context.findActivity()
        if (activity == null) {
            currentOnGradeReminderPrerequisiteFailed("需要開啟電池最佳化設定，才能準時提醒你。")
            return
        }
        waitingForBatteryOptimizationGrant.value = true
        if (!BatteryOptimizationHelper.openBatteryOptimizationRequest(activity)) {
            waitingForBatteryOptimizationGrant.value = false
            currentOnGradeReminderPrerequisiteFailed("需要開啟電池最佳化設定，才能準時提醒你。")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver observer@{ _, event ->
            if (event != Lifecycle.Event.ON_RESUME) {
                return@observer
            }
            val notificationGranted = context.arePostNotificationsGranted()
            val batteryOptimizationIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            isNotificationPermissionGranted = notificationGranted
            isBatteryOptimizationIgnored = batteryOptimizationIgnored
            if (waitingForNotificationGrant.value) {
                waitingForNotificationGrant.value = false
                if (notificationGranted) {
                    currentOnSetNotificationsEnabled(true)
                    requestBatteryOptimizationOrStart()
                } else {
                    currentOnGradeReminderPrerequisiteFailed("未取得通知權限，無法啟用段考提醒")
                }
                return@observer
            }
            if (waitingForBatteryOptimizationGrant.value) {
                waitingForBatteryOptimizationGrant.value = false
                if (batteryOptimizationIgnored) {
                    currentOnStartGradeReminder()
                } else {
                    currentOnGradeReminderPrerequisiteFailed("需要開啟電池最佳化設定，才能準時提醒你。")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun beginGradeReminderEnablement() {
        if (!context.arePostNotificationsGranted()) {
            waitingForNotificationGrant.value = true
            if (!context.openAppNotificationSettings()) {
                waitingForNotificationGrant.value = false
                currentOnGradeReminderPrerequisiteFailed("無法開啟通知設定，請手動到系統設定開啟")
            }
        } else {
            currentOnSetNotificationsEnabled(true)
            requestBatteryOptimizationOrStart()
        }
    }
    state.gradeReminderChangeSet?.let { changeSet ->
        GradeReminderChangeDialog(
            changeSet = changeSet,
            onDismiss = onDismissGradeReminderChanges,
        )
    }

    if (showStudentSheet) {
        StudentInfoBottomSheet(
            state = state,
            onDismiss = { showStudentSheet = false },
            onLogout = {
                showStudentSheet = false
                onLogout()
            },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerState = drawerState,
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                GradesNavigationDrawerContent(
                    state = state,
                    settings = settings,
                    settingsUiState = settingsUiState,
                    isExporting = isExporting,
                    exportResult = exportResult,
                    onSetThemeMode = onSetThemeMode,
                    onSetDynamicColor = onSetDynamicColor,
                    onSetAmoledBlack = onSetAmoledBlack,
                    onSetNotificationsEnabled = onSetNotificationsEnabled,
                    onCheckUpdate = onCheckUpdate,
                    onOpenAbout = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenAbout()
                    },
                    onDismissDeveloperToast = onDismissDeveloperToast,
                    onOpenDeveloperSettings = {
                        coroutineScope.launch { drawerState.close() }
                        onOpenDeveloperSettings()
                    },
                    onExportGrades = onExportGrades,
                    onDismissExportResult = onDismissExportResult,
                    onLogout = {
                        coroutineScope.launch { drawerState.close() }
                        onLogout()
                    },
                    onSetBiometricEnabled = onSetBiometricEnabled,
                )
            }
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val adaptiveLayout = gradesAdaptiveLayoutForWidth(maxWidth)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = snackbarHost,
                bottomBar = {
                    if (adaptiveLayout == GradesAdaptiveLayout.SingleColumn) {
                        GradesBottomNavigation(
                            selectedDestination = selectedDestination,
                            onSelect = { selectedDestination = it.ordinal },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            ) { padding ->
                Row(modifier = Modifier.fillMaxSize()) {
                    GradesAdaptiveNavigation(
                        layout = adaptiveLayout,
                        selectedDestination = selectedDestination,
                        onSelect = { selectedDestination = it.ordinal },
                    )
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onReload,
                        state = pullToRefreshState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(bottom = padding.calculateBottomPadding()),
                        indicator = {},
                    ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        val report = state.report
                        val analysis = state.analysis
                        when {
                            report == null && (state.isLoadingStructure || state.isLoadingGrades) -> GradesTabPage(
                                scrollState = overviewScrollState,
                                topPadding = topControlsContentPadding,
                            ) {
                                OverviewSkeleton()
                            }
                            report == null -> GradesTabPage(
                                scrollState = overviewScrollState,
                                topPadding = topControlsContentPadding,
                            ) {
                                EmptyPanel(
                                    message = if (state.structure.isEmpty()) "尚未取得可查詢考試" else "請選擇考試",
                                    onReload = onReload,
                                )
                            }
                            analysis == null -> GradesTabPage(
                                scrollState = overviewScrollState,
                                topPadding = topControlsContentPadding,
                            ) {
                                OverviewSkeleton()
                            }
                            else -> HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = false,
                            ) { destination ->
                                val tabScrollState = when (destination) {
                                    GradesDestination.Overview.ordinal -> overviewScrollState
                                    GradesDestination.Subjects.ordinal -> subjectsScrollState
                                    else -> advancedScrollState
                                }
                                GradesTabPage(
                                    scrollState = tabScrollState,
                                    topPadding = topControlsContentPadding,
                                ) {
                                    when (destination) {
                                        GradesDestination.Overview.ordinal -> OverviewTab(
                                            report = report,
                                            analysis = analysis,
                                            isLoadingComparison = state.isLoadingComparison,
                                            comparisonError = state.comparisonError,
                                            isLoadingTrend = state.isLoadingTrend,
                                            trendError = state.trendError,
                                            trend = state.trend,
                                            insights = state.insights,
                                            gradeReminderState = state.gradeReminderState,
                                            studentNo = state.studentNo,
                                            selectedYearValue = state.selectedYearValue,
                                            selectedExamValue = state.selectedExamValue,
                                            isStartingGradeReminder = state.isStartingGradeReminder,
                                            isNotificationPermissionGranted = isNotificationPermissionGranted,
                                            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                                            onStartGradeReminder = { beginGradeReminderEnablement() },
                                            onStopGradeReminder = onStopGradeReminder,
                                        )
                                        GradesDestination.Subjects.ordinal -> SubjectsTab(
                                            analyses = analysis.subjects,
                                            expandedSubjectKeys = state.expandedSubjectKeys,
                                            onToggleSubject = onToggleSubject,
                                        )
                                        GradesDestination.Advanced.ordinal -> AdvancedTab(
                                            report = report,
                                            analysis = analysis,
                                            isLoadingTrend = state.isLoadingTrend,
                                            trendError = state.trendError,
                                            isLoadingSimulatorHistory = state.isLoadingSimulatorHistory,
                                            simulatorHistoryLabel = state.simulatorHistoryLabel,
                                            simulatorHistoryCount = state.simulatorHistoryReports.size,
                                            trend = state.trend,
                                            onOpenScoreSimulator = onOpenScoreSimulator,
                                            onOpenSchedule = onOpenSchedule,
                                            onOpenSubjectTrend = onOpenSubjectTrend,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TopFadeOverlay(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .zIndex(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                shapes = IconButtonDefaults.shapes(
                                    shape = CircleShape,
                                    pressedShape = CircleShape,
                                ),
                            ) {
                                OutlinedRoundedSymbol(
                                    icon = "menu",
                                    contentDescription = "選單",
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        GradeSelectionPill(
                            state = state,
                            onSelectYear = onSelectYear,
                            onSelectExam = onSelectExam,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                        ) {
                            IconButton(
                                onClick = { showStudentSheet = true },
                                shapes = IconButtonDefaults.shapes(
                                    shape = CircleShape,
                                    pressedShape = CircleShape,
                                ),
                            ) {
                                OutlinedRoundedSymbol(
                                    icon = "account_circle",
                                    contentDescription = "帳號",
                                )
                            }
                        }
                    }
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .zIndex(2f),
                    )
                }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradesTabPage(
    scrollState: ScrollState,
    topPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding + 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GradesAdaptiveNavigation(
    layout: GradesAdaptiveLayout,
    selectedDestination: Int,
    onSelect: (GradesDestination) -> Unit,
) {
    when (layout) {
        GradesAdaptiveLayout.SingleColumn -> Unit
        GradesAdaptiveLayout.TwoColumn,
        GradesAdaptiveLayout.ListDetail,
        -> {
            val railExpanded = layout == GradesAdaptiveLayout.ListDetail
            key(layout) {
                val railState = rememberWideNavigationRailState(
                    initialValue = if (railExpanded) {
                        WideNavigationRailValue.Expanded
                    } else {
                        WideNavigationRailValue.Collapsed
                    },
                )
                WideNavigationRail(
                    state = railState,
                    modifier = Modifier.fillMaxHeight(),
                    arrangement = Arrangement.Center,
                ) {
                    GradesDestination.entries.forEach { destination ->
                        val selected = selectedDestination == destination.ordinal
                        WideNavigationRailItem(
                            selected = selected,
                            onClick = { onSelect(destination) },
                            railExpanded = railExpanded,
                            icon = {
                                if (selected) {
                                    FilledRoundedSymbol(
                                        icon = destination.icon,
                                        contentDescription = null,
                                    )
                                } else {
                                    OutlinedRoundedSymbol(
                                        icon = destination.icon,
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradesBottomNavigation(
    selectedDestination: Int,
    onSelect: (GradesDestination) -> Unit,
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        GradesDestination.entries.forEach { destination ->
            val selected = selectedDestination == destination.ordinal
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    if (selected) {
                        FilledRoundedSymbol(
                            icon = destination.icon,
                            contentDescription = destination.label,
                        )
                    } else {
                        OutlinedRoundedSymbol(
                            icon = destination.icon,
                            contentDescription = destination.label,
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GradeSelectionPill(
    state: GradesUiState,
    onSelectYear: (String) -> Unit,
    onSelectExam: (String) -> Unit,
) {
    val selectedYear = state.structure.find { it.value == state.selectedYearValue }
    val selectedYearLabel = selectedYear?.let(::compactYearTermLabel)
    val enabled = state.structure.isNotEmpty() && !state.isLoadingStructure
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var examMenuExpanded by remember { mutableStateOf(false) }
    val buttonContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
    val buttonContentColor = MaterialTheme.colorScheme.onSurface
    val buttonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = buttonContainerColor,
        contentColor = buttonContentColor,
        disabledContainerColor = buttonContainerColor.copy(alpha = 0.38f),
        disabledContentColor = buttonContentColor.copy(alpha = 0.42f),
    )

    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    modifier = Modifier.height(48.dp),
                    enabled = enabled,
                    colors = buttonColors,
                    onClick = {
                        yearMenuExpanded = true
                        examMenuExpanded = false
                    },
                ) {
                    Text(
                        text = selectedYearLabel ?: if (state.isLoadingStructure) "載入中" else "選擇成績",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TonalTrailingButton(
                    modifier = Modifier.height(48.dp),
                    checked = examMenuExpanded,
                    enabled = enabled,
                    colors = buttonColors,
                    onCheckedChange = { checked ->
                        examMenuExpanded = checked
                        if (checked) {
                            yearMenuExpanded = false
                        }
                    },
                ) {
                    OutlinedRoundedSymbol(
                        icon = if (examMenuExpanded) "keyboard_arrow_up" else "keyboard_arrow_down",
                        tint = buttonContentColor.copy(alpha = if (enabled) 1f else 0.42f),
                        contentDescription = "選擇考試",
                    )
                }
            },
        )

        DropdownMenu(
            expanded = yearMenuExpanded,
            onDismissRequest = { yearMenuExpanded = false },
            offset = DpOffset(x = 0.dp, y = 8.dp),
        ) {
            state.structure.forEach { year ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = compactYearTermLabel(year),
                            fontWeight = if (year.value == state.selectedYearValue) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    trailingIcon = {
                        if (year.value == state.selectedYearValue) {
                            FilledRoundedSymbol(icon = "check", contentDescription = null)
                        }
                    },
                    onClick = {
                        yearMenuExpanded = false
                        if (year.value != state.selectedYearValue) {
                            onSelectYear(year.value)
                        }
                    },
                )
            }
        }

        DropdownMenu(
            expanded = examMenuExpanded,
            onDismissRequest = { examMenuExpanded = false },
            offset = DpOffset(x = 0.dp, y = 8.dp),
        ) {
            if (selectedYear == null || selectedYear.exams.isEmpty()) {
                DropdownMenuItem(
                    enabled = false,
                    text = { Text("此學期沒有考試資料") },
                    onClick = {},
                )
            } else {
                selectedYear.exams.forEach { exam ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = exam.text,
                                fontWeight = if (exam.value == state.selectedExamValue) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            if (exam.value == state.selectedExamValue) {
                                FilledRoundedSymbol(icon = "check", contentDescription = null)
                            }
                        },
                        onClick = {
                            examMenuExpanded = false
                            onSelectExam(exam.value)
                        },
                    )
                }
            }
        }
    }
}

private fun compactYearTermLabel(yearTerm: YearTermOption): String {
    val (year, term) = parseYearTerm(yearTerm.value, defaultYear = "", defaultTerm = "")
    val termLabel = when (term) {
        "1" -> "上"
        "2" -> "下"
        else -> term
    }
    return when {
        year.isNotBlank() && termLabel.isNotBlank() -> "$year-$termLabel"
        year.isNotBlank() -> year
        else -> yearTerm.text
    }
}

@Composable
private fun OverviewTab(
    report: GradeReport,
    analysis: GradeAnalysis,
    isLoadingComparison: Boolean,
    comparisonError: String?,
    isLoadingTrend: Boolean,
    trendError: String?,
    trend: GradeTrend?,
    insights: ScoreInsightSet?,
    gradeReminderState: GradeReminderState,
    studentNo: String,
    selectedYearValue: String?,
    selectedExamValue: String?,
    isStartingGradeReminder: Boolean,
    isNotificationPermissionGranted: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onStartGradeReminder: () -> Unit,
    onStopGradeReminder: () -> Unit,
) {
    var showGradeReminderDetails by rememberSaveable { mutableStateOf(false) }
    if (showGradeReminderDetails) {
        GradeReminderDetailsDialog(
            reminderState = gradeReminderState,
            studentNo = studentNo,
            selectedYearValue = selectedYearValue,
            selectedExamValue = selectedExamValue,
            isStarting = isStartingGradeReminder,
            isNotificationPermissionGranted = isNotificationPermissionGranted,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            onStart = {
                showGradeReminderDetails = false
                onStartGradeReminder()
            },
            onStop = {
                showGradeReminderDetails = false
                onStopGradeReminder()
            },
            onDismiss = { showGradeReminderDetails = false },
        )
    }

    val summaryContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val isReminderActiveForSelection = gradeReminderState.isActiveFor(
                studentNo = studentNo,
                yearValue = selectedYearValue,
                examValue = selectedExamValue,
            )
            HeroCard(
                report = report,
                analysis = analysis,
                isGradeReminderActive = isReminderActiveForSelection,
                isStartingGradeReminder = isStartingGradeReminder,
                onOpenGradeReminder = { showGradeReminderDetails = true },
            )
            HeroChipRow(report, analysis)
            StrengthWeaknessCard(analysis)
        }
    }
    val detailContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            InsightCard(
                analysis = analysis,
                isLoadingComparison = isLoadingComparison,
                comparisonError = comparisonError,
                isLoadingTrend = isLoadingTrend,
                trendError = trendError,
                trend = trend,
                insights = insights,
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                summaryContent()
                detailContent()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    summaryContent()
                }
                Column(modifier = Modifier.weight(1f)) {
                    detailContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GradeReminderDetailsDialog(
    reminderState: GradeReminderState,
    studentNo: String,
    selectedYearValue: String?,
    selectedExamValue: String?,
    isStarting: Boolean,
    isNotificationPermissionGranted: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val isActiveForSelection = reminderState.isActiveFor(
        studentNo = studentNo,
        yearValue = selectedYearValue,
        examValue = selectedExamValue,
        nowMillis = now,
    )
    val lastCheckedText = reminderState.lastCheckedAtMillis?.let { "上次檢查時間 ${formatReminderTime(it)}" }
        ?: "尚未檢查"
    val otherExamName = reminderState.examName.ifBlank { "段考" }
    val primaryActionLabel = when {
        isActiveForSelection -> "停止"
        isStarting -> "啟用中..."
        !isNotificationPermissionGranted -> "開啟通知設定"
        isBatteryOptimizationIgnored -> "開始"
        else -> "開啟電池設定"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("段考更新提醒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "新成績、排名、五標等會在有變動時通知你。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    isActiveForSelection -> {
                        InlineStatus(message = "正在監控這次考試，每 15 分鐘會檢查一次更新")
                        Text(
                            text = "${formatRemaining(reminderState.expiresAtMillis, now)}後自動停止 · $lastCheckedText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    reminderState.isActive(now) -> {
                        InlineStatus(message = "目前正在監控其他考試")
                        Text(
                            text = otherExamName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    !isNotificationPermissionGranted && !isBatteryOptimizationIgnored -> {
                        InlineStatus(message = "需要開啟通知和電池最佳化設定")
                        Text(
                            text = "通知用來提醒你；電池最佳化設定能讓 app 在背景檢查更新。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    !isNotificationPermissionGranted -> {
                        InlineStatus(message = "需要開啟通知")
                        Text(
                            text = "允許 app 傳送通知，成績資訊更新時才能提醒你。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    !isBatteryOptimizationIgnored -> {
                        InlineStatus(message = "需要開啟電池最佳化設定")
                        Text(
                            text = "允許 app 不受電池最佳化限制，才能在背景檢查更新。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        InlineStatus(message = "點擊開始以繼續")
                    }
                }
            }
        },
        confirmButton = {
            if (isActiveForSelection) {
                TextButton(
                    enabled = !isStarting,
                    onClick = onStop,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(primaryActionLabel)
                }
            } else {
                TextButton(
                    enabled = !isStarting,
                    onClick = onStart,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(primaryActionLabel)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("關閉")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GradeReminderChangeDialog(
    changeSet: GradeChangeSet,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("成績資訊已更新") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = changeSet.examName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                GradeReminderText.detailLines(changeSet).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("知道了")
            }
        },
    )
}

@Composable
private fun AdvancedTab(
    report: GradeReport,
    analysis: GradeAnalysis,
    isLoadingTrend: Boolean,
    trendError: String?,
    isLoadingSimulatorHistory: Boolean,
    simulatorHistoryLabel: String?,
    simulatorHistoryCount: Int,
    trend: GradeTrend?,
    onOpenScoreSimulator: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenSubjectTrend: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val actions: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ScoreSimulatorEntryCard(
                    report = report,
                    analysis = analysis,
                    isLoadingHistory = isLoadingSimulatorHistory,
                    historyLabel = simulatorHistoryLabel,
                    historyCount = simulatorHistoryCount,
                    onOpen = onOpenScoreSimulator,
                )
                SubjectTrendEntryCard(onOpen = onOpenSubjectTrend)
                ScheduleEntryCard(onOpen = onOpenSchedule)
            }
        }

        if (maxWidth < 640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                TrendChart(
                    isLoadingTrend = isLoadingTrend,
                    trendError = trendError,
                    trend = trend,
                )
                actions()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    TrendChart(
                        isLoadingTrend = isLoadingTrend,
                        trendError = trendError,
                        trend = trend,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    actions()
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    report: GradeReport,
    analysis: GradeAnalysis,
    isGradeReminderActive: Boolean,
    isStartingGradeReminder: Boolean,
    onOpenGradeReminder: () -> Unit,
) {
    val student = report.studentInfo
    val summary = report.examSummary
    val animatedAverage by animateFloatAsState(
        targetValue = analysis.weightedAverage.toFloat(),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "weightedAverage",
    )
    val totalScore = summary?.totalScoreDisplay?.toDoubleOrNull()?.let { "%.0f".format(it) }
        ?: summary?.totalScoreDisplay?.takeIf { it.isNotBlank() }
        ?: "--"
    val rankLine = heroRankLine(summary, student)
    val showActiveReminderIcon = isGradeReminderActive || isStartingGradeReminder
    val reminderIconDescription = when {
        isStartingGradeReminder -> "段考提醒啟用中"
        isGradeReminderActive -> "段考提醒監控中"
        else -> "段考提醒"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "加權平均",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "%.1f".format(animatedAverage),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                        )
                        val deltaText = heroAverageDeltaTextShort(analysis)
                        if (deltaText != null) {
                            val deltaColor = diffColor(analysis.comparison?.averageDelta ?: 0.0)
                            Surface(
                                shape = CircleShape,
                                color = deltaColor.copy(alpha = 0.15f),
                                modifier = Modifier.padding(bottom = 4.dp),
                            ) {
                                Text(
                                    text = deltaText,
                                    color = deltaColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onOpenGradeReminder,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    if (showActiveReminderIcon) {
                        FilledRoundedSymbol(
                            icon = "notifications_active",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentDescription = reminderIconDescription,
                        )
                    } else {
                        OutlinedRoundedSymbol(
                            icon = "notifications",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f),
                            contentDescription = reminderIconDescription,
                        )
                    }
                }
            }
            Text(
                text = "總分 $totalScore",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = rankLine,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun HeroChip(
    text: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HeroChipRow(report: GradeReport, analysis: GradeAnalysis) {
    val summary = report.examSummary
    val examName = summary?.examName?.takeIf { it.isNotBlank() } ?: "本次考試"

    val classPercent = analysis.classPercentile?.topPercent
    val categoryPercent = analysis.categoryPercentile?.topPercent

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. 考試名稱
        HeroChip(
            text = examName,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 2. 班級百分位
        if (classPercent != null) {
            HeroChip(
                text = "班級前 $classPercent%",
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 3. 類組百分位
        if (categoryPercent != null) {
            HeroChip(
                text = "類組前 $categoryPercent%",
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun heroRankLine(summary: ExamSummary?, student: StudentInfo): String {
    val classRank = formatRank(summary?.classRank, summary?.classCount, student.showClassRankCount)
    val categoryRank = formatRank(summary?.categoryRank, summary?.categoryRankCount, student.showCategoryRankCount)
    return "班排 ${if (student.showClassRank) classRank else "--"} ・ 類排 ${if (student.showCategoryRank) categoryRank else "--"}"
}

private fun heroAverageDeltaTextShort(analysis: GradeAnalysis): String? {
    val delta = analysis.comparison?.averageDelta ?: return null
    return when {
        delta > 0.05 -> "↑ +${"%.1f".format(delta)}"
        delta < -0.05 -> "↓ ${"%.1f".format(delta)}"
        else -> "→ 持平"
    }
}

@Composable
private fun InsightCard(
    analysis: GradeAnalysis,
    isLoadingComparison: Boolean,
    comparisonError: String?,
    isLoadingTrend: Boolean,
    trendError: String?,
    trend: GradeTrend?,
    insights: ScoreInsightSet?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("分析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            DashboardInsightRow(
                label = "需要注意",
                text = riskInsightText(analysis, insights),
                color = ScoreTheme.semanticColors.negative,
            )
            DashboardInsightRow(
                label = "優勢",
                text = advantageInsightText(analysis),
                color = ScoreTheme.semanticColors.positive,
            )
            DashboardInsightRow(
                label = "ROI",
                text = roiInsightText(analysis, insights),
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                isLoadingComparison -> InlineStatus("正在載入上一考比較...")
                analysis.comparison != null -> InlineStatus("${analysis.comparison.previousExamName}：${analysis.comparison.summaryText}")
                comparisonError != null -> InlineStatus(comparisonError)
            }
            when {
                isLoadingTrend -> InlineStatus("正在載入歷次趨勢...")
                trend != null && trend.points.size >= 2 -> InlineStatus("近 ${trend.points.size} 次平均：${trend.averageLine}")
                trendError != null -> InlineStatus(trendError)
            }
        }
    }
}

@Composable
private fun DashboardInsightRow(label: String, text: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun riskInsightText(analysis: GradeAnalysis, insights: ScoreInsightSet?): String {
    insights?.projection?.let { projection ->
        val subject = shortenSubjectName(projection.subjectName)
        return "$subject 目前為主要拉低科目，若提升至班均，加權平均約可提升 ${"%.1f".format(projection.weightedAverageGain)}。"
    }
    val weakness = analysis.weaknesses.firstOrNull()
    return weakness?.let { "${shortenSubjectName(it.subjectName)} 低於班平均 ${"%.1f".format(abs(it.diffValue))} 分，建議優先處理。" }
        ?: "目前沒有明顯拉低科目，需要注意的科目集中度低。"
}

private fun advantageInsightText(analysis: GradeAnalysis): String {
    val strength = analysis.strengths.firstOrNull()
    return strength?.let {
        "${shortenSubjectName(it.subjectName)} ${subjectPercentLabel(it.classRank, it.classRankCount)}，高於班平均 ${"%.1f".format(it.diffValue)} 分，建議維持目前節奏。"
    } ?: "尚無明顯優勢科目，先把各科穩定在班平均附近。"
}

private fun roiInsightText(analysis: GradeAnalysis, insights: ScoreInsightSet?): String {
    val projection = insights?.projection
    return projection?.let {
        "目前投入效益最高科目為 ${shortenSubjectName(it.subjectName)}，每次小幅提升會直接推動加權平均。"
    } ?: analysis.weaknesses.firstOrNull()?.let {
        "目前投入效益最高科目為 ${shortenSubjectName(it.subjectName)}。"
    } ?: "目前各科差距接近，ROI 最高的方向是維持弱科不下滑。"
}

@Composable
private fun StrengthWeaknessCard(analysis: GradeAnalysis) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("摘要", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        SubjectHighlightRow(title = "優勢科目", subjects = analysis.strengths, color = ScoreTheme.semanticColors.positive, emptyText = "尚無明顯高於平均的科目")
        SubjectHighlightRow(title = "待加強科目", subjects = analysis.weaknesses, color = ScoreTheme.semanticColors.negative, emptyText = "尚無明顯低於平均的科目")
    }
}

@Composable
private fun SubjectHighlightRow(title: String, subjects: List<SubjectScore>, color: Color, emptyText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (subjects.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                subjects.forEach { subject ->
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .background(color.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = shortenSubjectName(subject.subjectName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = diffSentence(subject.diffValue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = subjectPercentLabel(subject.classRank, subject.classRankCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectsTab(
    analyses: List<SubjectAnalysis>,
    expandedSubjectKeys: Set<String>,
    onToggleSubject: (String) -> Unit,
) {
    var pendingBringIntoViewKey by remember { mutableStateOf<String?>(null) }

    Column {
        analyses.forEach { analysis ->
            val subjectKey = cleanSubjectName(analysis.subject.subjectName)
            val expanded = subjectKey in expandedSubjectKeys
            SubjectCard(
                analysis = analysis,
                expanded = expanded,
                bringIntoViewOnExpand = pendingBringIntoViewKey == subjectKey,
                onBringIntoViewHandled = {
                    if (pendingBringIntoViewKey == subjectKey) {
                        pendingBringIntoViewKey = null
                    }
                },
                onToggle = {
                    pendingBringIntoViewKey = if (expanded) null else subjectKey
                    onToggleSubject(analysis.subject.subjectName)
                },
            )
        }
    }
}

@Composable
private fun InlineStatus(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatReminderTime(timeMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))

private fun formatRemaining(expiresAtMillis: Long, nowMillis: Long): String {
    val remainingMillis = (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
    return when {
        hours > 0 -> "${hours}小時${minutes}分"
        minutes > 0 -> "${minutes}分"
        else -> "不到 1 分鐘"
    }
}

@Composable
private fun OverviewSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) {
            SkeletonBlock(height = if (it == 1) 190.dp else 96.dp)
        }
    }
}

@Composable
private fun SkeletonBlock(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
    )
}

@Composable
private fun EmptyPanel(message: String, onReload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                OutlinedRoundedSymbol(icon = "refresh", size = 40.dp, contentDescription = null)
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onReload,
            shapes = ButtonDefaults.shapes(),
        ) {
            Text("重新整理")
        }
    }
}

private fun signedValue(value: Double): String = "${if (value >= 0.0) "+" else ""}${"%.1f".format(value)}"

private fun Double?.formatCompactScore(): String = this?.let { "%.0f".format(it) } ?: "--"

private fun diffSentence(diff: Double): String = when {
    diff > 0.05 -> "高於平均 ${signedValue(diff)}"
    diff < -0.05 -> "低於平均 ${signedValue(diff)}"
    else -> "接近班級平均"
}

private fun trendGlyph(diff: Double?): String = when {
    diff == null -> "→"
    diff > 0.05 -> "↑"
    diff < -0.05 -> "↓"
    else -> "→"
}

@Composable
private fun diffColor(diff: Double): Color = when {
    diff > 0.05 -> ScoreTheme.semanticColors.positive
    diff < -0.05 -> ScoreTheme.semanticColors.negative
    else -> ScoreTheme.semanticColors.neutral
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun headerTitleText(state: GradesUiState): String {
    val student = state.report?.studentInfo
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "早安"
        in 12..17 -> "午安"
        else -> "晚安"
    }
    val name = student?.studentName?.takeIf { it.isNotBlank() }
    return if (name == null) greeting else "$greeting，$name"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentInfoBottomSheet(
    state: GradesUiState,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "帳號",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val student = state.report?.studentInfo
            if (student != null) {
                InfoRow(label = "姓名", value = student.studentName)
                InfoRow(label = "學號", value = student.studentNo)
                InfoRow(label = "班級", value = student.className)
                InfoRow(label = "座號", value = "${student.seatNo} 號")
                student.updatedAt.takeIf { it.isNotBlank() }?.let {
                    InfoRow(label = "資料更新時間", value = it)
                }
            } else {
                Text(
                    text = "尚未取得帳號資料，請先成功載入成績",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showLogoutDialog = true },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(
                    text = "登出",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun GradesNavigationDrawerContent(
    state: GradesUiState,
    settings: AppSettings,
    settingsUiState: SettingsUiState,
    isExporting: Boolean,
    exportResult: String?,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismissDeveloperToast: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onExportGrades: (List<ExamSelection>) -> Unit,
    onDismissExportResult: () -> Unit,
    onLogout: () -> Unit,
    onSetBiometricEnabled: (Boolean, String?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 72.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = headerTitleText(state),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "鍥而不舍，金石可鏤。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsContent(
                settings = settings,
                uiState = settingsUiState,
                structure = state.structure,
                isExporting = isExporting,
                exportResult = exportResult,
                onSetThemeMode = onSetThemeMode,
                onSetDynamicColor = onSetDynamicColor,
                onSetAmoledBlack = onSetAmoledBlack,
                onSetNotificationsEnabled = onSetNotificationsEnabled,
                onCheckUpdate = onCheckUpdate,
                onOpenAbout = onOpenAbout,
                onDismissDeveloperToast = onDismissDeveloperToast,
                onOpenDeveloperSettings = onOpenDeveloperSettings,
                onExportGrades = onExportGrades,
                onDismissExportResult = onDismissExportResult,
                onLogout = onLogout,
                onSetBiometricEnabled = onSetBiometricEnabled,
                modifier = Modifier.fillMaxWidth(),
                showLogout = false,
            )
        }

        TopFadeOverlay(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Text(
            text = "CLHS Pocket",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
        )
    }
}

@Composable
private fun TopFadeOverlay(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0f),
                    ),
                ),
            ),
    )
}
