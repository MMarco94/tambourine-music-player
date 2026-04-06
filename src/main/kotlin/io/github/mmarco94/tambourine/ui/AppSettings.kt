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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import io.github.mmarco94.klibportal.portals.openFile
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.utils.Preferences
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.jetbrains.compose.resources.stringResource
import java.io.File

private val logger = KotlinLogging.logger {}

@Composable
fun AppSettingsWindow(close: () -> Unit) {
    var library by remember { mutableStateOf(Preferences.getLibraryFolder()) }
    var useSystemDecorations by remember { mutableStateOf(Preferences.useSystemDecorations()) }
    val preferenceLibrary by Preferences.libraryFolder.collectAsState(library)
    val preferenceUseSystemDecorations by Preferences.useSystemDecorations

    Window(
        close,
        title = stringResource(Res.string.settings),
        state = rememberWindowState(
            size = DpSize(640.dp, 320.dp),
        ),
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.Escape -> {
                        close()
                        true
                    }

                    else -> false
                }
            } else false
        },
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(32.dp).widthIn(max = 480.dp).align(Alignment.TopCenter)) {
                    LibraryDirectorySetting(library) {
                        library = it
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { useSystemDecorations = !useSystemDecorations },
                    ) {
                        Checkbox(useSystemDecorations, {
                            useSystemDecorations = it
                        })
                        Text(stringResource(Res.string.use_system_decorations))
                    }

                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            Preferences.setLibraryFolder(library)
                            Preferences.setUseSystemDecorations(useSystemDecorations)
                            close()
                        },
                        enabled = (preferenceLibrary != library || preferenceUseSystemDecorations != useSystemDecorations) && library.isDirectory,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(Res.string.action_apply_changes))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryDirectorySetting(library: File, changeLibrary: (File) -> Unit) {
    val cs = rememberCoroutineScope()
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        val interactionSource = remember { MutableInteractionSource() }
        OutlinedTextField(
            library.absolutePath,
            { changeLibrary(File(it)) },
            Modifier.weight(1f),
            label = { SingleLineText(stringResource(Res.string.music_library_folder), style = LocalTextStyle.current) },
            maxLines = 1,
            shape = MaterialTheme.shapes.medium.copy(
                topEnd = ZeroCornerSize, bottomEnd = ZeroCornerSize
            ),
            leadingIcon = {
                Icon(Icons.Default.LibraryMusic, null)
            },
            isError = !library.isDirectory,
            interactionSource = interactionSource,
        )
        Surface(
            Modifier.fillMaxHeight().width(48.dp).padding(top = 8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize, bottomStart = ZeroCornerSize
            ),
        ) {
            val chooseLibraryStr = stringResource(Res.string.action_choose_library)
            Box(Modifier.fillMaxSize().clickable {
                cs.launch {
                    DBusConnectionBuilder.forSessionBus().build().use { conn ->
                        try {
                            val file = openFile(
                                conn,
                                title = chooseLibraryStr,
                                directory = true,
                                multiple = false,
                            ).singleOrNull()?.toFile()
                            if (file != null) {
                                changeLibrary(file)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "Error while picking file" }
                        }
                    }
                }
            }) {
                Icon(Icons.Default.FolderOpen, chooseLibraryStr, Modifier.align(Alignment.Center))
            }
        }
    }
}
