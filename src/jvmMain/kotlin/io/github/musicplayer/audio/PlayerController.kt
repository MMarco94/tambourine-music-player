package io.github.musicplayer.audio

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.musicplayer.audio.PlayerCommand.*
import io.github.musicplayer.data.Song
import io.github.musicplayer.data.SongQueue
import io.github.musicplayer.utils.debugElapsed
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

sealed interface Position {
    object Current : Position
    object Beginning : Position
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
    class Seeking(val event: Long) : PlayerCommand
    class SeekDone(val event: Long) : PlayerCommand
    class WaveformComputed(val song: Song, val waveform: Waveform) : PlayerCommand
}

private val buffer = 64.milliseconds
private val playLoopDelay = minOf(16.milliseconds, buffer / 2)

class PlayerController(
    private val coroutineScope: CoroutineScope
) {

    private val commandChannel: Channel<PlayerCommand> = Channel(Channel.UNLIMITED)
    private val stateChannel = Channel<State>(Channel.CONFLATED)
    val frequencyAnalyzer = FrequencyAnalyzer()

    private data class CurrentlyPlaying(
        val queue: SongQueue,
        val player: Player,
        val bufferer: Job,
        val waveformCreator: Job,
        val waveform: Waveform? = null,
    )

    private data class State(
        val processedEvents: Long,
        val currentlyPlaying: CurrentlyPlaying?,
        val position: Duration,
        val pause: Boolean,
        val seeking: Boolean,
    ) {

        companion object {
            val initial = State(0L, null, ZERO, true, false)
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
                if (result == Player.PlayResult.Finished && !seeking) {
                    val (next, keepPlaying) = currentlyPlaying.queue.nextInQueue()
                    changeQueue(cs, next, Position.Beginning, onWaveformComputed)
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
                    player.flush()
                    player.stop()
                    bufferer.cancel()
                    waveformCreator.cancel()
                }
                return StateChangeResult(State(processedEvents, null, ZERO, true, seeking), true)
            }

            val new = queue.currentSong

            val newCp = if (currentlyPlaying?.queue?.currentSong != new) {
                val stream = logger.debugElapsed("Opening song ${new.title}") {
                    new.audioStream()
                }
                currentlyPlaying?.bufferer?.cancel()
                currentlyPlaying?.waveformCreator?.cancel()
                val input = AsyncAudioInputStream(stream, 2)
                val player = Player.create(stream.format, input.readers[0], currentlyPlaying?.player, buffer)
                val bufferer = cs.launch(Dispatchers.IO) {
                    logger.debugElapsed("Reading ${new.title}") {
                        input.bufferAll()
                    }
                }
                val waveformCreator = cs.launch(Dispatchers.Default) {
                    logger.debugElapsed("Computing waveform for ${new.title}") {
                        val waveform = Waveform.fromStream(input.readers[1], stream.format, new)
                        onWaveformComputed(new, waveform)
                    }
                }
                CurrentlyPlaying(queue, player, bufferer, waveformCreator)
            } else {
                currentlyPlaying.copy(queue = queue)
            }
            when (position) {
                Position.Current -> {}
                Position.Beginning -> newCp.player.seekToStart()
                is Position.Specific -> newCp.player.seekTo(position.time)
            }
            return StateChangeResult(
                copy(
                    currentlyPlaying = newCp,
                    position = newCp.player.position,
                ),
                shouldPause = false
            )
        }
    }

    private var sentEvents = 0L
    private var pendingSeek by mutableStateOf<Pair<Long, Duration>?>(null)

    private var observableState by mutableStateOf(State.initial)
    val queue get() = observableState.currentlyPlaying?.queue
    val waveform get() = observableState.currentlyPlaying?.waveform
    val pause get() = observableState.pause
    val position by derivedStateOf {
        val ps = pendingSeek
        val os = observableState
        if (ps != null && os.processedEvents < ps.first) {
            ps.second
        } else {
            os.position
        }
    }

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

    private suspend fun sendCommand(command: PlayerCommand) {
        commandChannel.send(command)
    }

    fun startSeek(queue: SongQueue?, position: Duration) {
        val se = sentEvents
        sentEvents += 2
        pendingSeek = se + 1 to position
        coroutineScope.launch {
            sendCommand(Seeking(se))
            sendCommand(ChangeQueue(se + 1, queue, Position.Specific(position)))
        }
    }

    suspend fun endSeek() {
        sendCommand(SeekDone(sentEvents++))
    }

    init {
        coroutineScope.launch(Dispatchers.Main) {
            for (state in stateChannel) {
                observableState = state
            }
        }
        coroutineScope.launch(Dispatchers.Default) {
            frequencyAnalyzer.start()
        }
        thread {
            runBlocking {
                launch {
                    val onWaveformComputed: suspend (Song, Waveform) -> Unit = { song, waveform ->
                        commandChannel.send(WaveformComputed(song, waveform))
                    }
                    var state = State.initial
                    while (true) {
                        val (newState, shouldPause) = when (val command = commandChannel.tryReceive().getOrNull()) {
                            is ChangeQueue -> state
                                .changeQueue(coroutineScope, command.queue, command.position, onWaveformComputed)
                                .change { copy(processedEvents = command.event + 1) }

                            is Pause -> state.change { copy(processedEvents = command.event + 1, pause = true) }
                            is Play -> state.change { copy(processedEvents = command.event + 1, pause = false) }
                            is Seeking -> state.change { copy(processedEvents = command.event + 1, seeking = true) }
                            is SeekDone -> state.change { copy(processedEvents = command.event + 1, seeking = false) }
                            is WaveformComputed -> state.change {
                                copy(
                                    currentlyPlaying = if (currentlyPlaying?.queue?.currentSong == command.song) {
                                        currentlyPlaying.copy(waveform = command.waveform)
                                    } else {
                                        currentlyPlaying
                                    }
                                )
                            }

                            null -> state.play(coroutineScope, frequencyAnalyzer, onWaveformComputed)
                        }
                        if (newState != state) {
                            state = newState
                            stateChannel.send(state)
                        }
                        if (shouldPause) {
                            delay(playLoopDelay)
                        }
                    }
                }
            }
        }
    }
}