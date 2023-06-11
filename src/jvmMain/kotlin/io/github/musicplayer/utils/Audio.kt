package io.github.musicplayer.utils

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


fun decode(bytes: ByteArray, length: Int, format: AudioFormat): DoubleArray {
    // Adapted from https://stackoverflow.com/questions/21470012/apply-fft-to-audio-recording-in-java
    val bytesPerSample: Int = ceil(format.sampleSizeInBits / 8.0).toInt()
    val transfer = LongArray(length / bytesPerSample)
    if (format.isBigEndian) {
        var i = 0
        var k = 0
        var b: Int
        while (i < length) {
            val least = i + bytesPerSample - 1
            b = 0
            while (b < bytesPerSample) {
                transfer[k] = transfer[k] or (bytes[least - b].toLong() and 0xffL shl 8 * b)
                b++
            }
            i += bytesPerSample
            k++
        }
    } else {
        var i = 0
        var k = 0
        var b: Int
        while (i < length) {
            b = 0
            while (b < bytesPerSample) {
                transfer[k] = transfer[k] or (bytes[i + b].toLong() and 0xffL shl 8 * b)
                b++
            }
            i += bytesPerSample
            k++
        }
    }
    val fullScale = 1L.shl(format.sampleSizeInBits - 1)
    require(format.encoding == AudioFormat.Encoding.PCM_SIGNED)
    val signMask = -1L shl (format.sampleSizeInBits - 1L).toInt()
    for (i in transfer.indices) {
        if (transfer[i] and signMask != 0L) {
            transfer[i] = transfer[i] or signMask
        }
    }
    val channels = format.channels
    return DoubleArray(transfer.size / format.channels) {
        // Avg between channels (transfer[it * channels] + transfer[it * channels + 1]).toDouble() / channels / fullScale
        transfer[it * channels].toDouble() / fullScale
    }
}