package io.github.mmarco94.tambourine.audio

import androidx.compose.runtime.*
import io.github.mmarco94.tambourine.audio.PlayerCommand.*
import io.github.mmarco94.tambourine.data.Library
import io.github.mmarco94.tambourine.data.SongQueue
import io.github.mmarco94.tambourine.mpris.MPRISPlayerController
import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.swing.Swing
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val logger = KotlinLogging.logger {}

private val BUFFER = 10.seconds
private val FIRST_READ_BUFFER = MAX_SILENCE_PADDING
val LOW_LATENCY_BUFFER = 100.milliseconds
private val SONG_SWITCH_THRESHOLD = 100.milliseconds

sealed interface Position {
    data object Current : Position
    data object Beginning : Position
    data class Specific(val time: Duration) : Position
}

private sealed interface PlayerCommand {
    class TransformQueue(
        val transformation: (SongQueue?) -> Pair<SongQueue?, Position>,
    ) : PlayerCommand

    data object Play : PlayerCommand
    data object Pause : PlayerCommand
    data object SeekingStart : PlayerCommand
    data object SeekDone : PlayerCommand
    data object EnterLowLatencyMode : PlayerCommand
    data object ExitLowLatencyMode : PlayerCommand
    class SetLevel(val level: Float) : PlayerCommand
}

class PlayerController(
    private val coroutineScope: CoroutineScope,
    quit: () -> Unit,
    raise: () -> Unit,
    musicLibrary: Flow<Library?>,
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
        val jobsScope: CoroutineScope,
        val queue: SongQueue,
        val player: Player,
        val decodedSongData: StateFlow<SongDecoder.DecodedSongData?>,
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

        suspend fun play(): StateChangeResult {
            return if (currentlyPlaying != null && !pause) {
                val bufferingCap = if (lowLatencyMode) LOW_LATENCY_BUFFER / 2 else null
                val decodedSongData = currentlyPlaying.decodedSongData.value
                val limit = if (decodedSongData != null && decodedSongData.done) {
                    decodedSongData.songNonSilentLengthFrames()
                } else {
                    Long.MAX_VALUE
                }

                val result = currentlyPlaying.player.playFrame(bufferingCap, limit)
                if (
                    result == Player.PlayResult.Finished &&
                    !seeking &&
                    currentlyPlaying.player.pendingFlush() < SONG_SWITCH_THRESHOLD
                ) {
                    val (next, keepPlaying) = currentlyPlaying.queue.nextInQueue()
                    changeQueue(
                        queue = next,
                        position = Position.Beginning,
                        keepBufferedContent = true,
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

        fun clearQueue(): State {
            currentlyPlaying?.apply {
                player.stop()
                jobsScope.cancel()
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

        suspend fun changeQueue(
            queue: SongQueue?,
            position: Position,
            keepBufferedContent: Boolean,
        ): State {
            if (queue == null) {
                return clearQueue()
            }

            val new = queue.currentSong

            val newCp = if (currentlyPlaying?.queue?.currentSongKey != new.uniqueKey) {
                val stream = logger.debugElapsed("Opening song ${new.title}") {
                    try {
                        new.audioStream()
                    } catch (e: IOException) {
                        logger.error(e) { "Cannot open audio stream for ${new.title}" }
                        return clearQueue()
                    }
                }
                currentlyPlaying?.jobsScope?.cancel()
                val scope = CoroutineScope(Dispatchers.Default)
                logger.debugElapsed("Preparing song ${new.title}") {
                    val firstBufferSize = Player.optimalBufferSize(stream.format, FIRST_READ_BUFFER)
                    val bufferSize = Player.optimalBufferSize(stream.format, BUFFER)
                    val input = AsyncAudioInputStream(stream, 2, firstBufferSize, bufferSize)
                    scope.launch(Dispatchers.IO) {
                        logger.debugElapsed("Reading ${new.title}") {
                            input.bufferAll()
                        }
                    }
                    val songDecoder = SongDecoder(input.readers[1], stream.format, new)
                    songDecoder.start(scope)
                    val decoded = songDecoder.decodedSongData.filterNotNull().first()

                    // It doesn't make sense to keep the position across songs
                    val actualPosition = when (position) {
                        Position.Current, Position.Beginning -> decoded.songSilenceStart()
                        is Position.Specific -> position.time
                    }
                    val player = Player.create(
                        format = stream.format,
                        input = input.readers[0],
                        older = currentlyPlaying?.player,
                        bufferSize = bufferSize,
                        level = level,
                        position = actualPosition,
                        keepBufferedContent = keepBufferedContent,
                    )
                    CurrentlyPlaying(
                        scope,
                        queue,
                        player,
                        songDecoder.decodedSongData,
                    )
                }
            } else {
                currentlyPlaying.copy(queue = queue).apply {
                    when (position) {
                        Position.Beginning -> {
                            val decoded = currentlyPlaying.decodedSongData.value
                            val startPadding = decoded?.songSilenceStart() ?: ZERO
                            player.seekTo(startPadding, keepBufferedContent)
                        }

                        is Position.Specific -> player.seekTo(position.time, keepBufferedContent)
                        Position.Current -> {}
                    }
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
    val level by derivedStateOf { observableState.level }
    val queue by derivedStateOf { observableState.currentlyPlaying?.queue }
    val pause by derivedStateOf { observableState.pause }
    val position by derivedStateOf { observableState.position }

    fun position(now: Instant): Duration {
        return observableState.calculateCurrentPosition(now)
    }

    @Composable
    fun DecodedSongData(): androidx.compose.runtime.State<SongDecoder.DecodedSongData?> {
        val cp = derivedStateOf { observableState.currentlyPlaying }.value
        return if (cp != null) {
            cp.decodedSongData.collectAsState(null)
        } else {
            rememberUpdatedState(null)
        }
    }

    /**
     * Warning: the function re-composes like crazy!
     */
    @Composable
    fun Position(): androidx.compose.runtime.State<Duration> {
        return Position { it }
    }

    /**
     * Warning: the function re-composes like crazy!
     */
    @Composable
    fun <T> Position(
        transform: (Duration) -> T
    ): androidx.compose.runtime.State<T> {
        val ret = remember {
            mutableStateOf(
                transform(observableState.calculateCurrentPosition(Clock.System.now()))
            )
        }
        ObservePosition {
            ret.value = transform(it)
        }
        return ret
    }

    /**
     * `onPositionChange` will be called like crazy
     */
    @Composable
    fun ObservePosition(
        onPositionChange: (Duration) -> Unit,
    ) {
        onPositionChange(observableState.calculateCurrentPosition(Clock.System.now()))
        if (!pause) {
            LaunchedEffect(Unit) {
                while (true) {
                    withFrameNanos {
                        onPositionChange(observableState.calculateCurrentPosition(Clock.System.now()))
                    }
                }
            }
        } else {
            LaunchedEffect(observableState.position) {
                onPositionChange(observableState.calculateCurrentPosition(Clock.System.now()))
            }
        }
    }

    suspend fun play() {
        sendCommand(Play)
    }

    suspend fun pause() {
        sendCommand(Pause)
    }

    suspend fun transformQueue(
        transformation: (SongQueue?) -> Pair<SongQueue?, Position>,
    ) {
        sendCommand(TransformQueue(transformation))
    }

    suspend fun startSeek() {
        sendCommand(EnterLowLatencyMode)
        sendCommand(SeekingStart)
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

    private fun batch(commands: List<PlayerCommand?>): List<PlayerCommand> {
        return buildList {
            for (command in commands) {
                if (command != null) {
                    val last = lastOrNull()
                    // Transform commands are folded together
                    if (last is TransformQueue && command is TransformQueue) {
                        logger.debug { "Transformation events have been batched!" }
                        removeLast()
                        add(TransformQueue { initialQueue ->
                            val (queue, position) = last.transformation(initialQueue)
                            val (queue2, position2) = last.transformation(queue)
                            queue2 to when (position2) {
                                Position.Current -> position
                                else -> position2
                            }
                        })
                    } else {
                        add(command)
                    }
                }

            }
        }
    }

    init {
        coroutineScope.launch(Dispatchers.Swing) {
            for (state in stateChannel) {
                observableState = state
                mprisPlayer?.setState(state)
            }
        }
        thread(name = "PlayerControllerLoop", priority = Thread.MAX_PRIORITY) {
            runBlocking {
                launch {
                    musicLibrary
                        .filterNotNull()
                        .collectLatest { library ->
                            transformQueue { queue ->
                                queue?.updateLibrary(library) to Position.Current
                            }
                        }
                }
                launch {
                    var state = State.initial
                    var desiredPause = ZERO
                    while (true) {
                        var initialCommand: PlayerCommand? = if (desiredPause.isPositive()) {
                            withTimeoutOrNull(desiredPause) {
                                commandChannel.receive()
                            }
                        } else {
                            commandChannel.tryReceive().getOrNull()
                        }
                        val commands = batch(
                            buildList {
                                add(initialCommand)
                                while (last() != null) {
                                    add(commandChannel.tryReceive().getOrNull())
                                }
                            }
                        )
                        val newState1 = commands.fold(state) { state, command ->
                            state.run {
                                when (command) {
                                    is TransformQueue -> {
                                        val (newQueue, newPosition) = command.transformation(currentlyPlaying?.queue)
                                        changeQueue(
                                            newQueue,
                                            newPosition,
                                            keepBufferedContent = false,
                                        )
                                    }

                                    is Pause -> {
                                        currentlyPlaying?.player?.stop()
                                        copy(
                                            position = PositionState(currentlyPlaying?.player?.position ?: ZERO),
                                            pause = true,
                                        )
                                    }

                                    is Play -> copy(pause = false)
                                    is SeekingStart -> copy(seeking = true)
                                    is SeekDone -> copy(seeking = false)
                                    is EnterLowLatencyMode -> copy(lowLatencyMode = true)
                                    is ExitLowLatencyMode -> copy(lowLatencyMode = false)
                                    is SetLevel -> state.setLevel(command.level)
                                }
                            }
                        }
                        if (newState1 != state) {
                            stateChannel.send(newState1)
                        }
                        val (newState2, maxAllowedPause) = newState1.play()
                        if (newState2 != newState1) {
                            stateChannel.send(newState2)
                        }
                        state = newState2
                        desiredPause = maxAllowedPause
                    }
                }
            }
        }
        mprisPlayer?.start()
    }
}