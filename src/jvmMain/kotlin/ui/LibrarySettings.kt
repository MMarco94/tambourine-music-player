package ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import musicLibraryDirectory
import java.io.File


@Composable
fun LibrarySettings(close: () -> Unit) {
    var showFilePicker by remember { mutableStateOf(false) }

    val library by musicLibraryDirectory.collectAsState()

    Window(close, title = "Settings") {
        Surface(Modifier.fillMaxSize()) {
            Row {
                Text("Library: $library")
                Button({ showFilePicker = true }) {
                    Text("Choose library folder")
                }
            }
        }
    }

    DirectoryPicker(showFilePicker, initialDirectory = library.absolutePath) { path ->
        showFilePicker = false
        if (path != null) {
            musicLibraryDirectory.value = File(path)
        }
    }
}
