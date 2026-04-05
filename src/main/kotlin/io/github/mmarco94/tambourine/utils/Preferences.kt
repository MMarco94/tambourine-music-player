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
import java.io.File
import java.util.prefs.Preferences

object Preferences {

    private val prefs by lazy {
        Preferences.userRoot().node("/io/github/music-player")
    }
    private const val KEY_LIBRARY = "library_folder"
    private const val KEY_SYSTEM_DECORATIONS = "system_decorations"
    private val librarySignal = MutableStateFlow(Any())
    private val useSystemDecorationsSignal = MutableStateFlow(Any())

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryFolder: Flow<File> = librarySignal
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

    fun reloadLibrary() {
        librarySignal.value = Any()
    }

    fun getLibraryFolder() = File(prefs.get(KEY_LIBRARY, System.getProperty("user.home") + "/Music"))

    fun setLibraryFolder(library: File) {
        prefs.put(KEY_LIBRARY, library.absolutePath)
        prefs.flush()
        librarySignal.value = Any()
    }

    fun useSystemDecorations(): Boolean {
        return prefs.get(KEY_SYSTEM_DECORATIONS, "false") == "true"
    }

    fun setUseSystemDecorations(useSystemDecorations: Boolean) {
        prefs.put(KEY_SYSTEM_DECORATIONS, useSystemDecorations.toString())
        prefs.flush()
        useSystemDecorationsSignal.value = Any()
    }
}