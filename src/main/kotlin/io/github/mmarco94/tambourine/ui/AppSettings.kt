package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ZoomIn
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.jetbrains.compose.resources.stringResource
import java.text.NumberFormat
import kotlin.io.path.isDirectory
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Composable
fun AppSettingsWindow(close: () -> Unit) {
    val fontScale by Preferences.fontScale
    val useSystemDecorations by Preferences.useSystemDecorations
    var maintainOnTop by remember { mutableStateOf(0) }
    val cs = rememberCoroutineScope()

    Window(
        onCloseRequest = close,
        title = stringResource(Res.string.settings),
        state = rememberWindowState(
            size = DpSize(560.dp, 480.dp),
        ),
        alwaysOnTop = maintainOnTop > 0,
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
        fun setUseSystemDecorations(value: Boolean) {
            // Changing this, to avoid the main window going on top of us
            val token = Random.nextInt(1..Int.MAX_VALUE)
            maintainOnTop = token
            cs.launch {
                delay(1.seconds)
                if (maintainOnTop == token) {
                    maintainOnTop = 0
                }
            }
            Preferences.setUseSystemDecorations(value)
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LibraryDirectorySetting(
                    onSelectingFolder = { maintainOnTop = 0 },
                )

                PreferenceItem(
                    stringResource(Res.string.use_system_decorations),
                    stringResource(if (useSystemDecorations) Res.string.yes else Res.string.no),
                    modifier = Modifier.clickable { setUseSystemDecorations(!useSystemDecorations) },
                    leadingContent = { Icon(Icons.Default.Tab, contentDescription = null) },
                    tailingContent = {
                        Checkbox(useSystemDecorations, {
                            setUseSystemDecorations(it)
                        })
                    }
                )

                PreferenceItem(
                    stringResource(Res.string.font_size),
                    value = NumberFormat.getPercentInstance().format(fontScale),
                    leadingContent = { Icon(Icons.Default.ZoomIn, contentDescription = null) },
                ) {
                    Slider(
                        fontScale,
                        {
                            val rounded = (it * 10).roundToInt() / 10f
                            Preferences.setFontScale(rounded)
                        },
                        steps = 14,
                        valueRange = 0.5f..2f,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryDirectorySetting(onSelectingFolder: () -> Unit = {}) {
    val cs = rememberCoroutineScope()
    val chooseLibraryStr = stringResource(Res.string.action_choose_library)
    fun openFolderSelector() {
        onSelectingFolder()
        cs.launch {
            DBusConnectionBuilder.forSessionBus().build().use { conn ->
                try {
                    val file = openFile(
                        conn,
                        title = chooseLibraryStr,
                        directory = true,
                        multiple = false,
                    ).singleOrNull()
                    logger.info { "Selected file $file" }
                    if (file != null && file.isDirectory()) {
                        Preferences.setLibraryFolder(file)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error while picking file" }
                }
            }
        }
    }

    val libraryFolder by Preferences.libraryFolder.collectAsState(Preferences.getLibraryFolder())
    PreferenceItem(
        title = stringResource(Res.string.music_library_folder),
        value = libraryFolder.toString(),
        modifier = Modifier.clickable { openFolderSelector() },
        leadingContent = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
    )
}

@Composable
private fun PreferenceItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
    tailingContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            leadingContent()
            Column(Modifier.weight(1f)) {
                Scaled { // Scaling individual components, rather than whole window, so the Slider is unaffected
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
            tailingContent()
        }
        content()
    }
}
