package io.github.musicplayer.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.Album
import io.github.musicplayer.data.Song

@Composable
fun AlbumRow(
    maxTrackNumber: Int?,
    album: Album, songs: List<Song>, sideOffset: Int, onSongSelected: (Song) -> Unit,
) {
    BigSongRow(maxTrackNumber, true, songs, sideOffset, onSongSelected) {
        AlbumCover(album.cover, Modifier.size(128.dp), MaterialTheme.shapes.medium, elevation = 8.dp)
        Spacer(Modifier.height(8.dp))

        Text(album.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.subtitle1)
        Text(album.artist.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(8.dp))

        SongCollectionStatsComposable(album.stats)
    }
}