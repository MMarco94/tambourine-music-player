package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.mmarco94.tambourine.generated.resources.Res
import io.github.mmarco94.tambourine.generated.resources.action_play
import io.github.mmarco94.tambourine.generated.resources.action_shuffle_from_here
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayShuffleButtons(
    modifier: Modifier = Modifier,
    controller: SongQueueController,
    songs: List<Song>,
) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Row(modifier) {
            IconButtonWithBG({
                controller.playSongs(songs, null)
            }) {
                Icon(Icons.Default.PlayArrow, stringResource(Res.string.action_play))
            }
            IconButtonWithBG({
                controller.playSongs(songs, null, shuffle = true)
            }) {
                Icon(Icons.Default.Shuffle, stringResource(Res.string.action_shuffle_from_here))
            }
        }
    }
}