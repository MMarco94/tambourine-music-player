package io.github.mmarco94.tambourine.utils

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
    private val signals = MutableStateFlow(Any())

    @OptIn(ExperimentalCoroutinesApi::class)
    val libraryFolder: Flow<File> = signals
        .mapLatest { getLibraryFolder() }
        .flowOn(Dispatchers.IO)

    fun getLibraryFolder() = File(prefs.get(KEY_LIBRARY, System.getProperty("user.home") + "/Music"))

    fun setLibraryFolder(library: File) {
        prefs.put(KEY_LIBRARY, library.absolutePath)
        prefs.flush()
        signals.value = Any()
    }
}