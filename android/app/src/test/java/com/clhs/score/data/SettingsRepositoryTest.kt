package com.clhs.score.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createRepository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        return SettingsRepository(dataStore)
    }

    @Test
    fun testDefaultSettings() = testScope.runTest {
        val repository = createRepository()

        val initialSettings = repository.settings.first()
        assertEquals(ThemeMode.SYSTEM, initialSettings.themeMode)
        assertFalse(initialSettings.dynamicColor)
        assertFalse(initialSettings.amoledBlack)
        assertFalse(initialSettings.notificationsEnabled)
        assertFalse(initialSettings.notificationPromptDismissed)
        assertFalse(initialSettings.developerEnabled)
        assertFalse(initialSettings.demoMode)
        assertFalse(initialSettings.biometricEnabled)
    }


    @Test
    fun testUpdateSettings() = testScope.runTest {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.settings.first().themeMode)

        repository.setDynamicColor(true)
        assertTrue(repository.settings.first().dynamicColor)

        repository.setAmoledBlack(true)
        assertTrue(repository.settings.first().amoledBlack)

        repository.setNotificationsEnabled(true)
        assertTrue(repository.settings.first().notificationsEnabled)

        repository.setNotificationPromptDismissed(true)
        assertTrue(repository.settings.first().notificationPromptDismissed)

        repository.setDeveloperEnabled(true)
        assertTrue(repository.settings.first().developerEnabled)

        repository.setDemoMode(true)
        assertTrue(repository.settings.first().demoMode)

        repository.setBiometricEnabled(true)
        assertTrue(repository.settings.first().biometricEnabled)
    }

    @Test
    fun testMultipleUpdates() = testScope.runTest {
        val repository = createRepository()

        repository.settings.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(ThemeMode.SYSTEM, initial.themeMode)
            assertFalse(initial.demoMode)

            // Update theme
            repository.setThemeMode(ThemeMode.LIGHT)
            val updatedTheme = awaitItem()
            assertEquals(ThemeMode.LIGHT, updatedTheme.themeMode)
            assertFalse(updatedTheme.demoMode)

            // Update demo mode
            repository.setDemoMode(true)
            val updatedDemo = awaitItem()
            assertEquals(ThemeMode.LIGHT, updatedDemo.themeMode)
            assertTrue(updatedDemo.demoMode)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
