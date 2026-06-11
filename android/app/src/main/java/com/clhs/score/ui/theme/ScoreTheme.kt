package com.clhs.score.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clhs.score.data.ThemeMode
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable

data class ScoreSemanticColors(
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val neutral: Color
)

internal val LightSemanticColors = ScoreSemanticColors(
    positive = Color(0xFF059669),
    negative = Color(0xFFDC2626),
    warning = Color(0xFFD97706),
    neutral = Color(0xFF6B7280)
)

internal val DarkSemanticColors = ScoreSemanticColors(
    positive = Color(0xFF34D399),
    negative = Color(0xFFF87171),
    warning = Color(0xFFFBBF24),
    neutral = Color(0xFF9CA3AF)
)

internal val LocalScoreSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object ScoreTheme {
    val semanticColors: ScoreSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalScoreSemanticColors.current
}

internal val ScoreShapes = androidx.compose.material3.Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

internal val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF002171),
    secondary = Color(0xFF4F6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D7),
    onSecondaryContainer = Color(0xFF0D1F13),
    tertiary = Color(0xFF33618D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1E4FF),
    onTertiaryContainer = Color(0xFF001D35),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFCFF),
    surface = Color(0xFFFBFCFF),
    surfaceContainer = Color(0xFFEEF2F6),
    surfaceContainerHigh = Color(0xFFE4EAF0),
    surfaceVariant = Color(0xFFDDE3EA),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF00316B),
    primaryContainer = Color(0xFF004A97),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFB6CCBB),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3D),
    onSecondaryContainer = Color(0xFFD2E8D7),
    tertiary = Color(0xFFA0CAFD),
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF164974),
    onTertiaryContainer = Color(0xFFD1E4FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceContainer = Color(0xFF1E2225),
    surfaceContainerHigh = Color(0xFF282D30),
    surfaceVariant = Color(0xFF43474E),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
)

internal val AmoledDarkColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF141414),
)

@Composable
fun ScoreTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) {
                val base = dynamicDarkColorScheme(context)
                if (amoledBlack) {
                    base.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color(0xFF0A0A0A),
                        surfaceContainerHigh = Color(0xFF141414),
                    )
                } else base
            } else {
                dynamicLightColorScheme(context)
            }
        }
        useDark && amoledBlack -> AmoledDarkColors
        useDark -> DarkColors
        else -> LightColors
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
        }
    }

    val semanticColors = if (useDark) DarkSemanticColors else LightSemanticColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalScoreSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = ScoreShapes,
            content = content,
        )
    }
}
