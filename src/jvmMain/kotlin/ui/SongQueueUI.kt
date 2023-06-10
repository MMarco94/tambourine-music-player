package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.SongListItem
import data.SongQueue
import playerController

@Composable
fun SongQueueUI(
    modifier: Modifier,
    play: (SongQueue) -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    Box(modifier) {
        if (queue == null) {
            BigMessage(
                Modifier.fillMaxSize(),
                Icons.Filled.QueueMusic,
                "Empty queue",
                "To begin, select a song from your library",
            )
        } else {
            SongListUI(
                0,
                queue.songs.map { SongListItem.SingleSongListItem(it) }
            ) {
                play(queue.skipTo(it))
            }
        }
    }
}