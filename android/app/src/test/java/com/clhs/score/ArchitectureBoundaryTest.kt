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
    fun widgetBootReceiverHasRequiredPermission() {
        val manifest = readSource("app/src/main/AndroidManifest.xml")

        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
        assertTrue(manifest.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(manifest.contains(".widget.WidgetUpdateReceiver"))
    }

    @Test
    fun coroutineCancellationIsNotConvertedToUiOrWorkerErrors() {
        val scoreViewModel = readSource("app/src/main/java/com/clhs/score/viewmodel/ScoreViewModel.kt")
        val scheduleViewModel = readSource("app/src/main/java/com/clhs/score/viewmodel/ScheduleViewModel.kt")
        val reminderWorker = readSource("app/src/main/java/com/clhs/score/reminders/GradeReminderWorker.kt")
        val updateChecker = readSource("app/src/main/java/com/clhs/score/data/UpdateChecker.kt")

        assertTrue(scoreViewModel.contains("import kotlinx.coroutines.CancellationException"))
        assertTrue(scoreViewModel.countOccurrences("error.throwIfCancellation()") >= 7)
        assertTrue(scoreViewModel.contains("private fun Throwable.throwIfCancellation()"))

        assertTrue(scheduleViewModel.contains("import kotlinx.coroutines.CancellationException"))
        assertTrue(scheduleViewModel.countOccurrences("e.throwIfCancellation()") >= 4)
        assertTrue(scheduleViewModel.contains("private fun Throwable.throwIfCancellation()"))

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
    fun widgetPreferenceSaveCompletesBeforeWidgetRefresh() {
        val source = readSource("app/src/main/java/com/clhs/score/ui/schedule/WidgetSettingsScreen.kt")

        val saveIndex = source.indexOf("cacheStore.saveWidgetPreferences(teacher, classroom, time)")
        val updateIndex = source.indexOf("syncAllScheduleWidgets(context)", saveIndex)

        assertTrue("Widget preferences must be saved before requesting a widget refresh", saveIndex >= 0)
        assertTrue("All widgets must be refreshed after the new preferences are saved", updateIndex > saveIndex)
    }

    @Test
    fun widgetConfigurationUsesTargetedGlanceUpdateWithoutBlockingComposition() {
        val activitySource = readSource("app/src/main/java/com/clhs/score/widget/WidgetConfigurationActivity.kt")
        val widgetSource = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        val setContentBlock = activitySource.substringAfter("setContent {")
        assertTrue("Widget configuration must use the Android SplashScreen API while loading settings", activitySource.contains("installSplashScreen()"))
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
        assertTrue(activitySource.contains(".withEndAction { splashScreenView.remove() }"))
        assertFalse("Widget configuration must not block inside composition", setContentBlock.contains("runBlocking"))
        assertFalse("Widget configuration must not block the main thread while loading settings", activitySource.contains("runBlocking"))
        assertTrue(activitySource.contains("lifecycleScope.launch"))
        assertTrue(activitySource.contains("collectAsStateWithLifecycle"))
        assertFalse("Widget configuration must not render the first frame with a guessed default theme", activitySource.contains("initialValue = AppSettings()"))
        assertTrue(activitySource.contains("initialValue = initialSettings"))
        assertTrue(activitySource.contains("syncScheduleWidget(applicationContext, appWidgetId)"))
        assertTrue(widgetSource.contains("getGlanceIdBy(appWidgetId)"))
        assertTrue(widgetSource.contains("ScheduleWidget().update(context, glanceId)"))
    }

    @Test
    fun appThemeChangesRefreshScheduleWidgets() {
        val source = readSource("app/src/main/java/com/clhs/score/MainActivity.kt")

        assertTrue(source.contains("LaunchedEffect(appSettings.themeMode, appSettings.dynamicColor, appSettings.amoledBlack)"))
        assertTrue(source.contains("com.clhs.score.widget.syncAllScheduleWidgets(applicationContext)"))
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
        assertTrue(activitySource.contains(".withEndAction { splashScreenView.remove() }"))
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
    fun scheduleWidgetReadsPreferencesFromGlanceState() {
        val source = readSource("app/src/main/java/com/clhs/score/widget/ScheduleWidget.kt")

        assertTrue("Widget must read preferences via GradeCacheStore initially", source.contains("cacheStore.getWidgetPreferences()"))
        assertTrue("Widget must sync data to Glance State to support reactive updates", source.contains("updateAppWidgetState"))
        assertTrue("Widget content must read preferences using currentState", source.contains("currentState(key ="))
    }

    @Test
    fun analyticsLayerDoesNotDefineSensitiveParameters() {
        val sources = listOf(
            "app/src/main/java/com/clhs/score/analytics/AnalyticsEvents.kt",
            "app/src/main/java/com/clhs/score/analytics/AnalyticsLogger.kt",
            "app/src/main/java/com/clhs/score/analytics/AnalyticsParameterSanitizer.kt",
            "app/src/main/java/com/clhs/score/analytics/FirebaseAnalyticsLogger.kt",
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

        assertTrue(source.contains("copyText(\"CLHS Score 診斷包\""))
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
