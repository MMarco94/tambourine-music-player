import PlayerCommand.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import data.SongQueue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import javax.sound.sampled.*
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.microseconds
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

    private var clip: Clip? = null
    private var pause = false

    init {
        thread {
            runBlocking {
                launch {
                    while (true) {
                        // This is to avoid sending too many bytes to the audio device. That would cause pausing to be slow
                        val clip = clip
                        if (clip != null) {
                            _position.value = clip.microsecondPosition.microseconds
                        }
                        yield()
                    }
                }
                launch {
                    for (command in channel) {
                        when (command) {
                            is ChangeQueue -> changeQueue(command.queue, command.position)
                            Pause -> {
                                pause = true
                                clip?.stop()
                            }
                            Play -> {
                                pause = false
                                clip?.start()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun changeQueue(queue: SongQueue?, position: Duration?) {
        this.clip?.stop()
        if (queue == null) {
            this.clip = null
            this._queue.value = null
            this.pause = true
            return
        }
        val mp3In: AudioInputStream = AudioSystem.getAudioInputStream(queue.currentSong.file)
        val mp3Format: AudioFormat = mp3In.format
        val pcmFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            mp3Format.sampleRate,
            16,
            mp3Format.channels,
            16 * mp3Format.channels / 8,
            mp3Format.sampleRate,
            mp3Format.isBigEndian,
        )
        val pcmIn: AudioInputStream = AudioSystem.getAudioInputStream(pcmFormat, mp3In)

        val clip: Clip = AudioSystem.getClip()
        clip.open(pcmIn)
        if (position != null) {
            clip.microsecondPosition = position.inWholeMicroseconds
        }
        this.clip = clip
        this. _queue.value = queue
        if(!this.pause) {
            clip.start()
        }
        return
    }
}