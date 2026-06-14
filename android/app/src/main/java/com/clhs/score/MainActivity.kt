package com.clhs.score

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clhs.score.analytics.AnalyticsEvents
import com.clhs.score.analytics.AnalyticsLogger
import com.clhs.score.analytics.AnalyticsParams
import com.clhs.score.analytics.AnalyticsValues
import com.clhs.score.analytics.FirebaseAnalyticsLogger
import com.clhs.score.data.AuthenticatedSession
import com.clhs.score.data.BiometricHelper
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.SessionStore
import com.clhs.score.notifications.NotificationChannels
import com.clhs.score.notifications.ScoreFirebaseMessagingService
import com.clhs.score.reminders.GradeReminderNotifier
import com.clhs.score.ui.BiometricLockScreen
import com.clhs.score.ui.ScoreApp
import com.clhs.score.ui.theme.ScoreTheme
import com.clhs.score.viewmodel.ScoreViewModel
import com.clhs.score.viewmodel.SettingsViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val checkUpdateChannel = Channel<String>(Channel.BUFFERED)
    private val gradeReminderOpenChannel = Channel<Pair<String, String>>(Channel.BUFFERED)
    private val pendingScheduleOpen = mutableStateOf(false)
    private val isAppLocked = mutableStateOf(false)
    private val isBiometricInvalidated = mutableStateOf(false)
    private val isInitialLockResolved = mutableStateOf(false)
    private var wasInBackground = false
    private var shouldLockOnInitialReady = false
    private var isBiometricPromptShowing = false
    private lateinit var sessionStore: SessionStore
    private lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionStore = SessionStore(applicationContext)
        analyticsLogger = FirebaseAnalyticsLogger(applicationContext)
        val hasBiometricSession = sessionStore.hasBiometricSession()
        isAppLocked.value = savedInstanceState?.getBoolean(KEY_APP_LOCKED, false) == true &&
            hasBiometricSession
        pendingScheduleOpen.value = savedInstanceState?.getBoolean(KEY_PENDING_SCHEDULE_OPEN, false) == true
        shouldLockOnInitialReady = savedInstanceState == null
        isInitialLockResolved.value = !shouldLockOnInitialReady || isAppLocked.value
        applySecureWindowPolicy(hasBiometricSession)
        NotificationChannels.ensureCreated(this)
        enableEdgeToEdge()
        handleIntent(intent)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if (wasInBackground && !isBiometricPromptShowing) {
                    wasInBackground = false
                    if (sessionStore.hasBiometricSession()) {
                        isAppLocked.value = true
                    }
                }
            }
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                if (!isChangingConfigurations && !isBiometricPromptShowing) {
                    wasInBackground = true
                }
            }
        })

        setContent {
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(applicationContext),
            )
            val appSettings by settingsVm.settings.collectAsStateWithLifecycle()
            val settingsUi by settingsVm.uiState.collectAsStateWithLifecycle()
            val isReady by settingsVm.isReady.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                checkUpdateChannel.receiveAsFlow().collect { trigger ->
                    settingsVm.checkUpdate(trigger)
                }
            }

            if (!isReady) {
                return@setContent
            }

            // Avoid briefly showing the login intro before the biometric lock decision is applied.
            LaunchedEffect(isReady) {
                if (!isInitialLockResolved.value) {
                    if (shouldLockOnInitialReady &&
                        sessionStore.hasBiometricSession()
                    ) {
                        isAppLocked.value = true
                    }
                    shouldLockOnInitialReady = false
                    isInitialLockResolved.value = true
                }
            }
            if (!isInitialLockResolved.value) {
                return@setContent
            }

            val shouldSecureWindow = appSettings.biometricEnabled ||
                isAppLocked.value ||
                sessionStore.hasBiometricSession()
            LaunchedEffect(shouldSecureWindow) {
                applySecureWindowPolicy(shouldSecureWindow)
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
                val loginState by scoreVm.loginState.collectAsStateWithLifecycle()
                val gradesState by scoreVm.gradesState.collectAsStateWithLifecycle()

                LaunchedEffect(scoreVm) {
                    gradeReminderOpenChannel.receiveAsFlow().collect { (yearValue, examValue) ->
                        scoreVm.openGradeReminderTarget(yearValue, examValue)
                    }
                }

                // 監聽鎖定狀態變化，自動彈出解鎖
                LaunchedEffect(isAppLocked.value) {
                    if (isAppLocked.value) {
                        showBiometricUnlockPrompt(scoreVm, settingsVm)
                    }
                }

                if (isAppLocked.value) {
                    BiometricLockScreen(
                        isBiometricInvalidated = isBiometricInvalidated.value,
                        onTriggerBiometric = {
                            showBiometricUnlockPrompt(scoreVm, settingsVm)
                        },
                        onUnlockWithPin = { pin ->
                            val session = sessionStore.loadSessionWithPin(pin)
                            if (session != null) {
                                scoreVm.loginWithBiometricSession(session)
                                analyticsLogger.logEvent(
                                    AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                                    mapOf(
                                        AnalyticsParams.METHOD to AnalyticsValues.METHOD_PIN,
                                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                                    ),
                                )
                                isAppLocked.value = false
                                if (isBiometricInvalidated.value) {
                                    isBiometricInvalidated.value = false
                                    showBiometricEnrollPrompt(
                                        currentSession = session,
                                        pin = pin,
                                        settingsVm = settingsVm,
                                        replaceInvalidatedKey = true,
                                    )
                                }
                            } else {
                                analyticsLogger.logEvent(
                                    AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                                    mapOf(
                                        AnalyticsParams.METHOD to AnalyticsValues.METHOD_PIN,
                                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                                    ),
                                )
                                Toast.makeText(this@MainActivity, "密碼錯誤", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLogout = {
                            android.webkit.CookieManager.getInstance().removeAllCookies(null)
                            android.webkit.CookieManager.getInstance().flush()
                            scoreVm.logout(AnalyticsValues.SOURCE_LOCK_SCREEN)
                            clearWidgetScheduleCache()
                            sessionStore.clearBiometricSession()
                            settingsVm.setBiometricEnabled(false)
                            isAppLocked.value = false
                            isBiometricInvalidated.value = false
                        }
                    )
                } else {
                    ScoreApp(
                        scoreViewModel = scoreVm,
                        loginState = loginState,
                        gradesState = gradesState,
                        settings = appSettings,
                        settingsUiState = settingsUi,
                        openScheduleRequested = pendingScheduleOpen.value,
                        onScheduleOpenHandled = { pendingScheduleOpen.value = false },
                        onWebViewLoginSuccess = scoreVm::loginWithWebViewCookies,
                        onSelectYear = scoreVm::selectYear,
                        onSelectExam = scoreVm::selectExam,
                        onReload = scoreVm::reloadStructure,
                        onLogout = {
                            android.webkit.CookieManager.getInstance().removeAllCookies(null)
                            android.webkit.CookieManager.getInstance().flush()
                            scoreVm.logout()
                            clearWidgetScheduleCache()
                            sessionStore.clearBiometricSession()
                        },
                        onToggleSubject = scoreVm::toggleSubjectExpanded,
                        onDismissLoginError = scoreVm::clearLoginError,
                        onDismissGradesError = scoreVm::clearGradesError,
                        onStartGradeReminder = scoreVm::startGradeReminder,
                        onStopGradeReminder = scoreVm::stopGradeReminder,
                        onGradeReminderPrerequisiteFailed = scoreVm::reportGradeReminderPrerequisiteError,
                        onDismissGradeReminderError = scoreVm::clearGradeReminderError,
                        onDismissGradeReminderChanges = scoreVm::dismissGradeReminderChanges,
                        onSetThemeMode = settingsVm::setThemeMode,
                        onSetDynamicColor = settingsVm::setDynamicColor,
                        onSetAmoledBlack = settingsVm::setAmoledBlack,
                        onSetNotificationsEnabled = settingsVm::setNotificationsEnabled,
                        onCheckUpdate = { settingsVm.checkUpdate() },
                        onDismissUpdateResult = settingsVm::dismissUpdateResult,
                        onVersionTap = settingsVm::onVersionTap,
                        onDismissDeveloperToast = settingsVm::dismissDeveloperToast,
                        onSetDemoMode = settingsVm::setDemoMode,
                        onDismissRestartDialog = settingsVm::dismissRestartDialog,
                        onDismissNotificationPrompt = settingsVm::dismissNotificationPrompt,
                        onExportGrades = { selections -> scoreVm.exportGrades(selections, applicationContext) },
                        onDismissExportResult = scoreVm::dismissExportResult,
                        analyticsLogger = analyticsLogger,
                        onSetBiometricEnabled = { enabled, pin ->
                            if (enabled && pin != null) {
                                val currentSession = scoreVm.getCurrentSession()
                                if (currentSession != null) {
                                    showBiometricEnrollPrompt(currentSession, pin, settingsVm)
                                } else {
                                    Toast.makeText(applicationContext, "請先登入後再開啟生物解鎖", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val currentSession = scoreVm.getCurrentSession()
                                if (currentSession != null) {
                                    // 關閉時，將 session 寫回普通儲存以確保後續登入可用
                                    sessionStore.saveSession(currentSession)
                                }
                                sessionStore.clearBiometricSession()
                                settingsVm.setBiometricEnabled(false)
                                Toast.makeText(applicationContext, "已關閉生物識別解鎖", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_APP_LOCKED, isAppLocked.value)
        outState.putBoolean(KEY_PENDING_SCHEDULE_OPEN, pendingScheduleOpen.value)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        var routeSource: String? = null

        val isUpdateTopic = intent.getStringExtra("from") == "/topics/app_updates" ||
            intent.getStringExtra(ScoreFirebaseMessagingService.EXTRA_CHECK_UPDATE) == "true" ||
            intent.getStringExtra("action") == "check_update" ||
            intent.getBooleanExtra(ScoreFirebaseMessagingService.EXTRA_CHECK_UPDATE, false)

        if (isUpdateTopic) {
            routeSource = AnalyticsValues.SOURCE_NOTIFICATION_UPDATE
            checkUpdateChannel.trySend(AnalyticsValues.TRIGGER_NOTIFICATION)
            intent.removeExtra("from")
            intent.removeExtra("action")
            intent.removeExtra(ScoreFirebaseMessagingService.EXTRA_CHECK_UPDATE)
        }

        val isGradeReminderOpen = intent.getBooleanExtra(
            GradeReminderNotifier.EXTRA_OPEN_GRADE_REMINDER,
            false,
        )
        if (isGradeReminderOpen) {
            routeSource = AnalyticsValues.SOURCE_GRADE_REMINDER
            val yearValue = intent.getStringExtra(GradeReminderNotifier.EXTRA_YEAR_VALUE).orEmpty()
            val examValue = intent.getStringExtra(GradeReminderNotifier.EXTRA_EXAM_VALUE).orEmpty()
            if (yearValue.isNotBlank() && examValue.isNotBlank()) {
                gradeReminderOpenChannel.trySend(yearValue to examValue)
            }
            intent.removeExtra(GradeReminderNotifier.EXTRA_OPEN_GRADE_REMINDER)
            intent.removeExtra(GradeReminderNotifier.EXTRA_YEAR_VALUE)
            intent.removeExtra(GradeReminderNotifier.EXTRA_EXAM_VALUE)
        }

        val data = intent.data
        if (data?.scheme == "scoreapp" && data.host == "schedule") {
            routeSource = AnalyticsValues.SOURCE_WIDGET_SCHEDULE
            pendingScheduleOpen.value = true
            intent.data = null
        }

        if (routeSource == null &&
            intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        ) {
            routeSource = AnalyticsValues.SOURCE_LAUNCHER
        }

        routeSource?.let { source ->
            analyticsLogger.logEvent(
                AnalyticsEvents.APP_OPEN_ROUTE,
                mapOf(
                    AnalyticsParams.SOURCE to source,
                    AnalyticsParams.LOCKED to (isAppLocked.value || sessionStore.hasBiometricSession()),
                ),
            )
        }
    }

    private fun showBiometricUnlockPrompt(
        scoreVm: ScoreViewModel,
        settingsVm: SettingsViewModel
    ) {
        if (isBiometricPromptShowing) return
        val iv = sessionStore.getBiometricIv()
        if (iv == null) {
            handleKeyInvalidated(scoreVm, settingsVm, "解鎖資訊不完整，請重新登入")
            return
        }

        val cipher = try {
            BiometricHelper.getDecryptCipher(iv)
        } catch (e: Exception) {
            if (BiometricHelper.isKeyPermanentlyInvalidated(e)) {
                isBiometricInvalidated.value = true
                analyticsLogger.logEvent(
                    AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                    mapOf(
                        AnalyticsParams.METHOD to AnalyticsValues.METHOD_BIOMETRIC,
                        AnalyticsParams.RESULT to AnalyticsValues.RESULT_INVALIDATED,
                    ),
                )
            } else {
                handleKeyInvalidated(scoreVm, settingsVm, "初始化安全金鑰失敗，請重新登入")
            }
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricPromptShowing = false
                analyticsLogger.logEvent(
                    AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                    mapOf(
                        AnalyticsParams.METHOD to AnalyticsValues.METHOD_BIOMETRIC,
                        AnalyticsParams.RESULT to if (
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            AnalyticsValues.RESULT_CANCELLED
                        } else {
                            AnalyticsValues.RESULT_FAILURE
                        },
                    ),
                )
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(this@MainActivity, "驗證錯誤: $errString", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptShowing = false
                val decryptCipher = result.cryptoObject?.cipher
                if (decryptCipher != null) {
                    val session = sessionStore.loadBiometricSession(decryptCipher)
                    if (session != null) {
                        scoreVm.loginWithBiometricSession(session)
                        analyticsLogger.logEvent(
                            AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                            mapOf(
                                AnalyticsParams.METHOD to AnalyticsValues.METHOD_BIOMETRIC,
                                AnalyticsParams.RESULT to AnalyticsValues.RESULT_SUCCESS,
                            ),
                        )
                        isAppLocked.value = false
                    } else {
                        analyticsLogger.logEvent(
                            AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                            mapOf(
                                AnalyticsParams.METHOD to AnalyticsValues.METHOD_BIOMETRIC,
                                AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                            ),
                        )
                        handleKeyInvalidated(scoreVm, settingsVm, "解析登入資訊失敗，請重新登入")
                    }
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("已鎖定")
            .setSubtitle("請使用生物識別以解鎖")
            .setAllowedAuthenticators(BiometricHelper.strongBiometricAuthenticators)
            .setNegativeButtonText("取消")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        isBiometricPromptShowing = true
        runCatching {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }.onFailure { error ->
            isBiometricPromptShowing = false
            analyticsLogger.logEvent(
                AnalyticsEvents.BIOMETRIC_UNLOCK_RESULT,
                mapOf(
                    AnalyticsParams.METHOD to AnalyticsValues.METHOD_BIOMETRIC,
                    AnalyticsParams.RESULT to AnalyticsValues.RESULT_FAILURE,
                ),
            )
            Toast.makeText(this, "啟動生物識別失敗: ${error.message ?: "未知錯誤"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBiometricEnrollPrompt(
        currentSession: AuthenticatedSession,
        pin: String,
        settingsVm: SettingsViewModel,
        replaceInvalidatedKey: Boolean = false,
    ) {
        if (isBiometricPromptShowing) return
        val cipher = try {
            BiometricHelper.getEncryptCipher()
        } catch (e: Exception) {
            if (replaceInvalidatedKey) {
                fallBackToNormalSession(currentSession, settingsVm)
            }
            Toast.makeText(this, "金鑰生成失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricPromptShowing = false
                if (replaceInvalidatedKey) {
                    fallBackToNormalSession(currentSession, settingsVm)
                }
                Toast.makeText(this@MainActivity, "驗證失敗，未開啟生物解鎖", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptShowing = false
                val encryptCipher = result.cryptoObject?.cipher
                if (encryptCipher != null) {
                    runCatching {
                        sessionStore.saveBiometricSession(currentSession, pin, encryptCipher)
                        sessionStore.clearNormalSession()
                    }.onSuccess {
                        settingsVm.setBiometricEnabled(true)
                        Toast.makeText(this@MainActivity, "已開啟生物識別解鎖", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        if (replaceInvalidatedKey) {
                            fallBackToNormalSession(currentSession, settingsVm)
                        } else {
                            sessionStore.clearBiometricSession()
                        }
                        Toast.makeText(
                            this@MainActivity,
                            "啟用生物識別解鎖失敗: ${error.message ?: "未知錯誤"}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("啟用生物識別解鎖")
            .setSubtitle("請驗證指紋或臉部以進行安全性授權")
            .setAllowedAuthenticators(BiometricHelper.strongBiometricAuthenticators)
            .setNegativeButtonText("取消")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        isBiometricPromptShowing = true
        runCatching {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }.onFailure { error ->
            isBiometricPromptShowing = false
            Toast.makeText(this, "啟動生物識別失敗: ${error.message ?: "未知錯誤"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleKeyInvalidated(
        scoreVm: ScoreViewModel,
        settingsVm: SettingsViewModel,
        message: String
    ) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        sessionStore.clearBiometricSession()
        sessionStore.clearNormalSession()
        settingsVm.setBiometricEnabled(false)
        scoreVm.logout(AnalyticsValues.SOURCE_LOCK_SCREEN)
        clearWidgetScheduleCache()
        isAppLocked.value = false
    }

    private fun fallBackToNormalSession(
        currentSession: AuthenticatedSession,
        settingsVm: SettingsViewModel,
    ) {
        sessionStore.saveSession(currentSession)
        sessionStore.clearBiometricSession()
        settingsVm.setBiometricEnabled(false)
        isBiometricInvalidated.value = false
    }

    private fun applySecureWindowPolicy(enabled: Boolean) {
        if (enabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun clearWidgetScheduleCache() {
        lifecycleScope.launch {
            runCatching {
                GradeCacheStore(applicationContext).clearWidgetScheduleReport()
                com.clhs.score.widget.ScheduleWidget().updateAll(applicationContext)
            }
        }
    }

    private companion object {
        const val KEY_APP_LOCKED = "app_locked"
        const val KEY_PENDING_SCHEDULE_OPEN = "pending_schedule_open"
    }
}
