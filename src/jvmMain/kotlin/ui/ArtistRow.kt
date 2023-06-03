package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.Album
import data.Artist
import data.Song

@Composable
fun ArtistRow(artist: Artist, songs: List<Song>, onSongSelected: (Song) -> Unit) {
    Column {
        Row(Modifier) {
            Column(
                Modifier.width(128.dp + 32.dp).padding(
                    top = 16.dp,
                    bottom = 16.dp,
                    start = 16.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(artist.name, textAlign = TextAlign.Center)
            }
            Column {
                songs.forEach { song ->
                    SongRow(song, inAlbumContext = false, onSongSelected = { onSongSelected(song) })
                }
            }
        }
        Divider()
    }
}