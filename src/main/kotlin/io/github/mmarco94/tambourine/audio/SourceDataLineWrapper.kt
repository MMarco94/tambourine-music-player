package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.framesToDuration
import io.github.mmarco94.tambourine.utils.progress
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
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

    private fun refreshPendingFrames(now: ValueTimeMark = TimeSource.Monotonic.markNow()) {
        if (line.isRunning) {
            val elapsed: Duration = now - pendingTime
            val outputtedFrames = (elapsed.toDouble(DurationUnit.SECONDS) * format.frameRate).toLong()
            pendingFrames = (pendingFrames - outputtedFrames).coerceAtLeast(0)
        }
        pendingTime = now
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
        refreshPendingFrames()
        pendingFrames += wrote / format.frameSize
    }

    fun printDebugInfo() {
        refreshPendingFrames()
        logger.debug { "$pendingFrames (${format.framesToDuration(pendingFrames)}) are pending in the buffer" }
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