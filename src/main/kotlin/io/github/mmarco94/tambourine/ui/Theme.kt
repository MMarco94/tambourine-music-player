package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.utils.HSLColor
import io.github.mmarco94.tambourine.utils.hsb


val ColorScheme.onSurfaceSecondary get() = onSurface.copy(alpha = 0.55f)

// See Material3.ColorScheme
const val inactiveAlpha = .38f

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

    private val defaultPalette = listOf(
        Color(red = 208, green = 188, blue = 255),
        Color(red = 204, green = 194, blue = 220),
        Color(red = 239, green = 184, blue = 200),
    ).map { it.hsb() }

    val defaultScheme = colorSchemeFromPalette(defaultPalette)

    fun colorSchemeFromPalette(palette: List<HSLColor>): ColorScheme {
        val primary = palette[0]
        val secondary = palette[1]
        val tertiary = palette[2]
        val primaryContainer = primary.darker()
        val secondaryContainer = secondary.darker()
        val tertiaryContainer = tertiary.darker()
        return darkColorScheme(
            primary = primary.color,
            onPrimary = primary.contrast().color,
            primaryContainer = primaryContainer.color,
            onPrimaryContainer = primaryContainer.contrast().color,
            secondary = secondary.color,
            onSecondary = secondary.contrast().color,
            secondaryContainer = secondaryContainer.color,
            onSecondaryContainer = secondaryContainer.contrast().color,
            tertiary = tertiary.color,
            onTertiary = tertiary.contrast().color,
            tertiaryContainer = tertiaryContainer.color,
            onTertiaryContainer = tertiaryContainer.contrast().color,
            outlineVariant = Color.White.copy(alpha = 0.2f),
        )
    }

    val typography
        get() = androidx.compose.material3.Typography()
}