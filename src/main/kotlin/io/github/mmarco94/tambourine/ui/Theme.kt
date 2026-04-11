package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.mmarco94.klibportal.portals.Settings
import io.github.mmarco94.klibportal.portals.appearanceSettingsFlow
import io.github.mmarco94.klibportal.portals.readAppearanceSettings
import io.github.mmarco94.tambourine.LocalAppearanceSettings
import io.github.mmarco94.tambourine.generated.resources.Res
import io.github.mmarco94.tambourine.generated.resources.theme_auto
import io.github.mmarco94.tambourine.generated.resources.theme_dark
import io.github.mmarco94.tambourine.generated.resources.theme_light
import io.github.mmarco94.tambourine.utils.GLOBAL_CONNECTION
import io.github.mmarco94.tambourine.utils.HSLColor
import io.github.mmarco94.tambourine.utils.Preferences
import io.github.mmarco94.tambourine.utils.hsb
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.compose.resources.StringResource

val ColorScheme.onSurfaceSecondary get() = onSurface.copy(alpha = 0.55f)

private val logger = KotlinLogging.logger {}

// See Material3.ColorScheme
const val INACTIVE_ALPHA = .38f
private const val TOO_DARK_THRESHOLD = 0.15
private const val TOO_BRIGHT_THRESHOLD_LIGHT = 0.85
private const val TOO_BRIGHT_THRESHOLD_SATURATION = 0.3

private val defaultSystemAppearance = Settings.Appearance(
    colorScheme = Settings.Appearance.ColorScheme.NO_PREFERENCE,
    accentColor = null,
    contrast = Settings.Appearance.Contrast.NORMAL,
    reducedMotion = Settings.Appearance.ReducedMotion.NORMAL,
)

object TambourineTheme {

    enum class UserPreference(val nameRes: StringResource) {
        AUTO(Res.string.theme_auto), LIGHT(Res.string.theme_light), DARK(Res.string.theme_dark),
    }

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

    data class ColorSchemeContainer(
        val light: ColorScheme,
        val dark: ColorScheme,
    ) {
        @Composable
        fun auto(): ColorScheme {
            val theme by Preferences.theme.state
            return when (theme) {
                UserPreference.LIGHT -> light
                UserPreference.DARK -> dark
                UserPreference.AUTO -> when (LocalAppearanceSettings.current.colorScheme) {
                    Settings.Appearance.ColorScheme.LIGHT -> light
                    Settings.Appearance.ColorScheme.NO_PREFERENCE -> light
                    Settings.Appearance.ColorScheme.DARK -> dark
                }
            }
        }
    }

    val defaultScheme = colorScheme(defaultPalette)

    fun colorScheme(palette: List<HSLColor>): ColorSchemeContainer {
        fun penalty(color: HSLColor): Float {
            return if (color.lightness < TOO_DARK_THRESHOLD) {
                1 - color.lightness
            } else if (color.lightness > TOO_BRIGHT_THRESHOLD_LIGHT && color.saturation < TOO_BRIGHT_THRESHOLD_SATURATION) {
                color.lightness
            } else 0f
        }

        val sorted = palette.sortedBy { color -> penalty(color) }
        val primary = sorted[0].pastel()
        val secondary = sorted.getOrNull(1)?.pastel() ?: primary
        val tertiary = sorted.getOrNull(2)?.pastel() ?: secondary
        return ColorSchemeContainer(
            dark = darkColorScheme(
                primary = primary.color,
                onPrimary = primary.contrast().color,
                primaryContainer = primary.darker().color,
                onPrimaryContainer = primary.darker().contrast().color,
                surface = primary.darker(2f).color,
                onSurface = primary.darker(2f).contrast().color,
                background = primary.darker(3f).color,
                onBackground = primary.darker(3f).contrast().color,
                secondary = secondary.color,
                onSecondary = secondary.contrast().color,
                secondaryContainer = secondary.darker().color,
                onSecondaryContainer = secondary.darker().contrast().color,
                tertiary = tertiary.color,
                onTertiary = tertiary.contrast().color,
                tertiaryContainer = tertiary.darker().color,
                onTertiaryContainer = tertiary.darker().contrast().color,
                outlineVariant = Color.White.copy(alpha = 0.2f),
            ), light = lightColorScheme(
                primary = primary.color,
                onPrimary = primary.contrast().color,
                primaryContainer = primary.lighter().color,
                onPrimaryContainer = primary.lighter().contrast().color,
                surface = primary.lighter(2f).color,
                onSurface = primary.lighter(2f).contrast().color,
                background = primary.lighter(3f).color,
                onBackground = primary.lighter(3f).contrast().color,
                secondary = secondary.color,
                onSecondary = secondary.contrast().color,
                secondaryContainer = secondary.lighter().color,
                onSecondaryContainer = secondary.lighter().contrast().color,
                tertiary = tertiary.color,
                onTertiary = tertiary.contrast().color,
                tertiaryContainer = tertiary.lighter().color,
                onTertiaryContainer = tertiary.lighter().contrast().color,
                outlineVariant = Color.White.copy(alpha = 0.2f),
            )
        )
    }

    val typography
        get() = Typography()
}

@Composable
fun systemAppearanceSettings(): State<Settings.Appearance> {
    if (GLOBAL_CONNECTION == null) {
        return mutableStateOf(defaultSystemAppearance)
    }
    val flow = remember {
        appearanceSettingsFlow(GLOBAL_CONNECTION)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                logger.error(e) { "Failed to read appearance" }
            }
    }
    return flow.collectAsState(remember {
        try {
            readAppearanceSettings(GLOBAL_CONNECTION)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read appearance" }
            defaultSystemAppearance
        }
    })
}
