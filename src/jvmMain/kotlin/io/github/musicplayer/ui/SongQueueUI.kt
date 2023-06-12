package io.github.musicplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.SongListItem
import io.github.musicplayer.data.SongQueue
import io.github.musicplayer.playerController
import io.github.musicplayer.utils.format
import io.github.musicplayer.utils.sumOfDuration
import kotlin.math.roundToInt

@Composable
fun SongQueueUI(
    modifier: Modifier,
    play: (SongQueue) -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    val cs = rememberCoroutineScope()
    if (queue == null) {
        BigMessage(
            modifier,
            Icons.Filled.QueueMusic,
            "Empty queue",
            "To begin, select a song from your library",
        )
    } else {
        Column(modifier) {
            Row(
                Modifier.heightIn(min = 64.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    // TODO: plurals
                    SingleLineText(
                        "${queue.songs.size} songs in the queue • ${
                            queue.songs.sumOfDuration { it.length }.format()
                        }", style = MaterialTheme.typography.bodyMedium
                    )
                    val remaining = queue.remainingSongs
                    SingleLineText(
                        "${remaining.size} songs remaining • ${
                            remaining.sumOfDuration { it.length }.format()
                        }", style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.weight(1f))
                RepeatIcon(cs, queue)
                ShuffleIcon(cs, queue)
            }
            Divider()
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val approxRowHeight = 64.dp
                val pos = queue.position
                val offset = ((approxRowHeight.toPxApprox() - constraints.maxHeight) / 2).roundToInt()
                val listState = rememberLazyListState(pos, offset)
                LaunchedEffect(pos) {
                    listState.animateScrollToItem(pos, offset)
                }
                SongListUI(
                    0,
                    queue.songs.map { SongListItem.SingleSongListItem(it) },
                    listState,
                ) {
                    play(queue.skipTo(it))
                }
            }
        }
    }
}