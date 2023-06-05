import PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import data.Song
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

sealed interface PlayerCommand {
    data class Play(val song: Song, val position: Duration? = null) : PlayerCommand
    object Resume : PlayerCommand
    object Pause : PlayerCommand
}


object PlayerController {
    val channel = Channel<PlayerCommand>()

    var _position = mutableStateOf(0.milliseconds)
    val position by _position

    private var currentPlayer: Player? = null
    private var pause = false

    init {
        thread {
            runBlocking {
                launch {
                    while (true) {
                        if (currentPlayer != null) {
                            println("${currentPlayer?.devicePosition} - ${currentPlayer?.decodedPosition} - ${currentPlayer?.frameLength}")
                            delay(100)
                        }else yield()
                    }
                }
                launch {
                    while (true) {
                        // This is to avoid sending too many bytes to the audio device. That would cause pausing to be slow
                        if (!pause && currentPlayer != null && currentPlayer!!.deviceLag <= currentPlayer!!.frameLength * 2) {
                            pause = !currentPlayer!!.play(1)
                        }
                        if (currentPlayer != null) {
                            _position.value = currentPlayer!!.decodedPosition
                        }
                        yield()
                    }
                }
                launch {
                    for (command in channel) {
                        when (command) {
                            is Play -> {
                                currentPlayer?.close()
                                val player = Player(command.song.file.inputStream())
                                if (command.position != null) {
                                    // TODO: stop if other incoming messages are "Play"
                                    player.seek(command.position)
                                }
                                currentPlayer = player
                                pause = false
                            }

                            Pause -> pause = true
                            Resume -> pause = false
                        }
                    }
                }
            }
        }
    }
}