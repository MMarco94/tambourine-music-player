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


fun decode(bytes: ByteArray, offset: Int, length: Int, format: AudioFormat, channel: Int = 0): DoubleArray {
    // Adapted from https://stackoverflow.com/questions/21470012/apply-fft-to-audio-recording-in-java
    val bytesPerSample: Int = ceil(format.sampleSizeInBits / 8.0).toInt()
    val transfer = LongArray(length / format.frameSize) { frame ->
        val i = frame * format.frameSize + bytesPerSample * channel
        var ret = 0L
        for (b in 0 until bytesPerSample) {
            val actualB = if (format.isBigEndian) bytesPerSample - 1 - b else b
            ret = ret or ((bytes[offset + i + actualB].toLong() and 0xffL) shl (8 * b))
        }
        ret
    }
    require(format.encoding == AudioFormat.Encoding.PCM_SIGNED)
    val signMask = -1L shl (format.sampleSizeInBits - 1L).toInt()
    for (i in transfer.indices) {
        if (transfer[i] and signMask != 0L) {
            val before = transfer[i]
            val after = before or signMask
            transfer[i] = after
        }
    }
    val fullScale = 1L.shl(format.sampleSizeInBits - 1)
    return DoubleArray(transfer.size) {
        transfer[it].toDouble() / fullScale
    }
}