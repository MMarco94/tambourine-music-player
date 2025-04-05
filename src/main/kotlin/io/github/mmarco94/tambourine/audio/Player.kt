package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.bytesToDuration
import io.github.mmarco94.tambourine.utils.durationToFrames
import io.github.mmarco94.tambourine.utils.framesToDuration
import io.github.mmarco94.tambourine.utils.progress
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class Player private constructor(
    val format: AudioFormat,
    input: AsyncAudioInputStream.Reader,
    private val output: SourceDataLine,
    val bufferSize: Int,
) {
    private val source = SeekableAudioInputStream(format, input)
    val position: Duration get() = format.framesToDuration(precisePositionInFrames())
    private var cleanOutputtedBytes = 0L

    init {
        require(bufferSize in 1..output.bufferSize)
    }

    private fun flush() {
        logger.debug { "Flushing audio sink" }
        output.flush()
    }

    private fun dirtyBytes(availableOutput: Int = output.available()): Long {
        // The output line might already contain some data.
        // This function checks whether all data in the line was written by us
        return (pendingFlushBytes(availableOutput) - cleanOutputtedBytes).coerceAtLeast(0)
    }

    private fun precisePositionInFrames(availableOutput: Int = output.available()): Long {
        return (source.readFrames - (pendingFlushBytes(availableOutput) - dirtyBytes(availableOutput)) / format.frameSize).coerceAtLeast(
            0
        )
    }

    fun stop() {
        output.stop()
    }

    private fun pendingFlushBytes(availableOutput: Int = output.available()): Int {
        return output.bufferSize - availableOutput
    }

    fun pendingFlush(): Duration {
        return format.bytesToDuration(pendingFlushBytes().toLong())
    }

    sealed interface PlayResult {
        class Played(val chunk: Chunk) : PlayResult
        data object NotPlayed : PlayResult
        data object Finished : PlayResult
    }

    suspend fun playFrame(): PlayResult {
        val desiredBufferSize = bufferSize
        val available = output.available()
        val filled = output.bufferSize - available
        val toSend = (desiredBufferSize - filled).coerceAtLeast(0)
        output.start()
        if (toSend > 0) {
            val chunk = source.read(toSend)
            return if (chunk != null) {
                if (filled == 0) {
                    logger.debug { "Audio sink's buffer is empty. A stutter might have happened" }
                }
                cleanOutputtedBytes += chunk.length
                output.write(chunk.readData, chunk.offset, chunk.length)
                PlayResult.Played(chunk)
            } else {
                PlayResult.Finished
            }
        }
        return PlayResult.NotPlayed
    }

    suspend fun seekTo(position: Position) {
        when (position) {
            Position.Current -> {}
            Position.Beginning -> seekTo(0)
            is Position.Specific -> seekTo(format.durationToFrames(position.time))
        }
    }

    private suspend fun seekTo(positionInFrames: Long) {
        if (output.isRunning) {
            // Making sure the new data is ready
            source.seekTo(positionInFrames + bufferSize / format.frameSize)
        }

        flush()

        cleanOutputtedBytes = 0
        source.seekTo(positionInFrames)
    }

    suspend fun setLevel(level: Float) {
        output.setLevel(level)
        seekTo(precisePositionInFrames())
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
            position: Position,
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
                Player(format, input, older.output, bufferSize).apply {
                    this.seekTo(position)
                }
            } else {
                logger.debug { "Creating a new audio source data line" }
                val line: SourceDataLine = AudioSystem.getSourceDataLine(format)
                line.open(format, bufferSize)
                line.setLevel(level)
                line.start()
                val ret = Player(format, input, line, bufferSize)
                ret.seekTo(position)
                older?.flush()
                older?.stop()
                ret
            }
        }

        private fun SourceDataLine.setLevel(level: Float) {
            val masterGain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val progress = (masterGain.minimum..masterGain.maximum.coerceAtMost(0f)).progress(level.coerceIn(0f, 1f))
            masterGain.value = progress
        }
    }
}