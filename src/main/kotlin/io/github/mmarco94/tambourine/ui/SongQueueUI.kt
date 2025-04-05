package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.utils.format
import io.github.mmarco94.tambourine.utils.sumOfDuration
import kotlin.math.roundToInt

@Composable
fun SongQueueUI(
    modifier: Modifier,
    sortedLibrary: Library,
    showSettingsButton: Boolean,
    openSettings: () -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    val cs = rememberCoroutineScope()
    if (queue == null) {
        BigMessage(
            modifier,
            Icons.AutoMirrored.Default.QueueMusic,
            "Empty queue",
            "To begin, select a song from your library",
        )
    } else {
        val controller = SongQueueController(cs, queue.originalSongs, sortedLibrary, player)
        Column(modifier) {
            Row(
                Modifier.heightIn(min = 64.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(16.dp))
                SmallFakeSpectrometers(
                    Modifier.size(32.dp),
                    player,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
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
                Spacer(Modifier.width(16.dp))
                RepeatIcon(cs, queue)
                ShuffleIcon(cs, queue)
                if (showSettingsButton) {
                    SettingsButton(Modifier, openSettings)
                }
            }
            HorizontalDivider()
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
                    queue.songs.mapIndexed { index, it -> SongListItem.QueuedSongListItem(index, it) },
                    listState,
                    controller,
                )
            }
        }
    }
}