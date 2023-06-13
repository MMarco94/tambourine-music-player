package io.github.musicplayer

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.musicplayer.audio.PlayerController
import io.github.musicplayer.data.Library
import io.github.musicplayer.data.LibraryState
import io.github.musicplayer.utils.App
import io.github.musicplayer.utils.Panel
import io.github.musicplayer.utils.Preferences
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

    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    //System.setProperty("compose.application.configure.swing.globals", "false")
    application {
        val library by ml.collectAsState(null)
        val cs = rememberCoroutineScope()
        val player = PlayerController(cs)
        CompositionLocalProvider(playerController provides player) {
            var selectedPanel by remember { mutableStateOf(Panel.LIBRARY) }
            Window(
                title = "Music Player",
                onCloseRequest = ::exitApplication,
                state = remember {
                    WindowState(size = DpSize(1440.dp, 960.dp))
                },
                onPreviewKeyEvent = { event ->
                    handleKeypress(cs, event, player) { selectedPanel = it }
                }
            ) {
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