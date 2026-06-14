package com.clhs.score.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clhs.score.BuildConfig
import com.clhs.score.data.AppSettings
import com.clhs.score.data.SettingsRepository
import com.clhs.score.data.ThemeMode
import com.clhs.score.data.UpdateChecker
import com.clhs.score.data.UpdateResult
import com.clhs.score.notifications.NotificationTopicManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isCheckingUpdate: Boolean = false,
    val updateResult: UpdateResult? = null,
    val versionTapCount: Int = 0,
    val showDeveloperUnlockedToast: Boolean = false,
    val showRestartDialog: Boolean = false,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val updateChecker: UpdateChecker,
    private val notificationTopicManager: NotificationTopicManager,
) : ViewModel() {
    private var lastNotificationsEnabled: Boolean? = null

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { newSettings ->
                _settings.value = newSettings
                if (lastNotificationsEnabled != newSettings.notificationsEnabled) {
                    lastNotificationsEnabled = newSettings.notificationsEnabled
                    notificationTopicManager.setNotificationsEnabled(newSettings.notificationsEnabled)
                }
                if (!_isReady.value) {
                    _isReady.value = true
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { repository.setAmoledBlack(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    }

    fun dismissNotificationPrompt() {
        viewModelScope.launch { repository.setNotificationPromptDismissed(true) }
    }

    fun checkUpdate() {
        if (_uiState.value.isCheckingUpdate) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateResult = null) }
            val result = updateChecker.check(BuildConfig.VERSION_NAME)
            _uiState.update { it.copy(isCheckingUpdate = false, updateResult = result) }
        }
    }

    fun dismissUpdateResult() {
        _uiState.update { it.copy(updateResult = null) }
    }

    fun onVersionTap() {
        val current = _uiState.value
        if (settings.value.developerEnabled) return
        val newCount = current.versionTapCount + 1
        if (newCount >= DEVELOPER_TAP_THRESHOLD) {
            viewModelScope.launch { repository.setDeveloperEnabled(true) }
            _uiState.update {
                it.copy(versionTapCount = 0, showDeveloperUnlockedToast = true)
            }
        } else {
            _uiState.update { it.copy(versionTapCount = newCount) }
        }
    }

    fun dismissDeveloperToast() {
        _uiState.update { it.copy(showDeveloperUnlockedToast = false) }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDemoMode(enabled)
            _uiState.update { it.copy(showRestartDialog = true) }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
        }
    }

    fun dismissRestartDialog() {
        _uiState.update { it.copy(showRestartDialog = false) }
    }

    companion object {
        private const val DEVELOPER_TAP_THRESHOLD = 10

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val repo = SettingsRepository(appContext)
                    val checker = UpdateChecker()
                    return SettingsViewModel(repo, checker, NotificationTopicManager()) as T
                }
            }
    }
}
