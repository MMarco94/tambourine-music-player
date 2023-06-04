import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
@Preview
fun App() {
    val library = remember {
        runBlocking(Dispatchers.IO) {
            Library.fromFolder(musicDirectory)
        }
    }

    var currentSong by remember { mutableStateOf(library.songs.firstOrNull()) }
    var listOptions by remember { mutableStateOf(SongListOptions()) }
    val lib = library.filterAndSort(listOptions)
    val items = lib.toListItems(listOptions)

    MaterialTheme(
        typography = MusicPlayerTheme.typography,
        colors = MusicPlayerTheme.colors,
        shapes = MusicPlayerTheme.shapes,
    ) {
        Surface {
            BlurredFadeAlbumCover(currentSong?.cover, Modifier.fillMaxSize())
            Row {
                Box(
                    Modifier.weight(1f).background(Color.Black.copy(alpha = 0.1f))
                ) {
                    Column {
                        SongListOptionsController(library, listOptions,{ listOptions = it }) {
                            SongList(lib, items) {
                                currentSong = it
                            }
                        }
                    }
                }
                if (currentSong != null) {
                    Box(Modifier.weight(1f)) {
                        PlayerUI(currentSong!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongList(
    lib: Library,
    items: List<SongListItem>,
    onSongSelected: (Song) -> Unit,
) {
    Box {
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items) { index, item ->
                val offset = if (index == state.firstVisibleItemIndex) state.firstVisibleItemScrollOffset else 0
                when (item) {
                    is SongListItem.ArtistListItem -> {
                        ArtistRow(lib.stats.maxTrackNumber, item.artist, item.songs, offset, onSongSelected)
                    }

                    is SongListItem.AlbumListItem -> {
                        AlbumRow(lib.stats.maxTrackNumber, item.album, item.songs, offset, onSongSelected)
                    }

                    is SongListItem.SingleSongListItem -> {
                        SongRow(lib.stats.maxTrackNumber, item.song) { onSongSelected(item.song) }
                    }
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.Companion.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
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
