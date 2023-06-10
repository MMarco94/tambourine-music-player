package io.github.musicplayer.audio

import io.github.musicplayer.utils.AsyncInputStream
import io.github.musicplayer.utils.durationToFrames
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Player private constructor(
    source: AudioInputStream,
    private val output: SourceDataLine,
    private val buffer: ByteArray,
) {
    private val input = AsyncInputStream(source)
    private val source = SeekableAudioInputStream(source.format, input)
    private val format = source.format
    private val bufferChunks = 1.shl(18).roundBytesToFrame()
    val position get() = source.readTime

    fun flush() {
        output.drain()
    }

    fun stop() {
        output.stop()
    }

    enum class PlayResult {
        PLAYED, NOT_PLAYED, FINISHED
    }

    suspend fun playFrame(): PlayResult {
        val available = output.available()
        if (available > 0) {
            val s = source.read(buffer, available)
            return if (s >= 0) {
                output.write(buffer, 0, s)
                PlayResult.PLAYED
            } else {
                PlayResult.FINISHED
            }
        }
        return PlayResult.NOT_PLAYED
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
                val line: SourceDataLine = AudioSystem.getSourceDataLine(source.format)

                val bps: Float = source.format.frameRate * source.format.frameSize
                val buffer = ByteArray((bps * (bufferLength / 1.seconds)).roundToInt())
                line.open(source.format, buffer.size)
                line.start()
                Player(source, line, buffer)
            }

        }
    }
}