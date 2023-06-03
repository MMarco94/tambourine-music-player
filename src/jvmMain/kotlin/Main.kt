import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import audio.Album
import audio.Artist
import audio.Library
import audio.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ui.AlbumRow
import ui.BlurredFadeAlbumCover
import ui.PlayerUI
import ui.SongRow
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
    val artistSorter = compareBy<Artist> { it.name }
    val albumSorter = compareBy<Album> { it.title }
    val songSorter = compareBy<Song> { it.title }

    var groupByAlbum by remember { mutableStateOf(true) }
    var groupByArtist by remember { mutableStateOf(true) }
    val items: List<SongListItem> =
        generateList(library, artistSorter, albumSorter, songSorter, groupByArtist, groupByAlbum)


    MaterialTheme(colors = darkColors()) {
        Surface {
            Box {
                BlurredFadeAlbumCover(currentSong?.cover, Modifier.fillMaxSize())
                Row {
                    Box(Modifier.weight(1f).background(Color.Black.copy(alpha = 0.1f))) {
                        Column {
                            Row {
                                Checkbox(groupByArtist, { groupByArtist = it })
                                Text("Group by artist")
                            }
                            Row {
                                Checkbox(groupByAlbum, { groupByAlbum = it })
                                Text("Group by album")
                            }
                            SongList(items) {
                                currentSong = it
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
}

@Composable
private fun SongList(
    items: List<SongListItem>,
    onSongSelected: (Song) -> Unit,
) {
    Box {
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            items(items) { item ->
                when (item) {
                    is SongListItem.AlbumListItem -> {
                        AlbumRow(item.album, item.songs, onSongSelected)
                    }

                    is SongListItem.SingleSongListItem -> {
                        SongRow(item.song) { onSongSelected(item.song) }
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

private fun generateList(
    library: Library,
    artistSorter: Comparator<Artist>,
    albumSorter: Comparator<Album>,
    songSorter: Comparator<Song>,
    groupByArtist: Boolean,
    groupByAlbum: Boolean,
): List<SongListItem> {
    if (groupByAlbum) {
        val albums: List<Album> = if (groupByArtist) {
            library.artists.keys.sortedWith(artistSorter)
                .flatMap { library.artists.getValue(it).sortedWith(albumSorter) }
        } else {
            library.albums.keys.sortedWith(albumSorter)
        }
        return albums.map { album ->
            SongListItem.AlbumListItem(
                album,
                library.albums.getValue(album).sortedWith(compareBy<Song> { it.track }.then(songSorter)),
            )
        }
    } else {
        val songs: List<Song> = if (groupByArtist) {
            library.artists.keys.sortedWith(artistSorter)
                .flatMap { artist ->
                    library.artists.getValue(artist)
                        .flatMap { library.albums.getValue(it) }
                        .sortedWith(songSorter)
                }
        } else {
            library.songs.sortedWith(songSorter)
        }
        return songs.map { SongListItem.SingleSongListItem(it) }
    }
}

sealed interface SongListItem {
    data class AlbumListItem(val album: Album, val songs: List<Song>) : SongListItem
    data class SingleSongListItem(
        val song: Song
    ) : SongListItem
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
