package com.clhs.score

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionArchitectureTest {
    @Test
    fun notificationPermissionFlowUsesRuntimeRequestAndSystemAvailabilityChecks() {
        val settings = readSource("app/src/main/java/com/clhs/score/ui/SettingsScreen.kt")
        val grades = readSource("app/src/main/java/com/clhs/score/ui/GradesScreen.kt")
        val prompt = readSource("app/src/main/java/com/clhs/score/ui/NotificationPromptDialog.kt")
        val app = readSource("app/src/main/java/com/clhs/score/ui/ScoreApp.kt")
        val helper = readSource("app/src/main/java/com/clhs/score/notifications/NotificationPermissionHelper.kt")

        listOf(settings, grades, prompt).forEach { source ->
            assertTrue(source.contains("ActivityResultContracts.RequestPermission()"))
            assertTrue(source.contains("shouldShowPostNotificationsRationale()"))
        }
        assertTrue(settings.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU"))
        assertTrue(grades.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU"))
        assertTrue(app.contains("Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU"))
        assertTrue(helper.contains("ContextCompat.checkSelfPermission("))
        assertTrue(helper.contains("NotificationManagerCompat.from(this).areNotificationsEnabled()"))
        assertTrue(helper.contains("hasPostNotificationsPermission() && areNotificationsEnabled()"))
        assertTrue(helper.contains("ActivityCompat.shouldShowRequestPermissionRationale("))
    }

    private fun readSource(relativePath: String): String =
        Files.readString(findAndroidRoot().resolve(relativePath))

    private fun findAndroidRoot(): Path {
        var current = Paths.get("").toAbsolutePath()
        while (true) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current
            }
            current = current.parent ?: error("Unable to locate Android project root")
        }
    }
}
