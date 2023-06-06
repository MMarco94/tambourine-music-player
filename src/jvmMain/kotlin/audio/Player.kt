package audio

import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class Player(
    var source: AudioInputStream,
    val output: SourceDataLine,
    bufferLength: Duration,
) {
    private val bufferedSource = CachedAudioInputStream(source)
    private val format = source.format
    private val bps: Float = format.frameRate * format.frameSize

    private var posOffset: Duration = Duration.ZERO
    val position get() = posOffset + output.microsecondPosition.microseconds

    private val buffer =
        ByteArray((bps * (bufferLength / 1.seconds)).roundToInt())

    init {
        output.open(format, buffer.size)
    }

    fun start() {
        output.start()
    }

    fun stop() {
        output.stop()
    }

    fun playFrame(): Boolean {
        val available = output.available()
        if (available > 0) {
            val s = bufferedSource.read(buffer, 0, available)
            return if (s >= 0) {
                output.write(buffer, 0, s)
                true
            } else {
                output.drain()
                stop()
                false
            }
        }
        return true
    }

    fun reset() {
        output.drain()
        posOffset = -output.microsecondPosition.microseconds
        bufferedSource.reset()
    }

    fun seekTo(position: Duration) {
        if (position >= this.position) {
            skip(position - this.position)
        } else {
            reset()
            skip(position)
        }
    }

    fun skip(amount: Duration) {
        require(amount >= ZERO)
        val p = ((amount / 1.seconds) * format.frameRate).roundToLong() * format.frameSize
        val skipped = bufferedSource.skip(p)
        val skippedLength = (skipped * 1000_000_000 / bps).roundToLong().nanoseconds
        posOffset += skippedLength
    }
}