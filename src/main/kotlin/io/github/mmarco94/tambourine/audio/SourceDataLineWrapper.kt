package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.bytesToDuration
import io.github.mmarco94.tambourine.utils.framesToDuration
import io.github.mmarco94.tambourine.utils.progress
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

private val logger = KotlinLogging.logger {}

class SourceDataLineWrapper(
    private val line: SourceDataLine,
) {
    val format = line.format!!
    val isRunning: Boolean get() = line.isRunning
    val bufferSize: Int get() = line.bufferSize

    private var pendingFrames: Long = 0
    private var pendingTime: ValueTimeMark = TimeSource.Monotonic.markNow()
    private var allowedDelta = (format.frameRate * 0.05).toLong()

    private fun pendingLineFrames(available: Int = line.available()): Long {
        // In a sane World, this would be the only function needed. Unfortunately, sometimes, these numbers are widely wrong.
        // That's why `refreshPendingFrames` is also necessary.
        val buffered = (line.bufferSize - available).coerceAtLeast(0)
        return buffered.toLong() / format.frameSize
    }

    private fun refreshPendingFrames(
        now: ValueTimeMark = TimeSource.Monotonic.markNow(),
        available: Int = line.available(),
    ): Boolean {
        val line = pendingLineFrames(available)
        if (isRunning) {
            val elapsed: Duration = now - pendingTime
            val outputtedFrames = (elapsed.toDouble(DurationUnit.SECONDS) * format.frameRate).toLong()
            pendingFrames = (pendingFrames - outputtedFrames).coerceAtLeast(0)
        }
        pendingTime = now
        if (abs(pendingFrames - line) < allowedDelta) {
            pendingFrames = line
            return true
        } else {
            return false
        }
    }

    fun bufferedFrames(now: ValueTimeMark = TimeSource.Monotonic.markNow()): Long {
        refreshPendingFrames(now)
        return pendingFrames
    }


    fun setLevel(level: Float) {
        line.setLevel(level)
    }

    fun start() {
        refreshPendingFrames()
        line.start()
    }

    fun stop() {
        refreshPendingFrames()
        line.stop()
    }

    fun drain() {
        line.drain()
        pendingFrames = 0
    }

    fun flush() {
        line.flush()
        pendingFrames = 0
    }

    fun available(): Int {
        return line.available()
    }

    fun write(readData: ByteArray, offset: Int, length: Int) {
        val wrote = line.write(readData, offset, length)
        logger.debug { "Writing ${format.bytesToDuration(wrote.toLong())} to line" }
        refreshPendingFrames()
        pendingFrames += wrote / format.frameSize
    }

    fun printDebugInfo() {
        val now = TimeSource.Monotonic.markNow()
        val available = available()
        val lineBuffer = pendingLineFrames(available)
        val precise = refreshPendingFrames(now, available)
        val buffered = bufferedFrames(now)
        logger.debug {
            "${format.framesToDuration(buffered)} ${if (precise) "precisely" else "estimate"} pending. " +
                    "counter=${format.framesToDuration(pendingFrames)}; " +
                    "line=${format.framesToDuration(lineBuffer)}"
        }
    }

    companion object {

        fun create(format: AudioFormat, bufferSize: Int): SourceDataLineWrapper {
            val line = AudioSystem.getSourceDataLine(format)
            line.open(format, bufferSize)
            return SourceDataLineWrapper(line)
        }
    }
}

private fun SourceDataLine.setLevel(level: Float) {
    val masterGain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
    val progress = (masterGain.minimum..masterGain.maximum.coerceAtMost(0f)).progress(level.coerceIn(0f, 1f))
    masterGain.value = progress
}