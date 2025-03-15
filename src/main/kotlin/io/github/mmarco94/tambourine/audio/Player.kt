package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.durationToFrames
import io.github.mmarco94.tambourine.utils.progress
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class Player private constructor(
    val format: AudioFormat,
    input: AsyncAudioInputStream.Reader,
    private val output: SourceDataLine,
) {
    private val source = SeekableAudioInputStream(format, input)
    val position get() = source.readTime

    fun flush() {
        output.drain()
    }

    fun stop() {
        output.stop()
    }

    sealed interface PlayResult {
        class Played(val chunk: Chunk) : PlayResult
        object NotPlayed : PlayResult
        object Finished : PlayResult
    }

    suspend fun playFrame(): PlayResult {
        val available = output.available()
        if (available > 0) {
            val chunk = source.read(available)
            return if (chunk != null) {
                output.write(chunk.readData, chunk.offset, chunk.length)
                PlayResult.Played(chunk)
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

    fun setLevel(level: Float) {
        output.setLevel(level)
    }

    companion object {
        fun create(
            format: AudioFormat,
            input: AsyncAudioInputStream.Reader,
            older: Player?,
            bufferLength: Duration,
            level: Float,
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
                Player(format, input, older.output)
            } else {
                older?.flush()
                older?.stop()
                val line: SourceDataLine = AudioSystem.getSourceDataLine(format)

                val bps: Float = format.frameRate * format.frameSize
                val buffer = ByteArray((bps * (bufferLength / 1.seconds)).roundToInt())
                line.open(format, buffer.size)
                line.setLevel(level)
                line.start()
                Player(format, input, line)
            }
        }

        private fun SourceDataLine.setLevel(level: Float) {
            val masterGain = getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val progress = (masterGain.minimum..masterGain.maximum.coerceAtMost(0f)).progress(level.coerceIn(0f, 1f))
            masterGain.value = progress
        }
    }
}