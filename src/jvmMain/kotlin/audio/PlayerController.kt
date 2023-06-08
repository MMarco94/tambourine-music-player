package audio

import androidx.compose.runtime.derivedStateOf
import audio.PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.SongQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import utils.debugElapsed
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

sealed interface Position {
    object Current : Position
    object Beginning : Position
    data class Specific(val time: Duration) : Position
}

sealed interface PlayerCommand {
    data class ChangeQueue(
        val queue: SongQueue?,
        val position: Position = Position.Current,
    ) : PlayerCommand

    object Play : PlayerCommand
    object Pause : PlayerCommand
    object Seeking : PlayerCommand
    object SeekDone : PlayerCommand
}

private val buffer = 32.milliseconds
private val playLoopDelay = minOf(16.milliseconds, buffer / 2)

object PlayerController {
    val channel = Channel<PlayerCommand>(Channel.UNLIMITED)

    private var pendingSeek by mutableStateOf<Duration?>(null)
    private var playerPosition by mutableStateOf(ZERO)
    val position by derivedStateOf {
        pendingSeek ?: playerPosition
    }

    var queue by mutableStateOf<SongQueue?>(null)
        private set

    private var player: Player? = null
    private var seeking = false
    var pause by mutableStateOf(true)
        private set

    fun seek(coroutineScope: CoroutineScope, queue: SongQueue?, position: Duration) {
        pendingSeek = position
        coroutineScope.launch {
            channel.send(Seeking)
            channel.send(ChangeQueue(queue, Position.Specific(position)))
        }
    }

    init {
        thread {
            runBlocking {
                launch {
                    while (true) {
                        // This is to avoid sending too many bytes to the audio device. That would cause pausing to be slow
                        play()
                        delay(playLoopDelay)
                    }
                }
                launch {
                    for (command in channel) {
                        when (command) {
                            is ChangeQueue -> changeQueue(command.queue, command.position)
                            Pause -> pause = true
                            Play -> pause = false
                            Seeking -> seeking = true
                            SeekDone -> seeking = false
                        }
                    }
                }
            }
        }
    }

    private fun play() {
        val p = player
        if (p != null) {
            if (!pause) {
                val done = !p.playFrame()
                if (done && !seeking) {
                    val next = queue?.nextInQueue()
                    if (next == null) {
                        p.seekToStart()
                        pause = true
                    } else {
                        changeQueue(next, Position.Beginning)
                    }
                }
            }
            playerPosition = p.position
        }
    }

    private fun changeQueue(queue: SongQueue?, position: Position) {
        if (queue == null) {
            player?.flush()
            player?.stop()
            player = null
            pendingSeek = null
            this.queue = null
            pause = true
            return
        }

        val old = PlayerController.queue?.currentSong
        val new = queue.currentSong
        this.queue = queue

        if (old != new) {
            val stream = debugElapsed("Reading song ${queue.currentSong.title}") {
                queue.currentSong.audioStream()
            }
            player = Player.create(stream, player, buffer)
        }

        val p = player
        if (p != null) {
            when (position) {
                Position.Current -> {}
                Position.Beginning -> p.seekToStart()
                is Position.Specific -> p.seekTo(position.time)
            }
        }
        play()
        pendingSeek = null

        return
    }
}