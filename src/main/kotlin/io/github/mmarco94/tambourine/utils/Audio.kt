package io.github.mmarco94.tambourine.utils

import javax.sound.sampled.AudioFormat
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds


val humanHearingRange = (20..20000)
val humanHearingRangeLog = humanHearingRange.toLogRange()

fun AudioFormat.framesToDuration(frames: Long) = (frames / frameRate * 1000000000L).roundToLong().nanoseconds
fun AudioFormat.durationToFrames(duration: Duration) = ((duration / 1.seconds) * frameRate).roundToLong()


fun decodeToArray(bytes: ByteArray, offset: Int, length: Int, format: AudioFormat, channel: Int = 0): DoubleArray {
    val ret = DoubleArray(length / format.frameSize)
    decode(bytes, offset, length, format, channel) { frame: Int, _: Long, scaledValue: Double ->
        ret[frame] = scaledValue
    }
    return ret
}

inline fun decode(
    bytes: ByteArray,
    offset: Int,
    length: Int,
    format: AudioFormat,
    channel: Int = 0,
    consumer: (frame: Int, absoluteValue: Long, scaledValue: Double) -> Unit
) {
    // Adapted from https://stackoverflow.com/questions/21470012/apply-fft-to-audio-recording-in-java

    require(format.encoding == AudioFormat.Encoding.PCM_SIGNED)
    val signMask = -1L shl (format.sampleSizeInBits - 1L).toInt()
    val bytesPerSample: Int = ceil(format.sampleSizeInBits / 8.0).toInt()
    val fullScale = 1L.shl(format.sampleSizeInBits - 1)
    repeat(length / format.frameSize) { frame ->
        val i = frame * format.frameSize + bytesPerSample * channel
        var ret = 0L
        for (b in 0 until bytesPerSample) {
            val actualB = if (format.isBigEndian) bytesPerSample - 1 - b else b
            ret = ret or ((bytes[offset + i + actualB].toLong() and 0xffL) shl (8 * b))
        }
        val decoded = if (ret and signMask != 0L) {
            ret or signMask
        } else ret
        consumer(frame, decoded, decoded.toDouble() / fullScale)
    }
}