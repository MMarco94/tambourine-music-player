package io.github.musicplayer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.musicplayer.audio.Position
import io.github.musicplayer.data.Song
import io.github.musicplayer.data.SongQueue
import io.github.musicplayer.playerController
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