package io.github.mmarco94.tambourine

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.application
import io.github.mmarco94.klibportal.portals.Settings
import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.audio.Position
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.data.toLibrary
import io.github.mmarco94.tambourine.ui.*
import io.github.mmarco94.tambourine.utils.Preferences
import io.github.mmarco94.tambourine.utils.loadDbusCollection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.bridge.SLF4JBridgeHandler
import java.awt.Toolkit
import java.lang.reflect.Field
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.time.Clock

private val firstInstruction = Clock.System.now()
val playerController = staticCompositionLocalOf<PlayerController> { throw IllegalStateException() }
val mainWindowScope = staticCompositionLocalOf<WindowScope> { throw IllegalStateException() }
val LocalAppearanceSettings = compositionLocalOf<Settings.Appearance> { throw IllegalStateException() }
private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    Thread.currentThread().priority = Thread.MAX_PRIORITY
    val filesFromArgs = args.map { Path.of(it) }
    thread(name = "DBusConnection") { runBlocking { loadDbusCollection() } }
    runBlocking {
        // Start loading ASAP
        val musicLibrary: StateFlow<Library?> = Preferences.libraryFolder.flow
            .map { lib -> setOf(lib) + filesFromArgs }
            .toLibrary()
            .stateIn(this, started = SharingStarted.Eagerly, null)

        // Uncomment to get all logs from ffsampledsp
//         val root = java.util.logging.Logger.getLogger(com.tagtraum.ffsampledsp.FFNativeLibraryLoader::class.java.name)
//         root.level = java.util.logging.Level.ALL

        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()

        // Since this app includes no Swing component, we can avoid overriding its look and feel.
        // This saves ~200ms of time of application setup, see `configureSwingGlobalsForCompose`
        System.setProperty("skiko.rendering.laf.global", "false")
        application {

            remember {
                // Changing the WM class so the icon for the window is properly set.
                // Source - https://stackoverflow.com/a/29218320
                try {
                    val xToolkit = Toolkit.getDefaultToolkit()
                    val awtAppClassNameField: Field = xToolkit.javaClass.getDeclaredField("awtAppClassName")
                    awtAppClassNameField.setAccessible(true)
                    awtAppClassNameField.set(xToolkit, "io.github.mmarco94.tambourine")
                } catch (e: Exception) {
                    logger.warn(e) { "Cannot change default WMClass" }
                }
            }



            Thread.currentThread().priority = Thread.MAX_PRIORITY
            val cs = rememberCoroutineScope()
            // Using `alwaysOnTop` is the most reliable method. awt.Window.toFront() doesn't work :(
            var bringToTop by remember { mutableStateOf(false) }
            val player = remember {
                PlayerController(
                    cs,
                    quit = { exitApplication() },
                    raise = { bringToTop = true },
                    musicLibrary = musicLibrary,
                )
            }
            val systemAppearance by systemAppearanceSettings()
            CompositionLocalProvider(
                playerController provides player,
                LocalAppearanceSettings provides systemAppearance,
            ) {
                MaterialTheme(TambourineTheme.getDefaultScheme()) {
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
                                                player.transformQueue { queue to Position.Current }
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
                        CompositionLocalProvider(mainWindowScope provides this@MainWindow) {
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
    return if (songs.isEmpty()) {
        null
    } else {
        val songsKeys = songs.map { it.uniqueKey }
        SongQueue(
            originalSongs = songsKeys,
            songs = songsKeys,
            songsByKey = songs.associateBy { it.uniqueKey },
            position = 0,
        )
    }
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
                        player.transformQueue { queue ->
                            queue?.toggleShuffle() to Position.Current
                        }
                    }
                    return true
                }

                Key.R -> {
                    cs.launch {
                        player.transformQueue { queue ->
                            queue?.toggleRepeat() to Position.Current
                        }
                    }
                    return true
                }

                Key.DirectionLeft -> {
                    cs.launch {
                        player.transformQueue { queue ->
                            queue?.previous() to Position.Current
                        }
                    }
                    return true
                }

                Key.DirectionRight -> {
                    cs.launch {
                        player.transformQueue { queue ->
                            queue?.next() to Position.Current
                        }
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