package audio

import androidx.compose.runtime.derivedStateOf
import audio.PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.SongQueue
import debugElapsed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        val position: Position,
    ) : PlayerCommand

    object Play : PlayerCommand
    object Pause : PlayerCommand
}

private val buffer = 32.milliseconds
private val playLoopDelay = minOf(16.milliseconds, buffer / 2)

object PlayerController {
    val channel = Channel<PlayerCommand>(Channel.UNLIMITED)

    private var _pendingSeek by mutableStateOf<Duration?>(null)
    private var _position by mutableStateOf(ZERO)
    val position by derivedStateOf {
        _pendingSeek ?: _position
    }

    private var _queue = mutableStateOf<SongQueue?>(null)
    val queue by _queue

    private var player: Player? = null
    private var _pause = mutableStateOf(true)
    val pause by _pause

    fun seek(coroutineScope: CoroutineScope, queue: SongQueue?, position: Duration) {
        _pendingSeek = position
        coroutineScope.launch {
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
                            is ChangeQueue ->  changeQueue(command.queue, command.position)
                            Pause -> _pause.value = true
                            Play -> _pause.value = false
                        }
                    }
                }
            }
        }
    }

    private fun play() {
        val p = player
        if (p != null) {
            if (!_pause.value) {
                val done = !p.playFrame()
                if (done) {
                    changeQueue(queue?.next(), Position.Beginning)
                }
            }
            _position = p.position
        }
    }

    private fun changeQueue(queue: SongQueue?, position: Position) {
        if (queue == null) {
            player?.flush()
            player?.stop()
            player = null
            _pendingSeek = null
            _queue.value = null
            _pause.value = true
            return
        }

        val old = PlayerController.queue?.currentSong
        val new = queue.currentSong
        _queue.value = queue

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
        _pendingSeek = null

        return
    }
}