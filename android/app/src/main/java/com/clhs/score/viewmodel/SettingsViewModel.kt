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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isCheckingUpdate: Boolean = false,
    val updateResult: UpdateResult? = null,
    val versionTapCount: Int = 0,
    val showDeveloperUnlockedToast: Boolean = false,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { repository.setAmoledBlack(enabled) }
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

    companion object {
        private const val DEVELOPER_TAP_THRESHOLD = 10

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = SettingsRepository(context)
                    val checker = UpdateChecker()
                    return SettingsViewModel(repo, checker) as T
                }
            }
    }
}
