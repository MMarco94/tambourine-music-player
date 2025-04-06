package io.github.mmarco94.tambourine.audio

import androidx.compose.runtime.*
import io.github.mmarco94.tambourine.audio.PlayerCommand.*
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.mpris.MPRISPlayerController
import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.swing.Swing
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

private val BUFFER = 10.seconds
val LOW_LATENCY_BUFFER = 100.milliseconds
private val SONG_SWITCH_THRESHOLD = 100.milliseconds

sealed interface Position {
    data object Current : Position
    data object Beginning : Position
    data class Specific(val time: Duration) : Position
}

private sealed interface PlayerCommand {
    class ChangeQueue(
        val queue: SongQueue?,
        val position: Position,
    ) : PlayerCommand

    data object Play : PlayerCommand
    data object Pause : PlayerCommand
    data object SeekingStart : PlayerCommand
    data object SeekDone : PlayerCommand
    data object EnterLowLatencyMode : PlayerCommand
    data object ExitLowLatencyMode : PlayerCommand
    class SetLevel(val level: Float) : PlayerCommand
    class WaveformComputed(val song: Song, val waveform: Waveform) : PlayerCommand
}

class PlayerController(
    private val coroutineScope: CoroutineScope,
    quit: () -> Unit,
    raise: () -> Unit,
) {

    private val commandChannel: Channel<PlayerCommand> = Channel(Channel.UNLIMITED)
    private val stateChannel = Channel<State>(Channel.CONFLATED)
    private val mprisPlayer = run {
        try {
            MPRISPlayerController(
                coroutineScope,
                this,
                quit = quit,
                raise = raise,
            )
        } catch (e: Exception) {
            logger.error { "Error creating MPRIS: ${e.message}" }
            null
        }
    }

    data class CurrentlyPlaying(
        val queue: SongQueue,
        val player: Player,
        val bufferer: Job,
        val waveformCreator: Job,
        val waveform: Waveform? = null,
    )

    data class PositionState(
        val position: Duration,
        val informationAge: Instant = Clock.System.now(),
    )

    data class State(
        val currentlyPlaying: CurrentlyPlaying?,
        val position: PositionState,
        val pause: Boolean,
        val seeking: Boolean,
        val lowLatencyMode: Boolean,
        val level: Float,
    ) {

        companion object {
            val initial = State(
                currentlyPlaying = null,
                position = PositionState(ZERO),
                pause = true,
                seeking = false,
                lowLatencyMode = false,
                level = 1f,
            )
        }

        data class StateChangeResult(
            val newState: State,
            val maxAllowedPause: Duration,
        )

        inline fun change(f: State.() -> State) = StateChangeResult(
            this.f(), ZERO
        )

        fun calculateCurrentPosition(now: Instant): Duration {
            val actualPosition = position.position
            if (pause || currentlyPlaying == null) {
                return actualPosition
            }
            val elapsed = (now - position.informationAge)
                .coerceAtMost(currentlyPlaying.queue.currentSong.length - actualPosition)
                .coerceAtLeast(ZERO)
            return actualPosition + elapsed
        }

        suspend fun play(
            cs: CoroutineScope,
            onWaveformComputed: suspend (Song, Waveform) -> Unit,
        ): StateChangeResult {
            return if (currentlyPlaying != null && !pause) {
                val bufferingCap = if (lowLatencyMode) LOW_LATENCY_BUFFER / 2 else null
                val result = currentlyPlaying.player.playFrame(bufferingCap)
                if (
                    result == Player.PlayResult.Finished &&
                    !seeking &&
                    currentlyPlaying.player.pendingFlush() < SONG_SWITCH_THRESHOLD
                ) {
                    val (next, keepPlaying) = currentlyPlaying.queue.nextInQueue()
                    changeQueue(
                        cs = cs,
                        queue = next,
                        position = Position.Beginning,
                        keepBufferedContent = true,
                        onWaveformComputed = onWaveformComputed,
                    ).change { copy(pause = this.pause || !keepPlaying) }
                } else {
                    val maxAllowedPause = when (result) {
                        is Player.PlayResult.Played -> ZERO
                        is Player.PlayResult.NotPlayed -> (bufferingCap ?: BUFFER) / 10
                        is Player.PlayResult.Finished -> {
                            // I'll resume playing only when the "if" above will become true
                            if (seeking) {
                                Duration.INFINITE
                            } else {
                                currentlyPlaying.player.pendingFlush() - SONG_SWITCH_THRESHOLD
                            }
                        }
                    }
                    StateChangeResult(
                        copy(position = PositionState(currentlyPlaying.player.position)),
                        maxAllowedPause = maxAllowedPause,
                    )
                }
            } else {
                StateChangeResult(this, Duration.INFINITE)
            }
        }

        suspend fun changeQueue(
            cs: CoroutineScope,
            queue: SongQueue?,
            position: Position,
            keepBufferedContent: Boolean,
            onWaveformComputed: suspend (Song, Waveform) -> Unit,
        ): State {
            if (queue == null) {
                currentlyPlaying?.apply {
                    player.stop()
                    bufferer.cancel()
                    waveformCreator.cancel()
                }
                return State(
                    currentlyPlaying = null,
                    position = PositionState(ZERO),
                    pause = true,
                    seeking = seeking,
                    lowLatencyMode = lowLatencyMode,
                    level = level
                )
            }

            val new = queue.currentSong

            val newCp = if (currentlyPlaying?.queue?.currentSong != new) {
                val stream = logger.debugElapsed("Opening song ${new.title}") {
                    new.audioStream()
                }
                logger.debugElapsed("Preparing song ${new.title}") {
                    currentlyPlaying?.bufferer?.cancel()
                    currentlyPlaying?.waveformCreator?.cancel()
                    val bufferSize = Player.optimalBufferSize(stream.format, BUFFER)
                    val input = AsyncAudioInputStream(stream, 2, bufferSize)
                    val bufferer = cs.launch(Dispatchers.IO) {
                        logger.debugElapsed("Reading ${new.title}") {
                            input.bufferAll()
                        }
                    }
                    val player = Player.create(
                        format = stream.format,
                        input = input.readers[0],
                        older = currentlyPlaying?.player,
                        bufferSize = bufferSize,
                        level = level,
                        position = position,
                        keepBufferedContent = keepBufferedContent,
                    )
                    val waveformCreator = cs.launch(Dispatchers.Default) {
                        logger.debugElapsed("Computing waveform for ${new.title}") {
                            val waveform = Waveform.fromStream(input.readers[1], stream.format, new)
                            onWaveformComputed(new, waveform)
                        }
                    }
                    CurrentlyPlaying(
                        queue,
                        player,
                        bufferer,
                        waveformCreator,
                    )
                }
            } else {
                currentlyPlaying.copy(queue = queue).apply {
                    player.seekTo(position, keepBufferedContent)
                }
            }
            return copy(
                currentlyPlaying = newCp,
                position = PositionState(newCp.player.position),
            )
        }

        suspend fun setLevel(level: Float): State {
            currentlyPlaying?.player?.setLevel(level)
            return copy(level = level)
        }
    }

    private var observableState by mutableStateOf(State.initial)
    val level get() = observableState.level
    val queue get() = observableState.currentlyPlaying?.queue
    val waveform get() = observableState.currentlyPlaying?.waveform
    val pause get() = observableState.pause
    val position get() = observableState.position

    fun position(now: Instant): Duration {
        return observableState.calculateCurrentPosition(now)
    }

    @Composable
    fun Position(): Duration {
        var instant by remember { mutableStateOf(observableState.calculateCurrentPosition(Clock.System.now())) }
        LaunchedEffect(Unit) {
            while (true) {
                instant = withFrameNanos {
                    observableState.calculateCurrentPosition(Clock.System.now())
                }
            }
        }
        return instant
    }

    suspend fun play() {
        sendCommand(Play)
    }

    suspend fun pause() {
        sendCommand(Pause)
    }

    suspend fun changeQueue(
        queue: SongQueue?,
        position: Position = Position.Current,
    ) {
        sendCommand(ChangeQueue(queue, position))
    }

    suspend fun startSeek() {
        sendCommand(EnterLowLatencyMode)
        sendCommand(SeekingStart)
    }

    suspend fun seek(queue: SongQueue?, position: Duration) {
        sendCommand(ChangeQueue(queue, Position.Specific(position)))
    }

    suspend fun endSeek() {
        sendCommand(SeekDone)
        sendCommand(ExitLowLatencyMode)
    }

    suspend fun setLevel(level: Float) {
        sendCommand(SetLevel(level))
    }

    suspend fun enterLowLatencyMode() {
        sendCommand(EnterLowLatencyMode)
    }

    suspend fun exitLowLatencyMode() {
        sendCommand(ExitLowLatencyMode)
    }

    private suspend fun sendCommand(command: PlayerCommand) {
        commandChannel.send(command)
    }

    init {
        coroutineScope.launch(Dispatchers.Swing) {
            for (state in stateChannel) {
                observableState = state
                mprisPlayer?.setState(state)
            }
        }
        thread(name = "PlayerControllerLoop") {
            runBlocking {
                launch {
                    val onWaveformComputed: suspend (Song, Waveform) -> Unit = { song, waveform ->
                        commandChannel.send(WaveformComputed(song, waveform))
                    }
                    var state = State.initial
                    var desiredPause = ZERO
                    while (true) {
                        val command: PlayerCommand? = if (desiredPause.isPositive()) {
                            withTimeoutOrNull(desiredPause) {
                                commandChannel.receive()
                            }
                        } else {
                            commandChannel.tryReceive().getOrNull()
                        }
                        val (newState, maxAllowedPause) = when (command) {
                            is ChangeQueue -> state.change {
                                changeQueue(
                                    coroutineScope,
                                    command.queue,
                                    command.position,
                                    keepBufferedContent = false,
                                    onWaveformComputed,
                                )
                            }

                            is Pause -> state.change {
                                currentlyPlaying?.player?.stop()
                                copy(
                                    position = PositionState(currentlyPlaying?.player?.position ?: ZERO),
                                    pause = true,
                                )
                            }

                            is Play -> state.change { copy(pause = false) }
                            is SeekingStart -> state.change { copy(seeking = true) }
                            is SeekDone -> state.change { copy(seeking = false) }
                            is EnterLowLatencyMode -> state.change { copy(lowLatencyMode = true) }
                            is ExitLowLatencyMode -> state.change { copy(lowLatencyMode = false) }
                            is SetLevel -> state.change {
                                state.setLevel(command.level)
                            }

                            is WaveformComputed -> state.change {
                                copy(
                                    currentlyPlaying = if (currentlyPlaying?.queue?.currentSong == command.song) {
                                        currentlyPlaying.copy(waveform = command.waveform)
                                    } else {
                                        currentlyPlaying
                                    }
                                )
                            }

                            null -> state.play(
                                coroutineScope,
                                onWaveformComputed,
                            )
                        }
                        if (newState != state) {
                            state = newState
                            stateChannel.send(state)
                        }
                        desiredPause = maxAllowedPause
                    }
                }
            }
        }
        try {
            mprisPlayer?.start()
        } catch (e: Exception) {
            logger.error { "Error starting MPRIS: ${e.message}" }
        }
    }
}