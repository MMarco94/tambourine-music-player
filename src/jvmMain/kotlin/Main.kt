@file:OptIn(ExperimentalCoroutinesApi::class)

import Panel.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import audio.PlayerCommand
import audio.PlayerController
import audio.Position
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.bridge.SLF4JBridgeHandler
import ui.*
import java.io.File


val musicLibraryDirectory = MutableStateFlow(
    File(System.getProperty("user.home") + "/Music")
)
val musicLibrary: Flow<Library?> = musicLibraryDirectory
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
fun App() {
    val library by musicLibrary.collectAsState(null, Dispatchers.Default)
    var openSettings by remember { mutableStateOf(false) }
    var selectedPanel by remember { mutableStateOf(LIBRARY) }

    MaterialTheme(
        typography = MusicPlayerTheme.typography,
        colors = MusicPlayerTheme.colors,
        shapes = MusicPlayerTheme.shapes,
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            BlurredFadeAlbumCover(PlayerController.queue?.currentSong?.cover, Modifier.fillMaxSize())
            BoxWithConstraints {
                val w = constraints.maxWidth
                val large = w >= with(LocalDensity.current) { (BIG_SONG_ROW_DESIRED_WIDTH * 2).toPx() }
                val visiblePanels = if (large) {
                    listOf(selectedPanel, PLAYER).distinct()
                } else {
                    listOf(selectedPanel)
                }
                Column {
                    MainContent(Modifier.fillMaxWidth().weight(1f), visiblePanels, library) {
                        openSettings = true
                    }
                    Divider()
                    BottomBar(large, selectedPanel, visiblePanels) {
                        selectedPanel = it
                    }
                }
            }
            if (openSettings) {
                LibrarySettings { openSettings = false }
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    visiblePanels: List<Panel>,
    library: Library?,
    openSettings: () -> Unit,
) {
    val cs = rememberCoroutineScope()
    PanelContainer(modifier, values().toSet(), visiblePanels) { panel ->
        when (panel) {
            LIBRARY -> LibraryContainer(library) { library ->
                var listOptions by remember(library) { mutableStateOf(SongListOptions()) }
                val lib = library.filterAndSort(listOptions)
                val items = lib.toListItems(listOptions)
                LibraryHeader(Modifier.fillMaxSize(), library, listOptions, { listOptions = it }, openSettings) {
                    SongListUI(lib.stats.maxTrackNumber, items) { song ->
                        cs.launch {
                            PlayerController.channel.send(
                                PlayerCommand.ChangeQueue(
                                    SongQueue.of(lib, song), Position.Beginning
                                )
                            )
                            PlayerController.channel.send(PlayerCommand.Play)
                        }
                    }
                }
            }

            QUEUE -> SongQueueUI(Modifier.fillMaxSize()) { queue ->
                cs.launch {
                    PlayerController.channel.send(PlayerCommand.ChangeQueue(queue, Position.Beginning))
                    PlayerController.channel.send(PlayerCommand.Play)
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
                    SingleLineText(panel.label, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun LibraryContainer(library: Library?, f: @Composable (Library) -> Unit) {
    if (library == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Loading...")
            Spacer(Modifier.weight(1f))
        }
    } else {
        f(library)
    }
}

fun main() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    application {
        Window(
            title = "Music Player",
            onCloseRequest = ::exitApplication,
            state = remember {
                WindowState(size = DpSize(1280.dp, 800.dp))
            }
        ) {
            App()
        }
    }
}