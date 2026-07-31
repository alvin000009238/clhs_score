package com.clhs.score

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureBoundaryTest {
    @Test
    fun scheduleWidgetDoesNotDependOnAuthenticationState() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        assertTrue(source.contains("loadWidgetScheduleReport()"))

        val forbiddenTerms = listOf(
            "SessionStore",
            "loadSession(",
            "loadSessionWithPin",
            "loadBiometricSession",
            "saveSession(",
            "clearNormalSession",
            "clearBiometricSession",
            "apiToken",
            "cookies",
        )
        forbiddenTerms.forEach { term ->
            assertFalse("ScheduleWidget must not depend on authentication state: $term", source.contains(term))
        }
        assertTrue(source.contains("本週課表已過期"))
        assertTrue(source.contains("開啟 App 更新"))
    }

    @Test
    fun pinUnlockActivatesSessionBeforeReleasingLock() {
        val source = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")
        val pinUnlockBlock = source
            .substringAfter("onUnlockWithPin = { pin ->")
            .substringAfter("if (session != null) {")
            .substringBefore("if (isBiometricInvalidated.value)")

        val activateIndex = pinUnlockBlock.indexOf("scoreVm.loginWithBiometricSession(session)")
        val unlockIndex = pinUnlockBlock.indexOf("isAppLocked.value = false")

        assertTrue("PIN unlock path must activate the in-memory session", activateIndex >= 0)
        assertTrue("PIN unlock path must release the lock", unlockIndex >= 0)
        assertTrue("Session must be active before UI can render", activateIndex < unlockIndex)
    }

    @Test
    fun biometricPromptIsSingleFlightAndDoesNotCountAsBackgrounding() {
        val source = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")

        assertTrue(source.contains("private var isBiometricPromptShowing = false"))
        assertTrue(source.contains("if (wasInBackground && !isBiometricPromptShowing)"))
        assertTrue(source.contains("if (!isChangingConfigurations && !isBiometricPromptShowing)"))

        val unlockPromptBlock = source
            .substringAfter("private fun showBiometricUnlockPrompt(")
            .substringBefore("private fun showBiometricEnrollPrompt(")

        assertTrue(
            "Unlock prompt must ignore duplicate authenticate requests",
            unlockPromptBlock.contains("if (isBiometricPromptShowing) return"),
        )
        assertTrue(
            "Unlock prompt must mark the prompt as visible before authenticate",
            unlockPromptBlock.indexOf("isBiometricPromptShowing = true") <
                unlockPromptBlock.indexOf("biometricPrompt.authenticate"),
        )
        assertTrue(
            "Unlock prompt must clear prompt state on terminal callbacks",
            unlockPromptBlock.countOccurrences("isBiometricPromptShowing = false") >= 3,
        )
    }

    @Test
    fun singleTaskScheduleDeepLinkIsHeldUntilScheduleScreenCanOpen() {
        val activitySource = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")
        val appSource = readSource("app/src/main/java/com/clhs/score/ui/ScoreApp.kt")

        assertTrue(activitySource.contains("private val pendingScheduleOpen = mutableStateOf(false)"))
        assertTrue(activitySource.contains("data?.scheme == \"scoreapp\" && data.host == \"schedule\""))
        assertTrue(activitySource.contains("pendingScheduleOpen.value = true"))
        assertTrue(activitySource.contains("openScheduleRequested = pendingScheduleOpen.value"))
        assertTrue(activitySource.contains("onScheduleOpenHandled = { pendingScheduleOpen.value = false }"))
        assertTrue(appSource.contains("LaunchedEffect(openScheduleRequested, navController)"))
        assertTrue(appSource.contains("if (openScheduleRequested)"))
        assertTrue(appSource.contains("navController.navigate(ScheduleRoute)"))
        assertTrue(appSource.contains("onScheduleOpenHandled()"))
        assertFalse("Composable must not mutate Activity intent data", appSource.contains("intent?.data = null"))
    }

    @Test
    fun biometricLockStateSurvivesConfigurationChange() {
        val source = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")

        assertTrue(source.contains("override fun onSaveInstanceState(outState: Bundle)"))
        assertTrue(source.contains("outState.putBoolean(KEY_APP_LOCKED, isAppLocked.value)"))
        assertTrue(source.contains("savedInstanceState?.getBoolean(KEY_APP_LOCKED, false) == true"))
        assertTrue(source.contains("hasBiometricSession"))
        assertTrue(source.contains("isInitialLockResolved.value = !shouldLockOnInitialReady || isAppLocked.value"))
        assertTrue(source.contains("const val KEY_APP_LOCKED = \"app_locked\""))
    }

    @Test
    fun widgetAlarmUsesThePlatformWidgetBootLifecycle() {
        val manifest = readSource("app/src/main/AndroidManifest.xml")
        val widgetReceiver = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidgetReceiver.kt")

        assertFalse(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
        assertFalse(manifest.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(widgetReceiver.contains("override fun onEnabled(context: Context)"))
        assertTrue(widgetReceiver.contains("WidgetUpdateReceiver.scheduleNextUpdate(context)"))
    }

    @Test
    fun coroutineCancellationIsNotConvertedToUiOrWorkerErrors() {
        val scoreViewModel = readSource("app/src/main/java/com/clhs/score/viewmodel/ScoreViewModel.kt")
        val scheduleViewModel = readSource("app/src/main/java/com/clhs/score/viewmodel/ScheduleViewModel.kt")
        val schoolClient = readSource("app/src/main/java/com/clhs/score/data/SchoolGradeClient.kt")
        val reminderWorker = readSource("app/src/main/java/com/clhs/score/reminders/GradeReminderWorker.kt")
        val updateChecker = readSource("app/src/main/java/com/clhs/score/data/UpdateChecker.kt")

        assertTrue(scoreViewModel.contains("import kotlinx.coroutines.CancellationException"))
        assertTrue(scoreViewModel.countOccurrences("error.throwIfCancellation()") >= 7)
        assertTrue(scoreViewModel.contains("private fun Throwable.throwIfCancellation()"))

        assertTrue(scheduleViewModel.contains("import kotlinx.coroutines.CancellationException"))
        assertTrue(scheduleViewModel.countOccurrences("e.throwIfCancellation()") >= 4)
        assertTrue(scheduleViewModel.contains("private fun Throwable.throwIfCancellation()"))
        assertTrue(schoolClient.contains("error is CancellationException"))

        assertTrue(reminderWorker.contains("import kotlinx.coroutines.CancellationException"))
        assertTrue(reminderWorker.contains("if (error is CancellationException) throw error"))

        assertTrue(updateChecker.contains("catch (e: CancellationException)"))
        assertTrue(updateChecker.contains("throw e"))
    }

    @Test
    fun webViewLoginBridgeIsLimitedToTrustedSchoolLoginPage() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/WebViewLoginScreen.kt")

        assertTrue(source.contains("shouldOverrideUrlLoading"))
        assertTrue(source.contains("return !isTrustedSchoolUrl(url)"))
        assertTrue(source.contains("if (loginHandled || !isTrustedLoginPage)"))
        assertTrue(source.contains("isTrustedLoginPage = isTrustedSchoolLoginUrl(url)"))
        assertTrue(source.contains("uri.scheme == \"https\""))
        assertTrue(source.contains("SCHOOL_DOMAIN"))
    }

    @Test
    fun predictiveBackUsesPlatformNavigationOwners() {
        val manifest = readSource("app/src/main/AndroidManifest.xml")
        val appSource = readSource("app/src/main/java/com/clhs/score/ui/ScoreApp.kt")
        val loginSource = readSource("app/src/main/java/com/clhs/score/ui/WebViewLoginScreen.kt")
        val gradesSource = readSource("app/src/main/java/com/clhs/score/ui/GradesScreen.kt")
        val drawerSheet = gradesSource.substringAfter("ModalDrawerSheet(").substringBefore(") {")

        assertTrue(manifest.contains("android:enableOnBackInvokedCallback=\"true\""))
        assertTrue(appSource.contains("composable(WebViewLoginRoute)"))
        assertTrue(appSource.contains("onBack = { loginNavController.popBackStack() }"))
        assertFalse(loginSource.contains("BackHandler"))
        assertTrue(drawerSheet.contains("drawerState = drawerState"))
        assertFalse(gradesSource.contains("BackHandler"))
    }

    @Test
    fun scheduleErrorsExposeRefreshAction() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/schedule/ScheduleScreen.kt")

        assertTrue(source.contains("onClick = onRefresh"))
        assertTrue(source.contains("Text(\"重新整理\")"))
    }

    @Test
    fun currentWeekScheduleExposesForceReloadAction() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/schedule/ScheduleScreen.kt")

        assertTrue(source.contains("if (uiState.report?.scope == ScheduleScope.CURRENT_WEEK)"))
        assertTrue(source.contains("contentDescription = \"強制重新載入本週課表\""))
        assertTrue(source.contains("enabled = !uiState.isLoading"))
    }

    @Test
    fun subjectCardExpansionKeepsTheValidatedTweenTransition() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/SubjectComponents.kt")
        val visibilityTransition = source
            .substringAfter("AnimatedVisibility(")
            .substringBefore("            ) {")

        assertTrue(visibilityTransition.contains("durationMillis = 140"))
        assertTrue(visibilityTransition.contains("delayMillis = 40"))
        assertTrue(visibilityTransition.contains("durationMillis = 300"))
        assertTrue(visibilityTransition.contains("durationMillis = 90"))
        assertTrue(visibilityTransition.contains("durationMillis = 220"))
        assertTrue(visibilityTransition.contains("easing = LinearOutSlowInEasing"))
        assertTrue(visibilityTransition.countOccurrences("easing = FastOutSlowInEasing") == 3)
        assertFalse(visibilityTransition.contains("motion."))
    }

    @Test
    fun subjectTrendLegendSelectionCoversItsMinimumTouchTarget() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/SubjectTrendScreen.kt")
        val legendItem = source
            .substringAfter("groupedLegend.forEach")
            .substringBefore("val filtersSection")

        val toggleableIndex = legendItem.indexOf(".toggleable(")
        val backgroundIndex = legendItem.indexOf(".background(")
        val minimumSizeIndex = legendItem.indexOf(".minimumInteractiveComponentSize()")
        val paddingIndex = legendItem.indexOf(".padding(horizontal = 4.dp, vertical = 2.dp)")

        assertTrue(toggleableIndex >= 0)
        assertTrue(backgroundIndex > toggleableIndex)
        assertTrue(minimumSizeIndex > backgroundIndex)
        assertTrue(paddingIndex > minimumSizeIndex)
    }

    @Test
    fun updateDialogKeepsFullMarkdownScrollable() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/UpdateResultDialog.kt")

        assertTrue(source.contains("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("Markdown("))
        assertTrue(source.contains("content = result.releaseNotes"))
        assertFalse(source.contains("result.releaseNotes.take("))
    }

    @Test
    fun emptyScheduleReportShowsActionableState() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/schedule/ScheduleScreen.kt")

        assertTrue(source.contains("uiState.report.items.isEmpty()"))
        assertTrue(source.contains("\"查無課表資料\""))
        assertTrue(source.contains("onClick = onClearSelection"))
    }

    @Test
    fun widgetPreferencesAreStoredPerGlanceId() {
        val widgetSource = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")
        val appSource = readSource("app/src/main/java/com/clhs/score/ui/ScoreApp.kt")
        val scheduleSource = readSource("app/src/main/java/com/clhs/score/ui/schedule/ScheduleScreen.kt")

        assertTrue(widgetSource.contains("getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)"))
        assertTrue(widgetSource.contains("state[WidgetShowTeacherKey] = preferences.showTeacher"))
        assertTrue(widgetSource.contains("state[WidgetAfterLastClassKey] = preferences.afterLastClass"))
        assertFalse(widgetSource.contains("cacheStore.getWidgetPreferences()"))
        assertFalse(widgetSource.contains("cacheStore.getWidgetAfterLastClass()"))
        assertFalse(appSource.contains("WidgetSettingsRoute"))
        assertFalse(scheduleSource.contains("Widget 設定"))
    }

    @Test
    fun widgetConfigurationUsesTargetedGlanceUpdateWithoutBlockingComposition() {
        val activitySource = readSource("app/src/main/java/com/clhs/score/widget/WidgetConfigurationActivity.kt")
        val widgetSource = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        val setContentBlock = activitySource.substringAfter("setContent {")
        assertTrue("Widget configuration must use the Android SplashScreen API while loading settings", activitySource.contains("installSplashScreen()"))
        assertTrue(activitySource.contains("splashScreen.setKeepOnScreenCondition { launchConfiguration.value == null }"))
        assertTrue(activitySource.contains("splashScreen.setOnExitAnimationListener"))
        assertTrue(activitySource.contains("val iconView = runCatching { splashScreenView.iconView }.getOrNull()"))
        assertTrue(activitySource.contains("if (iconView == null)"))
        assertFalse(activitySource.contains("splashScreenView.iconView.animate()"))
        assertFalse("Splash exit must not fade the whole splash window over app content", activitySource.contains("splashScreenView.view.animate()"))
        assertTrue(activitySource.contains(".alpha(0f)"))
        assertTrue(activitySource.contains(".scaleX(0.96f)"))
        assertTrue(activitySource.contains(".scaleY(0.96f)"))
        assertTrue(activitySource.contains(".setDuration(200L)"))
        assertTrue(activitySource.contains("DecelerateInterpolator()"))
        assertTrue(activitySource.contains(".withEndAction { splashScreenView.remove() }"))
        assertFalse("Widget configuration must not block inside composition", setContentBlock.contains("runBlocking"))
        assertFalse("Widget configuration must not block the main thread while loading settings", activitySource.contains("runBlocking"))
        assertTrue(activitySource.contains("lifecycleScope.launch"))
        assertTrue(activitySource.contains("collectAsStateWithLifecycle"))
        assertFalse("Widget configuration must not render the first frame with a guessed default theme", activitySource.contains("initialValue = AppSettings()"))
        assertTrue(activitySource.contains("initialValue = configuration.settings"))
        assertTrue(activitySource.contains("loadScheduleWidgetPreferences(applicationContext, appWidgetId)"))
        assertTrue(activitySource.contains("saveScheduleWidgetPreferences("))
        assertTrue(activitySource.contains("syncScheduleWidget(applicationContext, appWidgetId)"))
        assertTrue(widgetSource.contains("getGlanceIdBy(appWidgetId)"))
        assertTrue(widgetSource.contains("ScheduleWidget().update(context, glanceId)"))
    }

    @Test
    fun widgetConfigurationCancellationKeepsTheWidgetIdResult() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/WidgetConfigurationActivity.kt")
        val dismissBlock = source.substringAfter("onDismiss = {").substringBefore("},")

        assertFalse(dismissBlock.contains("setResult(RESULT_CANCELED)"))
    }

    @Test
    fun widgetConfigurationDoesNotKeepSplashForeverWhenLoadingFails() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/WidgetConfigurationActivity.kt")
        val loadingBlock = source
            .substringAfter("lifecycleScope.launch {")
            .substringBefore("setContent {")

        assertTrue(loadingBlock.contains("catch (error: Exception)"))
        assertTrue(loadingBlock.contains("if (error is CancellationException) throw error"))
        assertTrue(loadingBlock.contains("finish()"))
    }

    @Test
    fun existingGlobalWidgetPreferencesMigrateToPerWidgetState() {
        val cacheSource = readSource("app/src/main/java/com/clhs/score/data/GradeCacheStore.kt")
        val widgetSource = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        assertTrue(cacheSource.contains("loadLegacyWidgetPreferences"))
        assertTrue(cacheSource.contains("clearLegacyWidgetPreferences"))
        assertTrue(widgetSource.contains("legacyPreferences"))
        assertTrue(widgetSource.contains("if (this[WidgetShowTeacherKey] == null)"))
        assertTrue(widgetSource.contains("if (this[WidgetShowClassroomKey] == null)"))
        assertTrue(widgetSource.contains("if (this[WidgetShowTimeKey] == null)"))
        assertTrue(widgetSource.contains("cacheStore.clearLegacyWidgetPreferences()"))
        val loadPreferences = widgetSource
            .substringAfter("internal suspend fun loadScheduleWidgetPreferences(")
            .substringBefore("internal suspend fun saveScheduleWidgetPreferences(")
        assertFalse(loadPreferences.contains("loadLegacyWidgetPreferences"))
    }

    @Test
    fun widgetConfigurationSaveIsSingleFlightAndReportsFailure() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/schedule/WidgetSettingsScreen.kt")

        assertTrue(source.contains("enabled = !isSaving"))
        assertTrue(source.contains("if (!isSaving)"))
        assertTrue(source.contains("if (error is CancellationException) throw error"))
        assertTrue(source.contains("snackbarHostState.showSnackbar(\"儲存失敗，請再試一次\")"))
    }

    @Test
    fun scheduleRefreshBoundaryUsesObservableTimeState() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/schedule/ScheduleScreen.kt")

        assertTrue(source.contains("var scheduleNow by remember(uiState.report)"))
        assertTrue(source.contains("delay(Duration.between(scheduleNow, refreshAt).toMillis())"))
        assertFalse(source.contains("shouldRefreshAt(LocalDateTime.now())"))
    }

    @Test
    fun widgetScheduleCacheOmitsUnusedComparisonDetails() {
        val source = readSource("app/src/main/java/com/clhs/score/data/GradeCacheStore.kt")

        assertTrue(source.countOccurrences("copy(changes = null)") >= 2)
    }

    @Test
    fun widgetSynchronizationRunsOffTheMainThread() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        assertTrue(source.countOccurrences("= withContext(Dispatchers.IO) {") >= 2)
    }

    @Test
    fun appThemeChangesRefreshScheduleWidgets() {
        val source = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")

        assertTrue(source.contains("LaunchedEffect(appSettings.themeMode, appSettings.dynamicColor, appSettings.amoledBlack)"))
        assertTrue(source.contains("com.clhs.score.widget.syncAllScheduleWidgets(applicationContext, appSettings)"))
        assertTrue(source.contains("com.clhs.score.widget.refreshScheduleWidgetPreview(applicationContext, appSettings)"))
    }

    @Test
    fun mainActivityUsesRealSettingsForFirstFrameTheme() {
        val activitySource = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")
        val settingsViewModelSource = readSource("app/src/main/java/com/clhs/score/viewmodel/SettingsViewModel.kt")

        val setContentIndex = activitySource.indexOf("setContent {")
        val scoreThemeIndex = activitySource.indexOf("ScoreTheme(")
        val readyGateIndex = activitySource.indexOf("if (!isReady)", scoreThemeIndex)

        assertTrue("MainActivity must use the Android SplashScreen API while loading settings", activitySource.contains("installSplashScreen()"))
        assertTrue(activitySource.contains("splashScreen.setKeepOnScreenCondition { launchSettings.value == null }"))
        assertTrue(activitySource.contains("splashScreen.setOnExitAnimationListener"))
        assertTrue(activitySource.contains("val iconView = runCatching { splashScreenView.iconView }.getOrNull()"))
        assertTrue(activitySource.contains("if (iconView == null)"))
        assertFalse(activitySource.contains("splashScreenView.iconView.animate()"))
        assertFalse("Splash exit must not fade the whole splash window over app content", activitySource.contains("splashScreenView.view.animate()"))
        assertTrue(activitySource.contains(".alpha(0f)"))
        assertTrue(activitySource.contains(".scaleX(0.96f)"))
        assertTrue(activitySource.contains(".scaleY(0.96f)"))
        assertTrue(activitySource.contains(".setDuration(200L)"))
        assertTrue(activitySource.contains("DecelerateInterpolator()"))
        assertTrue(activitySource.contains(".withEndAction {"))
        assertTrue("MainActivity must load persisted settings asynchronously", activitySource.contains("lifecycleScope.launch"))
        assertTrue("MainActivity must not block the main thread while loading settings", !activitySource.contains("runBlocking"))
        assertTrue("MainActivity must wait for persisted settings before creating app content", setContentIndex < scoreThemeIndex)
        assertTrue(activitySource.contains("val initialSettings = launchSettings.value ?: return@setContent"))
        assertTrue(activitySource.contains("SettingsViewModel.factory(applicationContext, initialSettings)"))
        assertTrue("Readiness gate must be inside ScoreTheme so the window does not flash the manifest light theme", scoreThemeIndex in 0 until readyGateIndex)
        assertTrue(settingsViewModelSource.contains("initialSettings: AppSettings = AppSettings()"))
        assertTrue(settingsViewModelSource.contains("private val _settings = MutableStateFlow(initialSettings)"))
    }

    @Test
    fun splashExitReappliesPersistedSystemBarAppearance() {
        val source = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")
        val splashExitBlock = source
            .substringAfter("splashScreen.setOnExitAnimationListener")
            .substringBefore("super.onCreate(savedInstanceState)")
        val nullIconBlock = splashExitBlock
            .substringAfter("if (iconView == null)")
            .substringBefore("return@setOnExitAnimationListener")
        val animatedExitBlock = splashExitBlock
            .substringAfter(".withEndAction {")
            .substringBefore("}", missingDelimiterValue = "")
        val systemBarAppearanceBlock = source
            .substringAfter("private fun applyLaunchSystemBarAppearance()")
            .substringBefore("private fun clearWidgetScheduleCache()")

        assertTrue(
            "Android 12 splash exit reapplies the manifest theme, so the app theme must win afterward",
            nullIconBlock.indexOf("splashScreenView.remove()") in
                0 until nullIconBlock.indexOf("applyLaunchSystemBarAppearance()"),
        )
        assertTrue(
            "Animated splash exit must reapply the app theme after removing the splash view",
            animatedExitBlock.indexOf("splashScreenView.remove()") in
                0 until animatedExitBlock.indexOf("applyLaunchSystemBarAppearance()"),
        )
        assertTrue(systemBarAppearanceBlock.contains("ThemeMode.DARK -> true"))
        assertTrue(systemBarAppearanceBlock.contains("ThemeMode.LIGHT -> false"))
        assertTrue(systemBarAppearanceBlock.contains("Configuration.UI_MODE_NIGHT_MASK"))
        assertTrue(systemBarAppearanceBlock.contains("isAppearanceLightStatusBars = !useDark"))
        assertTrue(systemBarAppearanceBlock.contains("isAppearanceLightNavigationBars = !useDark"))
    }

    @Test
    fun scheduleWidgetReadsPreferencesFromGlanceState() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        assertTrue("Widget must sync data to Glance State to support reactive updates", source.contains("updateAppWidgetState"))
        assertTrue("Widget content must read preferences using currentState", source.contains("currentState(key ="))
        assertTrue("Each widget must load its own preferences from Glance state", source.contains("getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)"))
        assertTrue("Widget theme mode must be part of Glance state", source.contains("WidgetThemeModeKey"))
        assertTrue("Widget dynamic color setting must be part of Glance state", source.contains("WidgetDynamicColorKey"))
        assertTrue("Widget AMOLED setting must be part of Glance state", source.contains("WidgetAmoledBlackKey"))
        assertTrue("Widget colors must use the current Glance theme settings", source.contains("getWidgetColorProviders(context, currentWidgetSettings(appSettings))"))
    }

    @Test
    fun smallScheduleWidgetPrioritizesOneUsefulLesson() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")
        val previewSource = readSource("app/src/main/java/com/clhs/score/ui/schedule/WidgetSettingsScreen.kt")

        assertTrue(source.contains("widgetSize.height < 160.dp"))
        assertTrue(source.contains("widgetSize.width < 220.dp"))
        assertTrue(source.contains("sections.prioritized.take(1)"))
        assertTrue(source.contains("showTeacher && !isShort && !isNarrow"))
        assertTrue(source.contains("showClassroom && !isShort && !isNarrow"))
        assertFalse(source.contains("text = \"上課中\""))
        assertFalse(previewSource.contains("text = \"上課中\""))
        assertFalse(previewSource.contains("PreviewTitleBar("))
        assertTrue(source.contains("horizontalPadding = if (isShort) 8.dp else 16.dp"))
        assertTrue(previewSource.contains("horizontal = if (isShort) 8.dp else 16.dp"))
        assertTrue(source.contains(".padding(top = if (isShort) 8.dp else 12.dp, bottom = if (isShort) 4.dp else 8.dp)"))
        assertTrue(previewSource.contains(".padding(top = if (isShort) 8.dp else 12.dp, bottom = if (isShort) 4.dp else 8.dp)"))
        assertTrue(previewSource.contains("android.R.dimen.system_app_widget_background_radius"))
    }

    @Test
    fun scheduleWidgetKeepsTierOnePlatformIntegration() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")
        val provider = readSource("app/src/main/res/xml/schedule_widget_info.xml")
        val manifest = readSource("app/src/main/AndroidManifest.xml")

        assertTrue(source.contains("Scaffold("))
        assertFalse(source.contains("TitleBar("))
        assertTrue(source.contains("override val previewSizeMode = SizeMode.Responsive("))
        assertTrue(source.contains("override suspend fun providePreview("))
        assertTrue(source.contains("setWidgetPreviews(ScheduleWidgetReceiver::class)"))
        assertTrue(source.contains("generatedPreviewCategories"))
        assertTrue(source.contains("ScheduleWidgetPreviewRevision"))
        assertTrue(source.contains("actionStartActivity(intent)"))

        assertTrue(provider.contains("android:minResizeWidth=\"180dp\""))
        assertTrue(provider.contains("android:minResizeHeight=\"110dp\""))
        assertTrue(provider.contains("android:maxResizeWidth=\"460dp\""))
        assertTrue(provider.contains("android:maxResizeHeight=\"500dp\""))
        assertTrue(provider.contains("android:initialLayout=\"@layout/glance_default_loading_layout\""))
        assertTrue(provider.contains("android:previewImage=\"@drawable/schedule_widget_preview\""))
        assertTrue(provider.contains("android:description=\"@string/schedule_widget_description\""))
        assertTrue(provider.contains("android:widgetFeatures=\"reconfigurable\""))
        assertTrue(manifest.contains("android:label=\"@string/schedule_widget_name\""))
    }

    @Test
    fun analyticsLayerDoesNotDefineSensitiveParameters() {
        val sources = listOf(
            "app/src/main/java/com/clhs/score/analytics/AnalyticsEvents.kt",
            "app/src/main/java/com/clhs/score/analytics/AnalyticsLogger.kt",
            "app/src/main/java/com/clhs/score/analytics/AnalyticsParameterSanitizer.kt",
            "app/src/main/java/com/clhs/score/analytics/FirebaseAnalyticsLogger.kt",
            "app/src/main/java/com/clhs/score/analytics/UsageStatisticsStore.kt",
        ).joinToString("\n") { path -> readSource(path) }

        val forbiddenTerms = listOf(
            "setUserId",
            "studentNo",
            "studentName",
            "className",
            "seatNo",
            "apiToken",
            "cookies",
            "rawResult",
            "scoreValue",
            "url",
        )
        forbiddenTerms.forEach { term ->
            assertFalse("Analytics layer must not expose sensitive data: $term", sources.contains(term))
        }
    }

    @Test
    fun developerDiagnosticsAreNotSharedThroughBroadTextIntent() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/DeveloperSettingsScreen.kt")

        assertTrue(source.contains("copyText(\"CLHS Pocket 診斷包\""))
        assertFalse("Diagnostic reports must not be sent through ACTION_SEND", source.contains("ACTION_SEND"))
        assertFalse("Diagnostic reports must not be embedded in EXTRA_TEXT", source.contains("EXTRA_TEXT"))
        assertFalse("Diagnostic reports must not keep a broad share helper", source.contains("shareText("))
    }

    private fun readSource(relativePath: String): String {
        val root = findAndroidRoot()
        return Files.readString(root.resolve(relativePath))
    }

    private fun findAndroidRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (true) {
            if (Files.exists(current.resolve("settings.gradle.kts")) &&
                Files.exists(current.resolve("app/src/main/java/com/clhs/score/MainActivity.kt"))
            ) {
                return current
            }
            current = current.parent ?: error("Unable to locate Android project root")
        }
    }

    private fun String.countOccurrences(term: String): Int =
        windowed(term.length).count { it == term }
}
