package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import audio.Position
import data.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import playerController
import utils.format
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerUI(
    modifier: Modifier,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    val queue = player.queue
    val song = queue?.currentSong
    Box(modifier.padding(horizontal = 16.dp)) {
        if (song == null) {
            BigMessage(
                Modifier.fillMaxSize(),
                Icons.Filled.PlayCircleFilled,
                "Play a song",
                "To begin, select a song from your library",
            )
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))

                AlbumCover(song.cover, Modifier.size(256.dp), MaterialTheme.shapes.large, elevation = 16.dp)
                Spacer(Modifier.height(24.dp))

                Text(song.title, style = MaterialTheme.typography.h2)
                Spacer(Modifier.height(8.dp))
                Text(song.album.title, style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(4.dp))
                Text(song.album.artist.name, style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerIcon(
                        cs, Icons.Default.Shuffle, "Shuffle", active = queue.isShuffled
                    ) {
                        player.changeQueue(queue.toggleShuffle())
                    }
                    Spacer(Modifier.width(16.dp))
                    PlayerIcon(
                        cs, Icons.Default.SkipPrevious, "Previous"
                    ) {
                        player.changeQueue(queue.previous(), Position.Beginning)
                        player.play()
                    }
                    Spacer(Modifier.width(8.dp))
                    PlayerIcon(
                        cs,
                        if (player.pause) Icons.Default.PlayArrow else Icons.Default.Pause,
                        if (player.pause) "Play" else "Pause",
                        iconModifier = Modifier.size(48.dp),
                    ) {
                        if (player.pause) {
                            player.play()
                        } else {
                            player.pause()
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    PlayerIcon(
                        cs, Icons.Default.SkipNext, "Next"
                    ) {
                        player.changeQueue(queue.next(), Position.Beginning)
                        player.play()
                    }
                    Spacer(Modifier.width(16.dp))
                    PlayerIcon(
                        cs,
                        if (queue.repeatMode == RepeatMode.REPEAT_SONG) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        active = queue.repeatMode != RepeatMode.DO_NOT_REPEAT
                    ) {
                        player.changeQueue(
                            queue.toggleRepeat()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = player.position.inWholeMilliseconds.toFloat(),
                    valueRange = 0f..song.length.inWholeMilliseconds.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                    ),
                    onValueChange = {
                        player.startSeek(queue, it.roundToInt().milliseconds)
                    },
                    onValueChangeFinished = {
                        cs.launch { player.endSeek() }
                    }
                )
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(player.position.format())
                    Spacer(Modifier.weight(1f))
                    Text((player.position - song.length).coerceAtMost(ZERO).format())
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlayerIcon(
    cs: CoroutineScope,
    icon: ImageVector,
    label: String,
    iconModifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = true,
    onClick: suspend CoroutineScope.() -> Unit
) {
    val alpha by animateFloatAsState(if (active) 1f else inactiveAlpha)
    BigIconButton({
        cs.launch {
            onClick()
        }
    }, enabled = enabled) {
        Icon(icon, label, iconModifier.padding(4.dp).alpha(alpha))
    }
}