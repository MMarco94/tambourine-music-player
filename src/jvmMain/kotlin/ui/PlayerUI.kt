package ui

import audio.PlayerCommand
import audio.PlayerController
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import audio.Position
import kotlinx.coroutines.launch
import rounded
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerUI(
    modifier: Modifier,
) {
    val cs = rememberCoroutineScope()
    val queue = PlayerController.queue
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

                AlbumCover(song.cover, Modifier.size(256.dp), MaterialTheme.shapes.large)
                Spacer(Modifier.height(16.dp))

                Text(song.title, style = MaterialTheme.typography.h2)
                Spacer(Modifier.height(8.dp))
                Text(song.album.title, style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(4.dp))
                Text(song.album.artist.name, style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val prev = PlayerController.queue?.previous()
                    val next = PlayerController.queue?.next()
                    IconButton({
                        cs.launch {
                            PlayerController.channel.send(PlayerCommand.ChangeQueue(prev, Position.Beginning))
                            PlayerController.channel.send(PlayerCommand.Play)
                        }
                    }, enabled = prev?.currentSong != null) {
                        Icon(Icons.Default.SkipPrevious, "Previous")
                    }
                    Spacer(Modifier.width(8.dp))
                    BigIconButton({
                        cs.launch {
                            PlayerController.channel.send(if (PlayerController.pause) PlayerCommand.Play else PlayerCommand.Pause)
                        }
                    }) {
                        val m = Modifier.size(48.dp).padding(4.dp)
                        if (PlayerController.pause) {
                            Icon(Icons.Default.PlayArrow, "Play", m)
                        } else {
                            Icon(Icons.Default.Pause, "Pause", m)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton({
                        cs.launch {
                            PlayerController.channel.send(PlayerCommand.ChangeQueue(next, Position.Beginning))
                            PlayerController.channel.send(PlayerCommand.Play)
                        }
                    }, enabled = next?.currentSong != null) {
                        Icon(Icons.Default.SkipNext, "Next")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = PlayerController.position.inWholeMilliseconds.toFloat(),
                    valueRange = 0f..song.length.inWholeMilliseconds.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                    ),
                    onValueChange = {
                        PlayerController.seek(cs, queue, it.roundToInt().milliseconds)
                    }
                )
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(PlayerController.position.rounded().toString())
                    Spacer(Modifier.weight(1f))
                    Text("-" + (song.length - PlayerController.position).rounded())
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}