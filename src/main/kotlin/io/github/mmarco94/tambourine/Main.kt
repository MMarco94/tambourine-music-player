package io.github.mmarco94.tambourine

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.data.toLibrary
import io.github.mmarco94.tambourine.ui.*
import io.github.mmarco94.tambourine.utils.Preferences
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.bridge.SLF4JBridgeHandler
import java.nio.file.Path
import kotlin.time.Clock

private val firstInstruction = Clock.System.now()
val playerController = staticCompositionLocalOf<PlayerController> { throw IllegalStateException() }
val mainWindowScope = staticCompositionLocalOf<WindowScope> { throw IllegalStateException() }
private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    Thread.currentThread().priority = Thread.MAX_PRIORITY
    val filesFromArgs = args.map { Path.of(it) }
    runBlocking {
        // Start loading ASAP
        val musicLibrary = Preferences.libraryFolder.flow
            .map { lib -> setOf(lib) + filesFromArgs }
            .toLibrary()
            .stateIn(this, started = SharingStarted.Eagerly, null)

        // Uncomment to get all logs from ffsampledsp
//         val root = java.util.logging.Logger.getLogger(com.tagtraum.ffsampledsp.FFNativeLibraryLoader::class.java.name)
//         root.level = java.util.logging.Level.WARNING

        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()

        // Since this app includes no Swing component, we can avoid overriding its look and feel.
        // This saves ~200ms of time of application setup, see `configureSwingGlobalsForCompose`
        System.setProperty("skiko.rendering.laf.global", "false")

        application {
            Thread.currentThread().priority = Thread.MAX_PRIORITY
            val cs = rememberCoroutineScope()
            // Using `alwaysOnTop` is the most reliable method. awt.Window.toFront() doesn't work :(
            var bringToTop by remember { mutableStateOf(false) }
            val player = remember {
                PlayerController(
                    cs,
                    quit = { exitApplication() },
                    raise = { bringToTop = true }
                )
            }
            MaterialTheme(TambourineTheme.defaultScheme.auto()) {
                CompositionLocalProvider(playerController provides player) {
                    var selectedPanel by remember { mutableStateOf(Panel.LIBRARY) }
                    var libraryTab: LibraryHeaderTab? by remember { mutableStateOf(null) }
                    val library by remember {
                        val ms = mutableStateOf<Library?>(null)
                        var done = false
                        cs.launch(start = CoroutineStart.UNDISPATCHED) {
                            musicLibrary
                                .onEach {
                                    if (it != null && !done && filesFromArgs.isNotEmpty()) {
                                        done = true
                                        cs.launch {
                                            val queue = createQueue(it, filesFromArgs)
                                            if (queue != null) {
                                                player.changeQueue(queue)
                                                player.play()
                                            }
                                        }
                                    }
                                }
                                .collect { ms.value = it }
                        }
                        ms
                    }
                    var openSettings by remember { mutableStateOf(false) }
                    MainWindow(
                        title = "Tambourine",
                        onCloseRequest = ::exitApplication,
                        state = rememberWindowState(size = DpSize(1440.dp, 960.dp)),
                        onPreviewKeyEvent = { event ->
                            handleKeypress(
                                cs,
                                event,
                                player,
                                libraryTab,
                                { selectedPanel = it },
                                { libraryTab = it })
                        },
                        alwaysOnTop = bringToTop.also { bringToTop = false },
                    ) {
                        CompositionLocalProvider(mainWindowScope provides this) {
                            var firstDraw by remember { mutableStateOf(true) }
                            Canvas(Modifier) {
                                if (firstDraw) {
                                    firstDraw = false
                                    logger.debug { "First draw took: ${Clock.System.now() - firstInstruction}" }
                                }
                            }
                            App(
                                library = library,
                                selectedPanel = selectedPanel,
                                selectPanel = { selectedPanel = it },
                                openSettings = { openSettings = true },
                                closeApp = ::exitApplication,
                                libraryTab = libraryTab,
                                selectLibraryTab = { libraryTab = it },
                            )
                        }
                    }
                    if (openSettings) {
                        AppSettingsWindow { openSettings = false }
                    }
                }
            }
        }
    }
}

private fun createQueue(library: Library, filesFromArgs: List<Path>): SongQueue? {
    val songs = filesFromArgs
        .flatMap { arg ->
            library.songs
                .filter { s -> s.file.startsWith(arg) }
                .sortedWith(compareBy<Song> { it.disk }.thenBy { it.track }.thenBy { it.file })
        }
    return if (songs.isEmpty()) null
    else SongQueue.of(null, songs, songs.first())
}

private fun handleKeypress(
    cs: CoroutineScope,
    event: KeyEvent,
    player: PlayerController,
    libraryTab: LibraryHeaderTab?,
    selectedPanel: (Panel) -> Unit,
    selectLibraryTab: (LibraryHeaderTab?) -> Unit,
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

                Key.F -> {
                    selectedPanel(Panel.LIBRARY)
                    selectLibraryTab(LibraryHeaderTab.SEARCH)
                    return true
                }
            }
        } else {
            when (event.key) {
                Key.Spacebar -> {
                    if (libraryTab != LibraryHeaderTab.SEARCH) {
                        cs.launch {
                            if (player.pause) {
                                player.play()
                            } else {
                                player.pause()
                            }
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

                Key.Escape -> {
                    if (libraryTab != null) {
                        selectLibraryTab(null)
                        return true
                    }
                }
            }
        }
    }
    return false
}