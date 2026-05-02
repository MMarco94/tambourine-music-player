package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.audio.Position
import io.github.mmarco94.tambourine.audio.SongDecoder
import io.github.mmarco94.tambourine.data.RepeatMode
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

private val TOP_SPACER_HEIGHT_EXPANSION = 540.dp..700.dp
private val BOTTOM_SPACER_HEIGHT_EXPANSION = 700.dp..960.dp
private val ALBUM_COVER_HEIGHT_COLLAPSE = 960.dp..1920.dp
private val ALBUM_COVER_MIN_TOTAL_HEIGHT = 540.dp

@Composable
fun PlayerUI(
    showToolbar: Boolean,
    showLyrics: Boolean,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    setShowLyrics: (Boolean) -> Unit,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    val queue = player.queue
    val song = queue?.currentSong
    BoxWithConstraints {
        val totalHeight = this@BoxWithConstraints.maxHeight
        if (song == null) {
            Column {
                if (showToolbar) {
                    AppToolbar(openSettings = openSettings, closeApp = closeApp)
                }
                BigMessage(
                    Modifier.fillMaxSize(),
                    Icons.Filled.PlayCircleFilled,
                    stringResource(Res.string.help_play_a_song_title),
                    stringResource(Res.string.help_play_a_song_message),
                )
            }
        } else {
            if (showToolbar) {
                AppToolbar(
                    openSettings = openSettings,
                    closeApp = closeApp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (totalHeight > ALBUM_COVER_MIN_TOTAL_HEIGHT) {
                    val topSpacerHeight = 24.dp + 40.dp * TOP_SPACER_HEIGHT_EXPANSION.percent(totalHeight)
                    Spacer(Modifier.height(topSpacerHeight))
                    Box(Modifier.weight(3f), contentAlignment = Alignment.Center) {
                        val coverWeight = 1f - ALBUM_COVER_HEIGHT_COLLAPSE.percent(totalHeight) * .5f
                        CoverOrLyrics(Modifier.fillMaxHeight(coverWeight), song, showLyrics)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(song.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                SingleLineText(song.album.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                SingleLineText(song.album.artist.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                Column(Modifier.widthIn(max = 480.dp)) {
                    Seeker(Modifier, player, song)
                    SeekerTime(Modifier, player, song)
                }
                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.lyrics != null) {
                        Spacer(Modifier.width(56.dp))
                    }
                    ShuffleIcon(cs, queue)
                    Spacer(Modifier.width(16.dp))
                    PlayerIcon(
                        cs, Icons.Default.SkipPrevious, stringResource(Res.string.action_go_to_previous_song)
                    ) {
                        player.transformQueue { queue ->
                            queue?.previous() to Position.Beginning
                        }
                        player.play()
                    }
                    Spacer(Modifier.width(8.dp))
                    PlayerIcon(
                        cs,
                        if (player.pause) Icons.Default.PlayArrow else Icons.Default.Pause,
                        stringResource(if (player.pause) Res.string.action_play else Res.string.action_pause),
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
                        cs, Icons.Default.SkipNext, stringResource(Res.string.action_go_to_next_song)
                    ) {
                        player.transformQueue { queue ->
                            queue?.next() to Position.Beginning
                        }
                        player.play()
                    }
                    Spacer(Modifier.width(16.dp))
                    RepeatIcon(cs, queue)
                    if (song.lyrics != null) {
                        Spacer(Modifier.width(16.dp))
                        LyricsIcon(cs, showLyrics) { setShowLyrics(!showLyrics) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                VolumeSlider(player)
                Spacer(Modifier.height(24.dp))
                val bottomSpacerWeight = BOTTOM_SPACER_HEIGHT_EXPANSION.percent(totalHeight)
                if (bottomSpacerWeight > 0) {
                    Spacer(Modifier.weight(bottomSpacerWeight))
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CoverOrLyrics(modifier: Modifier, song: Song, showLyrics: Boolean) {
    val cs = rememberCoroutineScope()
    Box(
        modifier.padding(horizontal = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        AlbumContainer(Modifier.fillMaxHeight(), MaterialTheme.shapes.large, elevation = 16.dp) {
            FadeIn(
                targetState = song.cover to song,
                contentKey = { (cover, _) -> cover },
                duration = 300.milliseconds,
            ) { (cover, song) ->
                val lyricsSong = (if (showLyrics) song else null)
                val hasLyrics = lyricsSong?.lyrics != null
                val lyricToNormal by animateFloatAsState(if (hasLyrics) 1f else 0f)
                val blur = 16.dp * lyricToNormal
                val paper = 0.075f * lyricToNormal
                val saturation by animateFloatAsState(if (hasLyrics) 0.6f else 1f)
                val baseColor = song.cover?.colorPalette?.first() ?: TambourineTheme.getDefaultScheme().primary
                val bgColor = remember(baseColor) {
                    baseColor.hsb().makeContrasty().color
                }
                val bgColorAnimated by animateColorAsState(
                    bgColor.copy(if (cover == null) 1f else if (hasLyrics) 0.75f else 0f)
                )
                val contentColor by animateColorAsState(
                    bgColor.hsb().contrast().color
                )
                Box {
                    AlbumCoverContent(
                        cover,
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
                        fullResolutionSource = if (lyricToNormal < 1f) song else null,
                        modifier = Modifier.paperNoise(baseBgColor = bgColorAnimated, strength = paper).blur(blur)
                            .fillMaxSize(),
                    )
                    Crossfade(lyricsSong) { lyricsSong ->
                        if (lyricsSong?.lyrics != null) {
                            val player = playerController.current
                            CompositionLocalProvider(LocalContentColor provides contentColor) {
                                LyricsComposable(
                                    lyricsSong.lyrics,
                                    getPosition = { transform ->
                                        var pos by remember { mutableStateOf(transform(ZERO)) }
                                        player.ObservePosition {
                                            if (player.queue?.currentSongKey == lyricsSong.uniqueKey) {
                                                pos = transform(it)
                                            }
                                        }
                                        pos
                                    },
                                    setPosition = { position ->
                                        cs.launch {
                                            player.transformQueue { queue ->
                                                if (queue?.currentSongKey == lyricsSong.uniqueKey) {
                                                    queue to Position.Specific(position)
                                                } else
                                                    queue to Position.Current
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsIcon(
    cs: CoroutineScope,
    showLyrics: Boolean,
    onClick: () -> Unit,
) {
    PlayerIcon(
        cs,
        Icons.Default.Lyrics,
        label = stringResource(if (showLyrics) Res.string.action_hide_lyrics else Res.string.action_show_lyrics),
        active = showLyrics,
    ) {
        onClick()
    }
}

@Composable
fun ShuffleIcon(
    cs: CoroutineScope,
    queue: SongQueue
) {
    val player = playerController.current
    PlayerIcon(
        cs,
        Icons.Default.Shuffle,
        label = stringResource(if (queue.isShuffled) Res.string.action_disable_shuffle else Res.string.action_enable_shuffle),
        active = queue.isShuffled
    ) {
        player.transformQueue { queue ->
            queue?.toggleShuffle() to Position.Current
        }
    }
}

@Composable
fun RepeatIcon(
    cs: CoroutineScope,
    queue: SongQueue
) {
    val player = playerController.current
    val next = queue.repeatMode.next
    PlayerIcon(
        cs,
        if (queue.repeatMode == RepeatMode.REPEAT_SONG) Icons.Default.RepeatOne else Icons.Default.Repeat,
        label = stringResource(
            when (next) {
                RepeatMode.DO_NOT_REPEAT -> Res.string.action_repeat_none
                RepeatMode.REPEAT_QUEUE -> Res.string.action_repeat_queue
                RepeatMode.REPEAT_SONG -> Res.string.action_repeat_song
            }
        ),
        active = queue.repeatMode != RepeatMode.DO_NOT_REPEAT
    ) {
        player.transformQueue { queue ->
            queue?.setRepeatMode(next) to Position.Current
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Seeker(
    modifier: Modifier,
    player: PlayerController,
    song: Song
) {
    val cs = rememberCoroutineScope()
    var seeking by remember { mutableStateOf(false) }
    val sliderState = remember(song.length, player) {
        SliderState(
            valueRange = 0f..song.length.toFloat(DurationUnit.MILLISECONDS),
            onValueChangeFinished = {
                seeking = false
                cs.launch {
                    player.endSeek()
                }
            },
        ).apply {
            value = player.position(Clock.System.now()).toFloat(DurationUnit.MILLISECONDS)
        }
    }
    val wrappedSliderState by rememberUpdatedState(sliderState)
    player.ObservePosition {
        wrappedSliderState.value = it.toFloat(DurationUnit.MILLISECONDS)
    }
    sliderState.onValueChange = {
        val newPosition = it.roundToInt().milliseconds
        val startSeek = !seeking
        seeking = true
        cs.launch {
            if (startSeek) {
                player.startSeek()
            }
            player.transformQueue { queue ->
                if (queue?.currentSongKey == song.uniqueKey) {
                    queue to Position.Specific(newPosition)
                } else {
                    queue to Position.Current
                }
            }
        }

    }
    Slider(
        modifier = modifier,
        state = sliderState,
        track = { pos ->
            var mousePositionX by remember { mutableStateOf<Float?>(null) }
            WaveformUI(
                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
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
                player = player,
                activePercent = { pos.value / pos.valueRange.endInclusive },
                mousePercent = mousePositionX?.let { p ->
                    { size -> p / size.width }
                },
            )
        },
        thumb = { }
    )
}

@Composable
private fun SeekerTime(
    modifier: Modifier,
    player: PlayerController,
    song: Song,
) {
    Row(modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        val decimals by player.Position { it.decimalsRounded() }
        val songLength by rememberUpdatedState(song.length)
        val remainingDecimals by player.Position {
            (it - songLength).coerceAtMost(ZERO).decimalsRounded()
        }
        val style = MaterialTheme.typography.labelMedium
        SingleLineText(formatDuration(decimals), style = style)
        Spacer(Modifier.weight(1f))
        SingleLineText(formatDuration(remainingDecimals), style = style)
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
    activePercent: Density.(Size) -> Float,
    mousePercent: (Density.(Size) -> Float)?,
    player: PlayerController,
) {
    Column(modifier) {
        SingleWaveformUI({
            val wf by player.DecodedSongData()
            Triple(
                wf?.analyzedFrames ?: 0L,
                wf?.waveformsPerChannel?.first(),
                wf?.maxAmplitude ?: 1.0
            )
        }, false, activePercent, mousePercent)
        SingleWaveformUI({
            val wf by player.DecodedSongData()
            Triple(
                wf?.analyzedFrames ?: 0L,
                wf?.waveformsPerChannel?.getOrNull(1) ?: wf?.waveformsPerChannel?.first(),
                wf?.maxAmplitude ?: 1.0
            )
        }, true, activePercent, mousePercent)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SingleWaveformUI(
    wfData: @Composable () -> Triple<Long, DoubleArray?, Double>,
    invert: Boolean,
    activePercent: Density.(Size) -> Float,
    mousePercent: (Density.(Size) -> Float)?,
) {
    // This is the most efficient way to do this. 10/10 would do again
    val animations = remember { mutableStateOf(DoubleArray(SongDecoder.WAVEFORM_SIZE)) }
    waveformAnimations(wfData, animations)
    val modifier = Modifier.fillMaxWidth().height(waveformHeight)
    Box {
        ActualWaveform(modifier, { animations.value }, invert, activePercent, INACTIVE_ALPHA, .8f, INACTIVE_ALPHA)
        val t = updateTransition(mousePercent)
        t.Crossfade(contentKey = { it == null }, animationSpec = spring(stiffness = Spring.StiffnessLow)) { mp ->
            if (mp != null) {
                ActualWaveform(modifier, { animations.value }, invert, mp, 0.6f, .8f, 0f)
            }
        }
    }
}

@Composable
private fun waveformAnimations(
    /**
     * A triple of:
     *  - "update counter", a number that should change every time the waveform changes
     *  - the waveform, as a double array. THIS IS mutable (that's why "update counter" is necessary)
     *  - the scale
     */
    wfData: @Composable () -> Triple<Long, DoubleArray?, Double>,
    animationStates: MutableState<DoubleArray>,
) {
    val anims = remember {
        List(SongDecoder.WAVEFORM_SIZE) { Animatable(0f) }
    }
    val spring = remember { spring<Float>(stiffness = Spring.StiffnessVeryLow) }
    val wfdata = wfData()
    val cs = rememberCoroutineScope()
    LaunchedEffect(wfdata) {
        val (_, wf, scale) = wfdata
        val targetState = DoubleArray(SongDecoder.WAVEFORM_SIZE) { index ->
            val h = wf?.getOrZero(index)?.div(scale) ?: fakeHeight
            val baseHeight = fakeHeight / 2
            baseHeight + h * (1 - baseHeight)
        }
        anims.forEachIndexed { i, anim ->
            val targetValue = targetState[i].toFloat()
            if (anim.targetValue != targetValue) {
                cs.launch {
                    anim.animateTo(
                        targetValue = targetValue,
                        animationSpec = spring,
                    )
                }
            }
        }
    }
    animationStates.value = DoubleArray(SongDecoder.WAVEFORM_SIZE) {
        anims[it].value.toDouble()
    }
}

@Composable
private fun ActualWaveform(
    modifier: Modifier,
    animatedWaveform: () -> DoubleArray,
    invert: Boolean,
    mousePercent: Density.(Size) -> Float,
    startAlpha: Float,
    activeAlpha: Float,
    endAlpha: Float,
) {
    val color = LocalContentColor.current
    Spectrometer(
        modifier,
        animatedWaveform,
        { ceil(it.width / 2.dp.toPx()).roundToInt() },
        linear = true,
        invert = invert,
        brush = { size ->
            val s = color.copy(alpha = startAlpha)
            val a = color.copy(alpha = activeAlpha)
            val e = color.copy(alpha = endAlpha)
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
    val alpha by animateFloatAsState(if (active) 1f else INACTIVE_ALPHA)
    BigIconButton(size = size, {
        cs.launch {
            onClick()
        }
    }, enabled = enabled) {
        Icon(icon, label, iconModifier.padding(4.dp).alpha(alpha))
    }
}

@Composable
fun VolumeSlider(player: PlayerController) {
    val level = player.level
    val cs = rememberCoroutineScope()
    var nonZeroLevel by remember { mutableStateOf(level) }
    if (level > 0) {
        nonZeroLevel = level
    }
    var seeking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        PlayerIcon(
            cs,
            if (level == 0f) Icons.AutoMirrored.Default.VolumeOff else Icons.AutoMirrored.Default.VolumeDown,
            label = stringResource(Res.string.action_mute_volume)
        ) {
            player.setLevel(if (level == 0f) nonZeroLevel else 0f)
        }
        Slider(
            value = level,
            onValueChange = {
                val start = !seeking
                seeking = true
                cs.launch {
                    if (start) {
                        player.enterLowLatencyMode()
                    }
                    player.setLevel(it)
                }
            },
            onValueChangeFinished = {
                cs.launch {
                    player.exitLowLatencyMode()
                }
                seeking = false
            },
            modifier = Modifier.width(176.dp),
            colors = SliderDefaults.colors(
                thumbColor = LocalContentColor.current,
                activeTrackColor = LocalContentColor.current,
            ),
        )
        PlayerIcon(
            cs,
            Icons.AutoMirrored.Default.VolumeUp,
            label = stringResource(Res.string.action_max_volume),
        ) {
            player.setLevel(1f)
        }
    }
}