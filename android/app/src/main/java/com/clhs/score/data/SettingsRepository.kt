package com.clhs.score.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[KEY_THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: false,
            amoledBlack = prefs[KEY_AMOLED_BLACK] ?: false,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: false,
            notificationPromptDismissed = prefs[KEY_NOTIFICATION_PROMPT_DISMISSED] ?: false,
            developerEnabled = prefs[KEY_DEVELOPER_ENABLED] ?: false,
            demoMode = prefs[KEY_DEMO_MODE] ?: false,
            biometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { it[KEY_AMOLED_BLACK] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDeveloperEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DEVELOPER_ENABLED] = enabled }
    }

    suspend fun setNotificationPromptDismissed(dismissed: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATION_PROMPT_DISMISSED] = dismissed }
    }

    suspend fun setDemoMode(enabled: Boolean) {
        dataStore.edit { it[KEY_DEMO_MODE] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_NOTIFICATION_PROMPT_DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
        val KEY_DEVELOPER_ENABLED = booleanPreferencesKey("developer_enabled")
        val KEY_DEMO_MODE = booleanPreferencesKey("demo_mode")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }
}
