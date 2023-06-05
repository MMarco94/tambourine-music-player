package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.Song
import data.SongListItem
import data.SongQueue

@Composable
fun SongQueueUI(
    modifier: Modifier,
    onSongSelected: (Song) -> Unit,
) {
    val songs = SongQueue.songs
    Box(modifier) {
        if (songs.isEmpty()) {
            Text("Empty queue")
        } else {
            SongListUI(
                0,
                songs.map { SongListItem.SingleSongListItem(it) },
                onSongSelected
            )
        }
    }
}