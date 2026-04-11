package io.github.mmarco94.tambourine.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import java.nio.file.Path
import java.util.prefs.Preferences
import kotlin.io.path.absolutePathString

object Preferences {

    private val prefs by lazy {
        Preferences.userRoot().node("/io/github/music-player")
    }
    private const val KEY_LIBRARY = "library_folder"
    private const val KEY_SYSTEM_DECORATIONS = "system_decorations"
    private const val KEY_FONT_SCALE = "font_scale"
    private val librarySignal = MutableStateFlow(Any())
    private val useSystemDecorationsSignal = MutableStateFlow(Any())
    private val fontScaleSignal = MutableStateFlow(Any())

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryFolder: Flow<Path> = librarySignal
        .mapLatest { getLibraryFolder() }
        .flowOn(Dispatchers.IO)

    val useSystemDecorations: State<Boolean>
        @OptIn(ExperimentalCoroutinesApi::class)
        @Composable
        get() {
            return remember {
                useSystemDecorationsSignal.mapLatest { useSystemDecorations() }
            }.collectAsState(initial = useSystemDecorations())
        }

    val fontScale: State<Float>
        @OptIn(ExperimentalCoroutinesApi::class)
        @Composable
        get() {
            return remember {
                fontScaleSignal.mapLatest { getFontScale() }
            }.collectAsState(initial = getFontScale())
        }

    fun reloadLibrary() {
        librarySignal.value = Any()
    }

    fun getLibraryFolder() = Path.of(prefs.get(KEY_LIBRARY, System.getProperty("user.home") + "/Music"))

    fun setLibraryFolder(library: Path) {
        prefs.put(KEY_LIBRARY, library.absolutePathString())
        prefs.flush()
        librarySignal.value = Any()
    }

    fun useSystemDecorations(): Boolean {
        return prefs.getBoolean(KEY_SYSTEM_DECORATIONS, false)
    }

    fun setUseSystemDecorations(useSystemDecorations: Boolean) {
        prefs.putBoolean(KEY_SYSTEM_DECORATIONS, useSystemDecorations)
        prefs.flush()
        useSystemDecorationsSignal.value = Any()
    }

    fun getFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SCALE, 1f)
    }

    fun setFontScale(fontScale: Float) {
        prefs.putFloat(KEY_FONT_SCALE, fontScale)
        prefs.flush()
        fontScaleSignal.value = Any()
    }
}