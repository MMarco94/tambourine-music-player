package io.github.musicplayer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.musicplayer.audio.PlayerController
import io.github.musicplayer.audio.Position
import io.github.musicplayer.audio.Waveform
import io.github.musicplayer.data.RepeatMode
import io.github.musicplayer.data.Song
import io.github.musicplayer.data.SongQueue
import io.github.musicplayer.playerController
import io.github.musicplayer.utils.format
import io.github.musicplayer.utils.getOrZero
import io.github.musicplayer.utils.progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
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

                Text(song.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                SingleLineText(song.album.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                SingleLineText(song.album.artist.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                Seeker(Modifier.widthIn(max = 480.dp), player, song, queue, cs)
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShuffleIcon(cs, queue)
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
                        size = 48.dp,
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
                    RepeatIcon(cs, queue)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ShuffleIcon(
    cs: CoroutineScope,
    queue: SongQueue
) {
    val player = playerController.current
    PlayerIcon(
        cs, Icons.Default.Shuffle, "Shuffle", active = queue.isShuffled
    ) {
        player.changeQueue(queue.toggleShuffle())
    }
}

@Composable
fun RepeatIcon(
    cs: CoroutineScope,
    queue: SongQueue
) {
    val player = playerController.current
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Seeker(
    modifier: Modifier,
    player: PlayerController,
    song: Song,
    queue: SongQueue?,
    cs: CoroutineScope
) {
    var mousePositionX by remember { mutableStateOf<Float?>(null) }
    Column(modifier) {
        val thumbRadius = 10.dp
        Slider(
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type == PointerEventType.Move) {
                            mousePositionX = e.changes.last().position.x
                        } else if (e.type == PointerEventType.Exit) {
                            mousePositionX = null
                        }
                    }
                }
            },
            value = player.position.inWholeMilliseconds.toFloat(),
            valueRange = 0f..song.length.inWholeMilliseconds.toFloat(),
            onValueChange = {
                player.startSeek(queue, it.roundToInt().milliseconds)
            },
            onValueChangeFinished = {
                cs.launch { player.endSeek() }
            },
            track = { pos ->
                val p = mousePositionX
                WaveformUI(
                    Modifier.fillMaxWidth(),
                    player.waveform,
                    { pos.positionFraction },
                    if (p == null) null else { size -> (p - thumbRadius.toPx()) / size.width },
                )
            },
            thumb = {
                val animProgress by animateFloatAsState(
                    if (player.waveform == null) 1f else 0f,
                    spring(stiffness = Spring.StiffnessVeryLow)
                )
                Surface(
                    Modifier.size(thumbRadius * 2)
                        .alpha(animProgress)
                        .scale((0.2f..1f).progress(animProgress)),
                    color = Color.White,
                    shape = CircleShape,
                ) {}
            }
        )
        Row(Modifier.padding(horizontal = thumbRadius), verticalAlignment = Alignment.CenterVertically) {
            val style = MaterialTheme.typography.labelMedium
            SingleLineText(player.position.format(), style = style)
            Spacer(Modifier.weight(1f))
            SingleLineText((player.position - song.length).coerceAtMost(ZERO).format(), style = style)
        }
    }
}

private val waveformHeight = 32.dp
val fakeHeight = run {
    val seekerTrackHeight = 4.dp // See TrackHeight in Seeker.kt
    seekerTrackHeight / waveformHeight / 2.0
}

@Composable
private fun WaveformUI(
    modifier: Modifier,
    wf: Waveform?,
    activePercent: Density.(Size) -> Float,
    mousePercent: (Density.(Size) -> Float)?,
) {
    val left: DoubleArray? = wf?.summaryChannel?.first()
    val right: DoubleArray? = wf?.summaryChannel?.getOrNull(1) ?: left
    Column(modifier) {
        SingleWaveformUI(left, false, activePercent, mousePercent)
        SingleWaveformUI(right, true, activePercent, mousePercent)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SingleWaveformUI(
    wf: DoubleArray?,
    invert: Boolean,
    activePercent: Density.(Size) -> Float,
    mousePercent: (Density.(Size) -> Float)?,
) {
    // This is the most efficient way to do this. 10/10 would do again
    val transition = updateTransition(wf, label = "Waveform")
    val spring = spring<Float>(stiffness = Spring.StiffnessVeryLow)
    val animations = (0 until Waveform.summaryLength).map { index ->
        transition.animateFloat({ spring }) { state ->
            val h = state?.getOrZero(index) ?: fakeHeight
            val baseHeight = fakeHeight / 2
            (baseHeight + h * (1 - baseHeight)).toFloat()
        }
    }
    val animatedWaveform = DoubleArray(Waveform.summaryLength) { index ->
        animations[index].value.toDouble()
    }
    val modifier = Modifier.fillMaxWidth().height(waveformHeight)
    Box {
        ActualWaveform(modifier, animatedWaveform, invert, activePercent, inactiveAlpha, .8f, inactiveAlpha)
        val t = updateTransition(mousePercent)
        t.Crossfade(contentKey = { it == null }, animationSpec = spring(stiffness = Spring.StiffnessLow)) { mp ->
            if (mp != null) {
                ActualWaveform(modifier, animatedWaveform, invert, mp, 0.6f, .8f, 0f)
            }
        }
    }
}

@Composable
private fun ActualWaveform(
    modifier: Modifier,
    animatedWaveform: DoubleArray,
    invert: Boolean,
    mousePercent: Density.(Size) -> Float,
    startAlpha: Float,
    activeAlpha: Float,
    endAlpha: Float,
) {
    Spectrometer(
        modifier,
        animatedWaveform,
        { ceil(it.width / 2.dp.toPx()).roundToInt() },
        linear = true,
        invert = invert,
        brush = { size ->
            val s = Color.White.copy(alpha = startAlpha)
            val a = Color.White.copy(alpha = activeAlpha)
            val e = Color.White.copy(alpha = endAlpha)
            val mp = mousePercent(this, size)
            Brush.horizontalGradient(
                0f to s,
                mp to a,
                mp to e,
            )
        }
    )
}


@Composable
private fun PlayerIcon(
    cs: CoroutineScope,
    icon: ImageVector,
    label: String,
    iconModifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    active: Boolean = true,
    onClick: suspend CoroutineScope.() -> Unit
) {
    val alpha by animateFloatAsState(if (active) 1f else inactiveAlpha)
    BigIconButton(size = size, {
        cs.launch {
            onClick()
        }
    }, enabled = enabled) {
        Icon(icon, label, iconModifier.padding(4.dp).alpha(alpha))
    }
}