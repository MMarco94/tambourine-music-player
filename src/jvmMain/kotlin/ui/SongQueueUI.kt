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
    queue: SongQueue,
    onSongSelected: (Song) -> Unit,
) {
    Box(modifier) {
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
}