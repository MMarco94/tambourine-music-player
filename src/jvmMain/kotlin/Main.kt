import Panels.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ui.*
import java.io.File


val musicDirectory = File("/home/marco/Music")
//val musicDirectory = File("/home/marco/Music/Coldplay - Mylo Xyloto")

private enum class Panels(
    val icon: ImageVector,
    val label: String,
) {
    LIBRARY(Icons.Filled.LibraryMusic, "Library"),
    QUEUE(Icons.Filled.QueueMusic, "Queue"),
    PLAYER(Icons.Filled.PlayCircleFilled, "Player"),
}

@Composable
@Preview
fun App() {
    var queue by remember { mutableStateOf(SongQueue(emptyList())) }
    val library = remember {
        runBlocking(Dispatchers.IO) {
            Library.fromFolder(musicDirectory)
        }
    }

    var listOptions by remember { mutableStateOf(SongListOptions()) }
    val lib = library.filterAndSort(listOptions)
    val items = lib.toListItems(listOptions)
    var currentSong: Song? by remember { mutableStateOf(null) }

    var selectedPanel by remember { mutableStateOf(LIBRARY) }

    MaterialTheme(
        typography = MusicPlayerTheme.typography,
        colors = MusicPlayerTheme.colors,
        shapes = MusicPlayerTheme.shapes,
    ) {
        Surface {
            BlurredFadeAlbumCover(currentSong?.cover, Modifier.fillMaxSize())
            BoxWithConstraints {
                val w = constraints.maxWidth
                val large = w >= with(LocalDensity.current) { (BIG_SONG_ROW_DESIRED_WIDTH * 2).toPx() }
                val visiblePanels = if (large) {
                    listOf(selectedPanel, PLAYER).distinct()
                } else {
                    listOf(selectedPanel)
                }
                Column {
                    PanelContainer(
                        Modifier.fillMaxWidth().weight(1f),
                        Panels.values().toSet(),
                        visiblePanels
                    ) { panel ->
                        RenderPanel(
                            Modifier.fillMaxSize(),
                            panel, library, listOptions, lib, items, queue, currentSong,
                            { listOptions = it },
                            { currentSong = it },
                            { queue = it },
                        )
                    }
                    Divider()
                    val doFancy = large && selectedPanel != PLAYER
                    Row(Modifier.height(IntrinsicSize.Max)) {
                        Panels.values().forEach { panel ->
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
                                            selectedPanel = if (large && selectedPanel == panel) PLAYER else panel
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
            }
        }
    }
}

@Composable
private fun RenderPanel(
    modifier: Modifier,
    panel: Panels,
    library: Library,
    listOptions: SongListOptions,
    lib: Library,
    items: List<SongListItem>,
    queue: SongQueue,
    currentSong: Song?,
    setOptions: (SongListOptions) -> Unit,
    setSong: (Song?) -> Unit,
    setQueue: (SongQueue) -> Unit,
) {
    when (panel) {
        LIBRARY -> SongListOptionsController(modifier, library, listOptions, setOptions) {
            SongListUI(lib.stats.maxTrackNumber, items) {
                setQueue(SongQueue(lib.songs))
                setSong(it)
            }
        }

        QUEUE -> SongQueueUI(modifier, queue) {
            setSong(it)
        }

        PLAYER -> PlayerUI(modifier, currentSong)
    }
}

fun main() = application {
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