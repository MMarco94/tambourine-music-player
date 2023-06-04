import Panels.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var currentSong by remember { mutableStateOf(lib.songs.firstOrNull()) }

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
                val panels = if (w >= with(LocalDensity.current) { (BIG_SONG_ROW_DESIRED_WIDTH*2).toPx() }) {
                    listOf(selectedPanel, PLAYER).distinct()
                } else {
                    listOf(selectedPanel)
                }
                Column {
                    PanelContainer(Modifier.fillMaxWidth().weight(1f), panels) { panel ->
                        when (panel) {
                            LIBRARY -> SongListOptionsController(library, listOptions, { listOptions = it }) {
                                SongListUI(lib.stats.maxTrackNumber, items) {
                                    queue = SongQueue(lib.songs)
                                    currentSong = it
                                }
                            }

                            QUEUE -> SongQueueUI(queue) {
                                currentSong = it
                            }

                            PLAYER -> PlayerUI(currentSong)
                        }
                    }
                    Row(
                        Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .height(IntrinsicSize.Max)
                            .padding(horizontal = 4.dp).padding(top = 4.dp)
                    ) {
                        Panels.values().forEach { panel ->
                            Surface(
                                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 4.dp).weight(1f).fillMaxHeight(),
                                color = if (panel == selectedPanel) MaterialTheme.colors.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.medium.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .clickable { selectedPanel = panel }
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