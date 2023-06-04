package ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.Album
import data.Song

@Composable
fun AlbumRow(album: Album, songs: List<Song>, sideOffset: Int, onSongSelected: (Song) -> Unit) {
    BigSongRow(true, songs, sideOffset, onSongSelected) {
        AlbumCover(album.cover, Modifier.size(128.dp), MaterialTheme.shapes.medium)
        Spacer(Modifier.height(16.dp))
        Text(album.title, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(album.artist.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.subtitle1)
        Spacer(Modifier.height(8.dp))
        SongCollectionStatsComposable(album.stats)
    }
}