package io.github.musicplayer.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.prefs.Preferences

object Preferences {

    private var prefs = Preferences.userRoot().node("/io/github/music-player")
    private val KEY_LIBRARY = "library_folder"

    val _libraryFolder = MutableStateFlow(
        File(prefs.get(KEY_LIBRARY, System.getProperty("user.home") + "/Music"))
    )
    val libraryFolder: StateFlow<File> get() = _libraryFolder

    fun setLibraryFolder(library: File) {
        _libraryFolder.value = library
        prefs.put(KEY_LIBRARY, library.absolutePath)
        prefs.flush()
    }
}