package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.mmarco94.tambourine.audio.Position
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.playerController
import kotlinx.coroutines.launch

@Composable
fun PlayShuffleButtons(
    modifier: Modifier = Modifier,
    songs: List<Song>,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    Row(modifier) {
        IconButtonWithBG({
            cs.launch {
                player.changeQueue(SongQueue.of(songs, songs.first()), Position.Beginning)
                player.play()
            }
        }) {
            Icon(Icons.Default.PlayArrow, "Play")
        }
        IconButtonWithBG({
            cs.launch {
                player.changeQueue(SongQueue.of(songs, songs.random()).shuffled(), Position.Beginning)
                player.play()
            }
        }) {
            Icon(Icons.Default.Shuffle, "Shuffle")
        }
    }
}