package ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import data.Song
import data.SongListItem
import data.SongQueue

@Composable
fun SongQueueUI(
    queue: SongQueue,
    onSongSelected: (Song) -> Unit,
) {
    if (queue.songs.isEmpty()) {
        Text("Empty queue")
    } else {
        SongListUI(
            0,
            queue.songs.map { SongListItem.SingleSongListItem(it) },
            onSongSelected
        )
    }
}