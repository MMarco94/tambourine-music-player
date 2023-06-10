package io.github.musicplayer.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.Artist
import io.github.musicplayer.data.Song

@Composable
fun ArtistRow(
    maxTrackNumber: Int?,
    artist: Artist, songs: List<Song>, sideOffset: Int, onSongSelected: (Song) -> Unit
) {
    BigSongRow(maxTrackNumber, false, songs, sideOffset, onSongSelected) {
        Text(artist.name, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        SongCollectionStatsComposable(artist.stats)
    }
}