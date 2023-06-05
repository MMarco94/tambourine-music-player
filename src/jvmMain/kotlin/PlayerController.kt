import PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import data.SongQueue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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


object PlayerController {
    val channel = Channel<PlayerCommand>()

    var _position = mutableStateOf(0.milliseconds)
    val position by _position

    private var _queue = mutableStateOf<SongQueue?>(null)
    val queue by _queue

    private var currentPlayer: Player? = null
    private var pause = false

    init {
        thread {
            runBlocking {
                launch {
                    while (true) {
                        if (currentPlayer != null) {
                            println("${currentPlayer?.decodedPosition} - ${currentPlayer?.deviceLag} - ${currentPlayer?.frameLength}")
                            delay(100)
                        } else yield()
                    }
                }
                launch {
                    while (true) {
                        // This is to avoid sending too many bytes to the audio device. That would cause pausing to be slow
                        val player = currentPlayer
                        if (!pause && player != null /*&& player.deviceLag <= player.frameLength * 2*/) {
                            if (!player.playFrame()) {
                                changeQueue(_queue.value?.next(), ZERO)
                            }
                        }
                        if (player != null) {
                            _position.value = player.devicePosition
                        }
                        yield()
                    }
                }
                launch {
                    for (command in channel) {
                        when (command) {
                            is ChangeQueue -> changeQueue(command.queue, command.position)
                            Pause -> pause = currentPlayer != null
                            Play -> pause = false
                        }
                    }
                }
            }
        }
    }

    private fun changeQueue(queue: SongQueue?, position: Duration?) {
        val old = this._queue.value
        val oldPos = this.currentPlayer?.decodedPosition
        if (old?.currentSong != queue?.currentSong || oldPos == null || position != null && position < oldPos) {
            currentPlayer?.device?.flush()
            currentPlayer?.close()
            currentPlayer = if (queue != null) {
                Player(queue.currentSong.file.inputStream())
            } else null
        }
        if (currentPlayer == null) {
            pause = true
        }
        _queue.value = queue
        if (position != null) {
            currentPlayer?.seek(position)
        }
    }
}