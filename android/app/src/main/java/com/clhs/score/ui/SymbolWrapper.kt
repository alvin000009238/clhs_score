package com.clhs.score.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vicart.compose.material.symbols.FilledRoundedSymbol as RealFilledRoundedSymbol
import dev.vicart.compose.material.symbols.OutlinedRoundedSymbol as RealOutlinedRoundedSymbol

@Composable
fun OutlinedRoundedSymbol(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified
) {
    if (LocalInspectionMode.current) {
        // Fallback for Android Studio Preview
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(if (tint == Color.Unspecified) LocalContentColor.current.copy(alpha = 0.5f) else tint.copy(alpha = 0.5f))
        )
    } else {
        // If tint is unspecified, we don't pass it to let the library use its default, or we pass LocalContentColor.
        // Actually the library's default tint is LocalContentColor.current
        val finalTint = if (tint == Color.Unspecified) LocalContentColor.current else tint
        RealOutlinedRoundedSymbol(
            icon = icon,
            modifier = modifier,
            size = size,
            tint = finalTint
        )
    }
}

@Composable
fun FilledRoundedSymbol(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.Unspecified
) {
    if (LocalInspectionMode.current) {
        // Fallback for Android Studio Preview
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(if (tint == Color.Unspecified) LocalContentColor.current else tint)
        )
    } else {
        val finalTint = if (tint == Color.Unspecified) LocalContentColor.current else tint
        RealFilledRoundedSymbol(
            icon = icon,
            modifier = modifier,
            size = size,
            tint = finalTint
        )
    }
}
