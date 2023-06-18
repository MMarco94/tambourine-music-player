package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import io.github.mmarco94.tambourine.utils.Preferences
import java.io.File


@Composable
fun SettingsButton(
    modifier: Modifier,
    openSettings: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    IconButton({ showMenu = !showMenu }, modifier) {
        Icon(Icons.Default.MoreVert, "Open menu")
    }
    DropdownMenu(
        showMenu,
        { showMenu = false },
        offset = DpOffset(8.dp, 0.dp),
    ) {
        DropdownMenuItem(text = { SingleLineText("Settings", style = LocalTextStyle.current) },
            leadingIcon = { Icon(Icons.Default.Settings, null) },
            onClick = {
                openSettings()
                showMenu = false
            })
    }
}

@Composable
fun LibrarySettings(close: () -> Unit) {
    var library by remember { mutableStateOf(Preferences.getLibraryFolder()) }
    val latest by Preferences.libraryFolder.collectAsState(library)

    Window(
        close,
        title = "Library settings",
        state = rememberWindowState(
            size = DpSize(640.dp, 320.dp),
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(32.dp).widthIn(max = 480.dp).align(Alignment.TopCenter)) {
                    LibraryDirectorySetting(library) {
                        library = it
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        {
                            Preferences.setLibraryFolder(library)
                            close()
                        }, enabled = latest != library && library.exists(), modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Apply changes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryDirectorySetting(library: File, changeLibrary: (File) -> Unit) {
    var showFilePicker by remember { mutableStateOf(false) }
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        val interactionSource = remember { MutableInteractionSource() }
        OutlinedTextField(
            library.absolutePath,
            { changeLibrary(File(it)) },
            Modifier.weight(1f),
            label = { SingleLineText("Library folder", style = LocalTextStyle.current) },
            maxLines = 1,
            shape = MaterialTheme.shapes.medium.copy(
                topEnd = ZeroCornerSize, bottomEnd = ZeroCornerSize
            ),
            leadingIcon = {
                Icon(Icons.Default.LibraryMusic, null)
            },
            isError = !library.exists(),
            interactionSource = interactionSource,
        )
        Surface(
            Modifier.fillMaxHeight().width(48.dp).padding(top = 8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize, bottomStart = ZeroCornerSize
            ),
        ) {
            Box(Modifier.fillMaxSize().clickable {
                showFilePicker = true
            }) {
                Icon(Icons.Default.FolderOpen, "Choose folder", Modifier.align(Alignment.Center))
            }
        }
    }

    DirectoryPicker(showFilePicker, initialDirectory = library.absolutePath) { path ->
        showFilePicker = false
        if (path != null) {
            changeLibrary(File(path))
        }
    }
}
