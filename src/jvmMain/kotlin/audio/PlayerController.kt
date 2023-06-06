package audio

import audio.PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import data.SongQueue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

sealed interface PlayerCommand {
    data class ChangeQueue(
        val queue: SongQueue?,
        // Null if you don't care
        val position: Duration? = Duration.ZERO,
    ) : PlayerCommand

    object Play : PlayerCommand
    object Pause : PlayerCommand
}

private val buffer = 25.milliseconds

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
                        val p = player
                        if (p != null) {
                            if (!pause) {
                                if (!p.playFrame()) {
                                    changeQueue(queue?.next(), ZERO)
                                }
                            }
                            _position.value = p.position
                        }
                        delay(buffer / 2)
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

    private fun changeQueue(queue: SongQueue?, position: Duration?) {
        if (queue == null) {
            player?.stop()
            player = null
            _queue.value = null
            pause = true
            return
        }
        val old = PlayerController.queue?.currentSong
        val new = queue.currentSong
        _queue.value = queue

        if (old == new) {
            if (position != null) player!!.seekTo(position)
        } else {
            val stream = queue.currentSong.audioStream()
            val line: SourceDataLine = AudioSystem.getSourceDataLine(stream.format)
            player = Player(stream, line, buffer).apply {
                skip(position ?: ZERO)
                start()
            }
        }
        return
    }
}