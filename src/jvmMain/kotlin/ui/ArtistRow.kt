package ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.Artist
import data.Song

@Composable
fun ArtistRow(artist: Artist, songs: List<Song>, sideOffset: Int, onSongSelected: (Song) -> Unit) {
    BigSongRow(false, songs, sideOffset, onSongSelected) {
        Text(artist.name, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        SongCollectionStatsComposable(artist.stats)
    }
}