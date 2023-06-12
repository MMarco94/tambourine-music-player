@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeUiApi::class)

package io.github.musicplayer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.musicplayer.Panel.*
import io.github.musicplayer.audio.PlayerController
import io.github.musicplayer.audio.Position
import io.github.musicplayer.data.*
import io.github.musicplayer.ui.*
import io.github.musicplayer.utils.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.slf4j.bridge.SLF4JBridgeHandler


val musicLibrary: Flow<Library?> = Preferences.libraryFolder
    .transformLatest {
        emit(null)
        emit(Library.fromFolder(it))
    }

private enum class Panel(
    val icon: ImageVector,
    val label: String,
) {
    LIBRARY(Icons.Filled.LibraryMusic, "Library"),
    QUEUE(Icons.Filled.QueueMusic, "Queue"),
    PLAYER(Icons.Filled.PlayCircleFilled, "Player"),
}

@Composable
private fun App(selectedPanel: Panel, selectPanel: (Panel) -> Unit) {
    val library by musicLibrary.collectAsState(null, Dispatchers.Default)
    var listOptions by remember(library) { mutableStateOf(SongListOptions()) }
    var openSettings by remember { mutableStateOf(false) }
    val player = playerController.current

    MaterialTheme(
        typography = MusicPlayerTheme.typography,
        colorScheme = MusicPlayerTheme.colors,
        shapes = MusicPlayerTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                thickness = 12.dp,
                shape = RoundedCornerShape(6.dp),
                unhoverColor = Color.White.copy(alpha = .12f),
                hoverColor = Color.White.copy(alpha = .5f),
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BlurredFadeAlbumCover(player.queue?.currentSong?.cover, Modifier.fillMaxSize())
                BoxWithConstraints {
                    val w = constraints.maxWidth
                    val large = w >= (BIG_SONG_ROW_DESIRED_WIDTH * 2).toPxApprox()
                    val visiblePanels = if (large) {
                        listOf(selectedPanel, PLAYER).distinct()
                    } else {
                        listOf(selectedPanel)
                    }
                    Column {
                        MainContent(
                            Modifier.fillMaxWidth().weight(1f),
                            visiblePanels,
                            library,
                            listOptions,
                            { listOptions = it }) {
                            openSettings = true
                        }
                        Divider()
                        BottomBar(large, selectedPanel, visiblePanels, selectPanel)
                    }
                }
                if (openSettings) {
                    LibrarySettings { openSettings = false }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    visiblePanels: List<Panel>,
    library: Library?,
    listOptions: SongListOptions,
    setListOptions: (SongListOptions) -> Unit,
    openSettings: () -> Unit,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    PanelContainer(modifier, values().toSet(), visiblePanels) { panel ->
        when (panel) {
            LIBRARY -> LibraryContainer(library) { library ->
                val lib = library.filterAndSort(listOptions)
                val items = lib.toListItems(listOptions)
                LibraryHeader(Modifier.fillMaxSize(), library, listOptions, setListOptions, openSettings) {
                    SongListUI(lib.stats.maxTrackNumber, items) { song ->
                        cs.launch {
                            player.changeQueue(SongQueue.of(lib, song), Position.Beginning)
                            player.play()
                        }
                    }
                }
            }

            QUEUE -> SongQueueUI(Modifier.fillMaxSize()) { queue ->
                cs.launch {
                    player.changeQueue(queue, Position.Beginning)
                    player.play()
                }
            }

            PLAYER -> PlayerUI(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BottomBar(
    large: Boolean,
    selectedPanel: Panel,
    visiblePanels: List<Panel>,
    selectPanel: (Panel) -> Unit,
) {
    val doFancy = large && selectedPanel != PLAYER
    Row(Modifier.height(IntrinsicSize.Max)) {
        values().forEach { panel ->
            key(panel) {
                val isSelected = panel in visiblePanels
                val alpha by animateFloatAsState(if (isSelected) 1f else inactiveAlpha)
                val bg by animateColorAsState(Color.Black.copy(alpha = if (panel != PLAYER && doFancy) 0.3f else 0.2f))
                val weight by animateFloatAsState(if (panel == PLAYER && doFancy) 2f else 1f)
                Column(
                    Modifier
                        .weight(weight)
                        .background(bg)
                        .fillMaxHeight()
                        .clickable {
                            selectPanel(if (large && selectedPanel == panel) PLAYER else panel)
                        }
                        .alpha(alpha)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(panel.icon, null)
                    SingleLineText(
                        panel.label,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContainer(library: Library?, f: @Composable (Library) -> Unit) {
    if (library == null) {
        BigMessage(
            Modifier.fillMaxSize(),
            Icons.Default.LibraryMusic,
            "Loading...",
        )
    } else {
        f(library)
    }
}

val playerController = staticCompositionLocalOf<PlayerController> { throw IllegalStateException() }

fun main() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    application {
        val cs = rememberCoroutineScope()
        val pc = PlayerController(cs)
        CompositionLocalProvider(playerController provides pc) {
            var selectedPanel by remember { mutableStateOf(LIBRARY) }
            Window(
                title = "Music Player",
                onCloseRequest = ::exitApplication,
                state = remember {
                    WindowState(size = DpSize(1440.dp, 960.dp))
                },
                onPreviewKeyEvent = {
                    if (it.type == KeyEventType.KeyDown) {
                        if (it.isCtrlPressed) {
                            when (it.key) {
                                Key.S -> {
                                    cs.launch {
                                        pc.changeQueue(pc.queue?.toggleShuffle())
                                    }
                                    return@Window true
                                }

                                Key.R -> {
                                    cs.launch {
                                        pc.changeQueue(pc.queue?.toggleRepeat())
                                    }
                                    return@Window true
                                }

                                Key.DirectionLeft -> {
                                    cs.launch {
                                        pc.changeQueue(pc.queue?.previous())
                                    }
                                    return@Window true
                                }

                                Key.DirectionRight -> {
                                    cs.launch {
                                        pc.changeQueue(pc.queue?.next())
                                    }
                                    return@Window true
                                }
                            }
                        } else {
                            when (it.key) {
                                Key.Spacebar -> {
                                    cs.launch {
                                        if (pc.pause) {
                                            pc.play()
                                        } else {
                                            pc.pause()
                                        }
                                    }
                                    return@Window true
                                }

                                Key.F1 -> {
                                    selectedPanel = LIBRARY
                                    return@Window true
                                }

                                Key.F2 -> {
                                    selectedPanel = QUEUE
                                    return@Window true
                                }

                                Key.F3 -> {
                                    selectedPanel = PLAYER
                                    return@Window true
                                }
                            }
                        }
                    }
                    false
                }
            ) {
                App(selectedPanel) { selectedPanel = it }
            }
        }
    }
}