package io.github.mmarco94.tambourine.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import io.github.mmarco94.tambourine.ui.TambourineTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.awt.Insets
import java.nio.file.Path
import java.util.prefs.Preferences
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

object Preferences {

    private val prefs: Preferences by lazy {
        Preferences.userRoot().node("/io/github/music-player")
    }
    val libraryFolder = PreferenceContainer(
        read = { prefs -> Path.of(prefs.get("library_folder", System.getProperty("user.home") + "/Music")) },
        write = { prefs, value ->
            prefs.put("library_folder", value.pathString)
        }
    )
    val useSystemDecorations = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("system_decorations", false) },
        write = { prefs, value ->
            prefs.putBoolean("system_decorations", value)
        }
    )
    val fontScale = PreferenceContainer(
        read = { prefs -> prefs.getFloat("font_scale", 1f).coerceAtLeast(0.1f) },
        write = { prefs, value ->
            prefs.putFloat("font_scale", value)
        }
    )
    val theme = PreferenceContainer(
        read = { prefs ->
            val preference = prefs.get("theme", "")
            TambourineTheme.UserPreference.entries.singleOrNull {
                it.name.equals(preference, ignoreCase = true)
            } ?: TambourineTheme.UserPreference.AUTO
        },
        write = { prefs, value ->
            prefs.put("theme", value.name)
        }
    )

    @Composable
    fun mainWindowState(): WindowState {
        // 980x680 for screenshot
        val positionX = prefs.get("main_window_position_x", null)?.toFloatOrNull()
        val positionY = prefs.get("main_window_position_y", null)?.toFloatOrNull()
        val state = rememberWindowState(
            size = DpSize(
                prefs.getFloat("main_window_width", 1080f).dp,
                prefs.getFloat("main_window_height", 960f).dp,
            ),
            placement = WindowPlacement.valueOf(prefs.get("main_window_placement", WindowPlacement.Floating.name)),
            position = if (positionX != null && positionY != null) {
                WindowPosition.Absolute(positionX.dp, positionY.dp)
            } else {
                WindowPosition.PlatformDefault
            }
        )
        return state
    }

    suspend fun save(state: WindowState, insets: Insets, density: Density) {
        val size = state.size
        val position = state.position
        val placement = state.placement
        logger.debug { "Saving window state: size = ${size}; position = $position; placement = $placement; insets = $insets" }
        withContext(Dispatchers.IO) {
            if (size.isSpecified && placement == WindowPlacement.Floating) {
                prefs.putFloat("main_window_width", size.width.value - (insets.left + insets.right) / density.density)
                prefs.putFloat("main_window_height", size.height.value - (insets.top + insets.bottom) / density.density)
            }
            prefs.put("main_window_placement", placement.name)
            if (position.isSpecified) {
                prefs.putFloat("main_window_position_x", position.x.value)
                prefs.putFloat("main_window_position_y", position.y.value)
            }
            prefs.flush()
        }
    }

    class PreferenceContainer<T>(
        val read: (Preferences) -> T,
        val write: (Preferences, T) -> Unit,
    ) {
        val signal = MutableStateFlow(Any())

        @Suppress("OPT_IN_USAGE")
        val flow: Flow<T>
            get() = signal
                .mapLatest { get() }
                .flowOn(Dispatchers.Default)

        val state: State<T>
            @OptIn(ExperimentalCoroutinesApi::class)
            @Composable
            get() {
                return remember {
                    signal.mapLatest { get() }
                }.collectAsState(initial = get())
            }

        fun reload() {
            signal.value = Any()
        }

        fun get(): T {
            return read(prefs)
        }

        fun set(value: T) {
            write(prefs, value)
            prefs.flush()
            reload()
        }
    }
}