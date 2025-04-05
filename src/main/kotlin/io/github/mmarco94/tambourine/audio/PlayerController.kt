package io.github.mmarco94.tambourine.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mmarco94.tambourine.audio.PlayerCommand.*
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.mpris.MPRISPlayerController
import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.swing.Swing
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

private val BUFFER = 10.seconds
private val SONG_SWITCH_THRESHOLD = 100.milliseconds
private val PLAY_LOOP_DELAY = minOf(10.milliseconds, BUFFER / 2)

sealed interface Position {
    data object Current : Position
    data object Beginning : Position
    data class Specific(val time: Duration) : Position
}

private sealed interface PlayerCommand {
    class ChangeQueue(
        val event: Long,
        val queue: SongQueue?,
        val position: Position,
    ) : PlayerCommand

    class Play(val event: Long) : PlayerCommand
    class Pause(val event: Long) : PlayerCommand
    class SeekingStart(val event: Long) : PlayerCommand
    class SeekDone(val event: Long) : PlayerCommand
    class SetLevel(val event: Long, val level: Float) : PlayerCommand
    class WaveformComputed(val song: Song, val waveform: Waveform) : PlayerCommand
}

class PlayerController(
    private val coroutineScope: CoroutineScope,
    quit: () -> Unit,
    raise: () -> Unit,
) {

    private val commandChannel: Channel<PlayerCommand> = Channel(Channel.UNLIMITED)
    private val stateChannel = Channel<State>(Channel.CONFLATED)
    val frequencyAnalyzer = FrequencyAnalyzer()
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

    data class State(
        val event: Long,
        val currentlyPlaying: CurrentlyPlaying?,
        val position: Duration,
        val pause: Boolean,
        val seeking: Boolean,
        val level: Float,
    ) {

        companion object {
            val initial = State(
                event = 0L,
                currentlyPlaying = null,
                position = ZERO,
                pause = true,
                seeking = false,
                level = 1f,
            )
        }

        data class StateChangeResult(
            val newState: State,
            val shouldPause: Boolean,
        ) {
            inline fun change(f: State.() -> State) = copy(newState = newState.f())
        }

        inline fun change(f: State.() -> State) = StateChangeResult(
            this.f(), false
        )

        suspend fun play(
            cs: CoroutineScope,
            frequencyAnalyzer: FrequencyAnalyzer,
            onWaveformComputed: suspend (Song, Waveform) -> Unit,
        ): StateChangeResult {
            return if (currentlyPlaying != null && !pause) {
                val result = currentlyPlaying.player.playFrame()
                if (result is Player.PlayResult.Played) {
                    frequencyAnalyzer.push(result.chunk, currentlyPlaying.player.format)
                }
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
                        onWaveformComputed = onWaveformComputed,
                    )
                        .change { copy(pause = this.pause || !keepPlaying) }
                } else {
                    StateChangeResult(
                        copy(position = currentlyPlaying.player.position),
                        shouldPause = result !is Player.PlayResult.Played
                    )
                }
            } else {
                StateChangeResult(this, true)
            }
        }

        suspend fun changeQueue(
            cs: CoroutineScope,
            queue: SongQueue?,
            position: Position,
            onWaveformComputed: suspend (Song, Waveform) -> Unit,
        ): StateChangeResult {
            if (queue == null) {
                currentlyPlaying?.apply {
                    player.stop()
                    bufferer.cancel()
                    waveformCreator.cancel()
                }
                return StateChangeResult(
                    State(
                        event = event,
                        currentlyPlaying = null,
                        position = ZERO,
                        pause = true,
                        seeking = seeking,
                        level = level
                    ), true
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
                    player.seekTo(position)
                }
            }
            return StateChangeResult(
                copy(
                    currentlyPlaying = newCp,
                    position = newCp.player.position,
                ),
                shouldPause = false
            )
        }

        suspend fun setLevel(level: Float): State {
            currentlyPlaying?.player?.setLevel(level)
            return copy(level = level)
        }
    }

    private var sentEvents = 0L
    private var observableState by mutableStateOf(State.initial)
    val level get() = observableState.level
    val queue get() = observableState.currentlyPlaying?.queue
    val waveform get() = observableState.currentlyPlaying?.waveform
    val pause get() = observableState.pause
    val position get() = observableState.position

    suspend fun play() {
        sendCommand(Play(sentEvents++))
    }

    suspend fun pause() {
        sendCommand(Pause(sentEvents++))
    }

    suspend fun changeQueue(
        queue: SongQueue?,
        position: Position = Position.Current,
    ) {
        sendCommand(ChangeQueue(sentEvents++, queue, position))
    }

    suspend fun startSeek() {
        sendCommand(SeekingStart(sentEvents++))
    }

    suspend fun seek(queue: SongQueue?, position: Duration) {
        sendCommand(ChangeQueue(sentEvents++, queue, Position.Specific(position)))
    }

    suspend fun endSeek() {
        sendCommand(SeekDone(sentEvents++))
    }

    suspend fun setLevel(level: Float) {
        sendCommand(SetLevel(sentEvents++, level))
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
        coroutineScope.launch(Dispatchers.Default) {
            frequencyAnalyzer.start()
        }
        thread(name = "PlayerControllerLoop") {
            runBlocking {
                launch {
                    val onWaveformComputed: suspend (Song, Waveform) -> Unit = { song, waveform ->
                        commandChannel.send(WaveformComputed(song, waveform))
                    }
                    var state = State.initial
                    while (true) {
                        val command = if (state.pause || state.currentlyPlaying == null) {
                            // If I don't need to play, I can idle waiting for commands
                            commandChannel.receive()
                        } else {
                            commandChannel.tryReceive().getOrNull()
                        }
                        val (newState, shouldPause) = when (command) {
                            is ChangeQueue -> state
                                .changeQueue(
                                    coroutineScope,
                                    command.queue,
                                    command.position,
                                    onWaveformComputed,
                                )
                                .change { copy(event = command.event + 1) }

                            is Pause -> state.change {
                                currentlyPlaying?.player?.stop()
                                copy(event = command.event + 1, pause = true)
                            }

                            is Play -> state.change { copy(event = command.event + 1, pause = false) }
                            is SeekingStart -> state.change {
                                logger.debug { "Seeking started" }
                                copy(
                                    event = command.event + 1,
                                    seeking = true,
                                )
                            }

                            is SeekDone -> state.change {
                                logger.debug { "Seeking done" }
                                copy(
                                    event = command.event + 1,
                                    seeking = false,
                                )
                            }

                            is SetLevel -> state.change {
                                state.setLevel(command.level).copy(event = command.event + 1)
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
                                frequencyAnalyzer,
                                onWaveformComputed,
                            )
                        }
                        if (newState != state) {
                            state = newState
                            stateChannel.send(state)
                        }
                        if (shouldPause) {
                            delay(PLAY_LOOP_DELAY)
                        }
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