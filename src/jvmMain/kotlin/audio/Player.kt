package audio

import utils.durationToFrames
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Player private constructor(
    source: AudioInputStream,
    val output: SourceDataLine,
    val buffer: ByteArray,
) {
    private val source = CachedAudioInputStream(source)
    private val format = source.format
    val position get() = source.readTime

    fun flush() {
        output.drain()
    }

    fun stop() {
        output.stop()
    }

    fun playFrame(): Boolean {
        val available = output.available()
        if (available > 0) {
            val s = source.read(buffer, 0, available)
            return if (s >= 0) {
                output.write(buffer, 0, s)
                true
            } else {
                false
            }
        }
        return true
    }

    fun seekToStart() {
        source.seekToStart()
    }
    fun seekTo(position: Duration) {
        source.seekTo(format.durationToFrames(position))
    }

    companion object {
        fun create(
            source: AudioInputStream,
            older: Player?,
            bufferLength: Duration,
        ): Player {
            return if (
                older != null &&
                older.format.encoding == source.format.encoding &&
                older.format.sampleRate == source.format.sampleRate &&
                older.format.frameRate == source.format.frameRate &&
                older.format.frameSize == source.format.frameSize &&
                older.format.channels == source.format.channels &&
                older.format.isBigEndian == source.format.isBigEndian
            ) {
                Player(source, older.output, older.buffer)
            } else {
                older?.flush()
                older?.stop()
                val bps: Float = source.format.frameRate * source.format.frameSize
                val buffer = ByteArray((bps * (bufferLength / 1.seconds)).roundToInt())
                val line: SourceDataLine = AudioSystem.getSourceDataLine(source.format)
                line.open(source.format, buffer.size)
                line.start()
                Player(source, line, buffer)
            }

        }
    }
}