package io.github.musicplayer.audio

import io.github.musicplayer.utils.AsyncInputStream
import io.github.musicplayer.utils.durationToFrames
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Player private constructor(
    val format: AudioFormat,
    val input: AsyncInputStream,
    private val output: SourceDataLine,
    private val buffer: ByteArray,
) {
    private val source = SeekableAudioInputStream(format, this.input)
    private val bufferChunks = 1.shl(18).roundBytesToFrame()
    val position get() = source.readTime

    fun flush() {
        output.drain()
    }

    fun stop() {
        output.stop()
    }

    sealed interface PlayResult {
        class Played(val data: ByteArray, val size: Int) : PlayResult
        object NotPlayed : PlayResult
        object Finished : PlayResult
    }

    suspend fun playFrame(): PlayResult {
        val available = output.available()
        if (available > 0) {
            val s = source.read(buffer, available)
            return if (s >= 0) {
                output.write(buffer, 0, s)
                PlayResult.Played(buffer, s)
            } else {
                PlayResult.Finished
            }
        }
        return PlayResult.NotPlayed
    }

    suspend fun seekToStart() {
        source.seekToStart()
    }

    suspend fun seekTo(position: Duration) {
        source.seekTo(format.durationToFrames(position))
    }

    /**
     * Returns whether the finish has been reached
     */
    suspend fun buffer(): Boolean {
        return input.buffer(bufferChunks)
    }

    private fun Int.roundBytesToFrame() = this / format.frameSize * format.frameSize

    companion object {
        fun create(
            format: AudioFormat,
            input: AsyncInputStream,
            older: Player?,
            bufferLength: Duration,
        ): Player {
            return if (
                older != null &&
                older.format.encoding == format.encoding &&
                older.format.sampleRate == format.sampleRate &&
                older.format.frameRate == format.frameRate &&
                older.format.frameSize == format.frameSize &&
                older.format.channels == format.channels &&
                older.format.isBigEndian == format.isBigEndian
            ) {
                Player(format, input, older.output, older.buffer)
            } else {
                older?.flush()
                older?.stop()
                val line: SourceDataLine = AudioSystem.getSourceDataLine(format)

                val bps: Float = format.frameRate * format.frameSize
                val buffer = ByteArray((bps * (bufferLength / 1.seconds)).roundToInt())
                line.open(format, buffer.size)
                line.start()
                Player(format, input, line, buffer)
            }
        }
    }
}