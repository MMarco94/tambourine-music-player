package io.github.mmarco94.tambourine.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.prefs.Preferences

object Preferences {

    private val prefs by lazy {
        Preferences.userRoot().node("/io/github/music-player")
    }
    private const val KEY_LIBRARY = "library_folder"
    private const val KEY_SYSTEM_DECORATIONS = "system_decorations"
    private val signals = MutableStateFlow(Any())

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryFolder: Flow<File> = signals
        .mapLatest { getLibraryFolder() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val useSystemDecorations: State<Boolean>
        @OptIn(ExperimentalCoroutinesApi::class)
        @Composable
        get() {
            return remember {
                signals
                    .mapLatest { useSystemDecorations() }
                    .distinctUntilChanged()
            }.collectAsState(initial = useSystemDecorations())
        }

    fun getLibraryFolder() = File(prefs.get(KEY_LIBRARY, System.getProperty("user.home") + "/Music"))

    fun setLibraryFolder(library: File) {
        prefs.put(KEY_LIBRARY, library.absolutePath)
        prefs.flush()
        signals.value = Any()
    }

    fun useSystemDecorations(): Boolean {
        return prefs.get(KEY_SYSTEM_DECORATIONS, "false") == "true"
    }

    fun setUseSystemDecorations(useSystemDecorations: Boolean) {
        prefs.put(KEY_SYSTEM_DECORATIONS, useSystemDecorations.toString())
        prefs.flush()
        signals.value = Any()
    }
}