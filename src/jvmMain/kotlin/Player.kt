import data.Song
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

class Player(
    val source: AudioInputStream,
    val output: SourceDataLine,
    bufferLength: Duration,
    private val posOffset: Duration = Duration.ZERO,
) {
    val position get() = posOffset + output.microsecondPosition.microseconds

    private val buffer =
        ByteArray((source.format.frameRate * source.format.frameSize * (bufferLength / 1.seconds)).roundToInt())

    init {
        output.open(source.format, buffer.size)
    }

    fun playFrame(): Boolean {
        val available = output.available()
        if (available > 0) {
            val s = source.read(buffer, 0, available)
            return if (s >= 0) {
                output.write(buffer, 0, s)
                true
            } else {
                stop()
                false
            }
        }
        return true
    }

    fun start() {
        output.start()
    }

    fun stop() {
        output.drain()
    }

    companion object {
        fun forMp3(file: File, position: Duration, bufferLength: Duration): Player {
            val mp3In: AudioInputStream = AudioSystem.getAudioInputStream(file)
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
            val p = ((position / 1.seconds) * pcmFormat.frameRate).roundToLong() * pcmFormat.frameSize
            pcmIn.skip(p)
            val line: SourceDataLine = AudioSystem.getSourceDataLine(pcmFormat)
            return Player(pcmIn, line, bufferLength, position)
        }
    }
}