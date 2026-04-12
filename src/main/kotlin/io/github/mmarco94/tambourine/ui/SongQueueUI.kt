package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.utils.format
import io.github.mmarco94.tambourine.utils.sumOfDuration
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SongQueueUI(
    sortedLibrary: Library,
    showToolbar: Boolean,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    val cs = rememberCoroutineScope()
    if (queue == null) {
        WindowDraggableArea {
            Column {
                if (showToolbar) {
                    AppToolbar(openSettings = openSettings, closeApp = closeApp)
                }
                BigMessage(
                    Modifier.fillMaxSize(),
                    Icons.AutoMirrored.Default.QueueMusic,
                    stringResource(Res.string.empty_queue),
                    stringResource(Res.string.help_play_a_song_message),
                )
            }
        }
    } else {
        val controller = SongQueueController(cs, sortedLibrary, queue.originalSongs, player)
        Column(Modifier.fillMaxSize()) {
            WindowDraggableArea {
                Row(
                    Modifier.heightIn(min = 64.dp).padding(vertical = 8.dp).padding(end = 8.dp),
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
                        val songsInQueueStr =
                            pluralStringResource(Res.plurals.n_songs_in_queue, queue.songs.size, queue.songs.size)
                        val totalLength = queue.songs.sumOfDuration { queue.songsByKey.getValue(it).length }.format()
                        SingleLineText("$songsInQueueStr • $totalLength", style = MaterialTheme.typography.bodyMedium)

                        val remaining = queue.remainingSongs
                        val songsRemainingStr =
                            pluralStringResource(Res.plurals.n_songs_remaining, remaining.size, remaining.size)
                        val remainingLength = remaining.sumOfDuration { queue.songsByKey.getValue(it).length }.format()
                        SingleLineText(
                            "$songsRemainingStr • $remainingLength", style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    RepeatIcon(cs, queue)
                    ShuffleIcon(cs, queue)
                    if (showToolbar) {
                        AppToolbar(
                            openSettings = openSettings,
                            closeApp = closeApp,
                            modifier = Modifier,
                            autoSize = false
                        )
                    }
                }
            }
            HorizontalDivider()
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val items = queue.songs.mapIndexed { index, it ->
                    SongListItem.QueuedSongListItem(index, queue.songsByKey.getValue(it))
                }
                val state = rememberLazySongListState(maxHeight, items, tryNotToScroll = false)
                SongListUI(
                    0,
                    items,
                    { state },
                    controller,
                )
            }
        }
    }
}