package io.github.musicplayer.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.SongListItem
import io.github.musicplayer.data.SongQueue
import io.github.musicplayer.playerController
import kotlin.math.roundToInt

@Composable
fun SongQueueUI(
    modifier: Modifier,
    play: (SongQueue) -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    BoxWithConstraints(modifier) {
        if (queue == null) {
            BigMessage(
                Modifier.fillMaxSize(),
                Icons.Filled.QueueMusic,
                "Empty queue",
                "To begin, select a song from your library",
            )
        } else {
            val padding = 128.dp
            val approxRowHeight = 64.dp
            val pos = queue.position
            val offset =
                (-(constraints.maxHeight - padding.toPxApprox()) / 2 + approxRowHeight.toPxApprox()).roundToInt()
            val listState = rememberLazyListState(pos, offset)
            LaunchedEffect(pos) {
                listState.animateScrollToItem(pos, offset)
            }
            SongListUI(
                0,
                queue.songs.map { SongListItem.SingleSongListItem(it) },
                listState,
                contentPadding = PaddingValues(vertical = padding),
            ) {
                play(queue.skipTo(it))
            }
        }
    }
}