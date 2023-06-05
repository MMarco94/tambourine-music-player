import PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import data.Song
import data.SongQueue
import kotlinx.coroutines.channels.Channel
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
//                launch {
//                    while (true) {
//                        if (currentPlayer != null) {
//                            println("${currentPlayer?.devicePosition} - ${currentPlayer?.decodedPosition} - ${currentPlayer?.frameLength}")
//                            delay(100)
//                        }else yield()
//                    }
//                }
                launch {
                    while (true) {
                        // This is to avoid sending too many bytes to the audio device. That would cause pausing to be slow
                        val player = currentPlayer
                        if (!pause && player != null && player.deviceLag <= player.frameLength * 2) {
                            if (!player.playFrame()) {
                                val next = SongQueue.popNext()
                                if (next != null) {
                                    play(next, null)
                                } else {
                                    player.close()
                                    currentPlayer = null
                                    pause = true
                                }
                            }
                        }
                        if (player != null) {
                            _position.value = player.decodedPosition
                        }
                        yield()
                    }
                }
                launch {
                    for (command in channel) {
                        when (command) {
                            is Play -> {
                                play(command.song, command.position)
                            }

                            Pause -> pause = true
                            Resume -> pause = false
                        }
                    }
                }
            }
        }
    }

    private fun play(song: Song, position: Duration?) {
        currentPlayer?.close()
        val player = Player(song.file.inputStream())
        if (position != null) {
            // TODO: stop if other incoming messages are "Play"
            player.seek(position)
        }
        currentPlayer = player
        pause = false
    }
}