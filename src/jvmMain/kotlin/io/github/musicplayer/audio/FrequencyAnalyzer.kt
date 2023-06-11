package io.github.musicplayer.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.fft4j.FastFouriers
import kotlinx.coroutines.channels.Channel
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.pow

private const val samplesSize = 1.shl(15) //32k
private const val fade = 0.1
private const val fadeALittle = 0.3

class FrequencyAnalyzer {
    var lastFrequency by mutableStateOf(DoubleArray(samplesSize))
        private set
    var fadedFrequency by mutableStateOf(DoubleArray(samplesSize))
        private set
    var fadedALittleFrequency by mutableStateOf(DoubleArray(samplesSize))
        private set

    private val audioChannel = Channel<DoubleArray>(Int.MAX_VALUE)
    private val samples = DoubleArray(samplesSize)

    suspend fun start() {
        for (sample in audioChannel) {
            samples.copyInto(samples, 0, sample.size)
            sample.copyInto(samples, destinationOffset = samples.size - sample.size)
            //val frames = sec.size / format.frameSize
            //logger.debugElapsed("Computing FFT for $frames frames") {
            val outReal = DoubleArray(samples.size)
            FastFouriers.ITERATIVE_COOLEY_TUKEY.transform(
                samples,
                DoubleArray(samples.size),
                outReal,
                DoubleArray(samples.size)
            )
            outReal.onEachIndexed { index, d -> outReal[index] = 1 - 2.0.pow(-d.absoluteValue / 100) }
            // TODO: Why it's symmetric?
            lastFrequency = outReal
            fadedFrequency = DoubleArray(outReal.size) { index ->
                outReal[index] * fade + fadedFrequency[index] * (1 - fade)
            }
            fadedALittleFrequency = DoubleArray(outReal.size) { index ->
                outReal[index] * fadeALittle + fadedALittleFrequency[index] * (1 - fadeALittle)
            }
        }
    }

    suspend fun push(buf: ByteArray, length: Int, format: AudioFormat) {
        audioChannel.send(decode(buf, length, format))
    }

    private fun decode(bytes: ByteArray, length: Int, format: AudioFormat): DoubleArray {
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
}