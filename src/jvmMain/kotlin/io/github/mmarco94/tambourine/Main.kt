package io.github.mmarco94.tambourine

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.data.LibraryState
import io.github.mmarco94.tambourine.ui.App
import io.github.mmarco94.tambourine.ui.Panel
import io.github.mmarco94.tambourine.utils.Preferences
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import org.slf4j.bridge.SLF4JBridgeHandler


@OptIn(ExperimentalCoroutinesApi::class)
private val musicLibrary: Flow<LibraryState?> = Preferences.libraryFolder
    .transformLatest {
        emit(null)
        val c = Channel<LibraryState>(Channel.CONFLATED)
        coroutineScope {
            launch(Dispatchers.Default) { Library.fromFolder(it, c) }
            launch {
                for (lib in c) {
                    emit(lib)
                }
            }
        }
    }

val playerController = staticCompositionLocalOf<PlayerController> { throw IllegalStateException() }

fun main() = runBlocking {
    // Start loading ASAP
    val ml = musicLibrary.stateIn(this, started = SharingStarted.Eagerly, null)

    // Uncomment to get all logs from ffsampledsp
    // val root: Logger = Logger.getLogger(FFNativeLibraryLoader::class.java.name)
    // root.level = Level.ALL

    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    System.setProperty("compose.application.configure.swing.globals", "false")
    application {
        val cs = rememberCoroutineScope()
        // Using `alwaysOnTop` is the most reliable method. awt.Window.toFront() doesn't work :(
        var bringToTop by remember { mutableStateOf(false) }
        val player = PlayerController(
            cs,
            quit = { exitApplication() },
            raise = { bringToTop = true }
        )
        CompositionLocalProvider(playerController provides player) {
            var selectedPanel by remember { mutableStateOf(Panel.LIBRARY) }
            Window(
                title = "Tambourine",
                onCloseRequest = ::exitApplication,
                state = remember {
                    WindowState(size = DpSize(1440.dp, 960.dp))
                },
                onPreviewKeyEvent = { event ->
                    handleKeypress(cs, event, player) { selectedPanel = it }
                },
                alwaysOnTop = bringToTop.also { bringToTop = false }
            ) {
                val library by ml.collectAsState(null)
                App(library, selectedPanel) { selectedPanel = it }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun handleKeypress(
    cs: CoroutineScope,
    event: KeyEvent,
    player: PlayerController,
    selectedPanel: (Panel) -> Unit
): Boolean {
    if (event.type == KeyEventType.KeyDown) {
        if (event.isCtrlPressed) {
            when (event.key) {
                Key.S -> {
                    cs.launch {
                        player.changeQueue(player.queue?.toggleShuffle())
                    }
                    return true
                }

                Key.R -> {
                    cs.launch {
                        player.changeQueue(player.queue?.toggleRepeat())
                    }
                    return true
                }

                Key.DirectionLeft -> {
                    cs.launch {
                        player.changeQueue(player.queue?.previous())
                    }
                    return true
                }

                Key.DirectionRight -> {
                    cs.launch {
                        player.changeQueue(player.queue?.next())
                    }
                    return true
                }
            }
        } else {
            when (event.key) {
                Key.Spacebar -> {
                    cs.launch {
                        if (player.pause) {
                            player.play()
                        } else {
                            player.pause()
                        }
                    }
                    return true
                }

                Key.F1 -> {
                    selectedPanel(Panel.LIBRARY)
                    return true
                }

                Key.F2 -> {
                    selectedPanel(Panel.QUEUE)
                    return true
                }

                Key.F3 -> {
                    selectedPanel(Panel.PLAYER)
                    return true
                }
            }
        }
    }
    return false
}