package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.Song

@Composable
fun BigSongRow(
    inAlbumContext:Boolean,
    songs: List<Song>,
    onSongSelected: (Song) -> Unit,
    sideContent: @Composable () -> Unit
) {
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
                sideContent()
            }
            Column {
                songs.forEach { song ->
                    SongRow(song, inAlbumContext = inAlbumContext, onSongSelected = { onSongSelected(song) })
                }
            }
        }
        Divider()
    }

}