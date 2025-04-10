package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.durationToFrames
import io.github.mmarco94.tambourine.utils.framesToDuration
import io.github.mmarco94.tambourine.utils.toIntOrMax
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.sound.sampled.AudioFormat
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

private val logger = KotlinLogging.logger {}

class Player private constructor(
    val format: AudioFormat,
    input: AsyncAudioInputStream.Reader,
    private val output: SourceDataLineWrapper,
) {
    private val source = SeekableAudioInputStream(format, input)
    val position: Duration
        get() {
            return format.framesToDuration(precisePositionInFrames())
        }
    private var cleanOutputtedFrames = 0L

    private fun drainOrFlush() {
        if (output.isRunning) {
            logger.debug { "Draining audio sink" }
            output.drain()
        } else {
            flush()
        }
    }

    private fun flush() {
        output.flush()
    }

    private fun needsFlushingForLowLatency(): Boolean {
        return pendingFlush() > LOW_LATENCY_BUFFER
    }

    private fun precisePositionInFrames(now: ValueTimeMark = TimeSource.Monotonic.markNow()): Long {
        val buffered = output.bufferedFrames(now)
        val dirty = (buffered - cleanOutputtedFrames).coerceAtLeast(0)
        return (source.readFrames - buffered + dirty).coerceAtLeast(0)
    }

    fun pendingFlush(): Duration {
        return format.framesToDuration(output.bufferedFrames())
    }

    fun stop() {
        output.stop()
    }

    sealed interface PlayResult {
        data object Played : PlayResult
        data object NotPlayed : PlayResult
        data object Finished : PlayResult
    }

    suspend fun playFrame(bufferingCap: Duration? = null, limitFrames: Long = Long.MAX_VALUE): PlayResult {
        output.printDebugInfo()
        val available = if (bufferingCap != null) {
            val capFrames = format.durationToFrames(bufferingCap)
            val buffered = output.bufferedFrames()
            val softCap = (capFrames - buffered).toIntOrMax().coerceAtLeast(0)
            // This has the potential of being a very small number.
            // Sending excessively small frames might destroy performances
            val softAvailable = output.available().coerceAtMost(softCap * format.frameSize)
            if (softAvailable < capFrames * format.frameSize / 10) {
                0
            } else {
                softAvailable
            }
        } else {
            output.available()
        }

        output.start()
        if (available > 0) {
            val limitBytes = if (limitFrames == Long.MAX_VALUE) {
                Long.MAX_VALUE
            } else {
                limitFrames * format.frameSize
            }
            val chunk = source.read(available, limitBytes)
            return if (chunk != null) {
                cleanOutputtedFrames += chunk.length / format.frameSize
                output.write(chunk.readData, chunk.offset, chunk.length)
                PlayResult.Played
            } else {
                PlayResult.Finished
            }
        }
        return PlayResult.NotPlayed
    }

    suspend fun seekTo(position: Duration, keepBufferedContent: Boolean) {
        seekTo(format.durationToFrames(position), keepBufferedContent)
    }

    private suspend fun seekTo(positionInFrames: Long, keepBufferedContent: Boolean) {
        if (!keepBufferedContent && needsFlushingForLowLatency()) {
            // Making sure the new data is ready
            if (output.isRunning) {
                source.seekTo(positionInFrames + output.bufferSize / format.frameSize)
            }
            flush()
            cleanOutputtedFrames = 0
        }

        source.seekTo(positionInFrames)
    }

    suspend fun setLevel(level: Float) {
        output.setLevel(level)
        if (needsFlushingForLowLatency()) {
            seekTo(precisePositionInFrames(), false)
        }
    }

    companion object {

        fun optimalBufferSize(format: AudioFormat, buffer: Duration): Int {
            val bps: Float = format.frameRate * format.frameSize
            val bufferSize = (bps * (buffer / 1.seconds)).roundToInt()
            // Buffer, rounded so chunks contain a whole number of frames
            val rounded = (bufferSize / format.frameSize).coerceAtLeast(1) * format.frameSize
            return rounded
        }

        suspend fun create(
            format: AudioFormat,
            input: AsyncAudioInputStream.Reader,
            older: Player?,
            bufferSize: Int,
            level: Float,
            position: Duration,
            keepBufferedContent: Boolean,
        ): Player {
            return if (
                older != null &&
                older.format.encoding == format.encoding &&
                older.format.sampleRate == format.sampleRate &&
                older.format.frameRate == format.frameRate &&
                older.format.frameSize == format.frameSize &&
                older.format.channels == format.channels &&
                older.format.isBigEndian == format.isBigEndian &&
                older.output.bufferSize == bufferSize
            ) {
                Player(format, input, older.output).apply {
                    this.seekTo(position, keepBufferedContent)
                }
            } else {
                logger.debug { "Creating a new audio source data line" }
                val line = SourceDataLineWrapper.create(format, bufferSize)
                line.setLevel(level)
                line.start()
                val ret = Player(format, input, line)
                ret.seekTo(position, keepBufferedContent)
                if (keepBufferedContent) {
                    older?.drainOrFlush()
                } else {
                    older?.flush()
                }
                older?.stop()
                ret
            }
        }
    }
}