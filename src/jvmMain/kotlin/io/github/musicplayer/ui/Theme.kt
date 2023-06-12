package io.github.musicplayer.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


val ColorScheme.onSurfaceSecondary get() = onSurface.copy(alpha = 0.5f)

const val inactiveAlpha = .4f

/**
 * Inspired by https://github.com/gtk-flutter/adwaita/blob/main/lib/src/theme.dart
 */
object MusicPlayerTheme {
    val shapes: Shapes = Shapes(
        extraSmall = RoundedCornerShape(size = 4.dp),
        small = RoundedCornerShape(size = 6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )
    val colors: ColorScheme = darkColorScheme(
        outlineVariant = Color.White.copy(alpha = 0.2f),
        //surface = Color(0xFF242424)
    )
    val typography
        get() = androidx.compose.material3.Typography()
}