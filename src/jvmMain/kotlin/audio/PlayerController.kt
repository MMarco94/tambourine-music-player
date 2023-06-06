package audio

import audio.PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import data.SongQueue
import debugElapsed
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
    val channel = Channel<PlayerCommand>()

    var _position = mutableStateOf(0.milliseconds)
    val position by _position

    private var _queue = mutableStateOf<SongQueue?>(null)
    val queue by _queue

    private var player: Player? = null
    private var pause = false

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
                        }
                    }
                }
            }
        }
    }

    private fun play() {
        val p = player
        if (p != null && !pause) {
            val done = !p.playFrame()
            _position.value = p.position
            if (done) {
                changeQueue(queue?.next(), Position.Beginning)
            }
        }
    }

    private fun changeQueue(queue: SongQueue?, position: Position) {
        if (queue == null) {
            player?.flush()
            player?.stop()
            player = null
            _queue.value = null
            pause = true
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
        return
    }
}