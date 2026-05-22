package com.clhs.score.data

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val amoledBlack: Boolean = false,
    val developerEnabled: Boolean = false,
    val demoMode: Boolean = false,
)
