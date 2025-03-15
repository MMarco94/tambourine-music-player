package io.github.mmarco94.tambourine.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.fft4j.FastFouriers
import io.github.mmarco94.tambourine.utils.decodeToArray
import io.github.mmarco94.tambourine.utils.mapInPlace
import kotlinx.coroutines.channels.Channel
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.pow

private const val samplesSize = 1.shl(15) //32k
private val imaginaryPart = DoubleArray(samplesSize)
private val junk = DoubleArray(samplesSize)
private const val fade = 0.1
private const val fadeALittle = 0.3

class FrequencyAnalyzer {
    var lastFrequency by mutableStateOf(DoubleArray(samplesSize))
        private set
    var fadedFrequency by mutableStateOf(DoubleArray(samplesSize))
        private set
    var fadedALittleFrequency by mutableStateOf(DoubleArray(samplesSize))
        private set

    private class AudioData(val chunk: Chunk, val format: AudioFormat)

    private val audioChannel = Channel<AudioData>(Channel.CONFLATED)
    private val samples = DoubleArray(samplesSize)

    suspend fun start() {
        for (audioData in audioChannel) {
            val sample =
                decodeToArray(
                    audioData.chunk.readData,
                    audioData.chunk.offset,
                    audioData.chunk.length,
                    audioData.format
                )
            if (sample.size < samples.size) {
                samples.copyInto(samples, 0, sample.size)
            }
            val destinationOffset = samples.size - sample.size
            sample.copyInto(
                samples,
                destinationOffset = destinationOffset.coerceAtLeast(0),
                startIndex = (-destinationOffset).coerceAtLeast(0)
            )

            val outReal = DoubleArray(samples.size)
            FastFouriers.ITERATIVE_COOLEY_TUKEY.transform(
                samples,
                imaginaryPart,
                outReal,
                junk
            )
            outReal.mapInPlace { 1 - 2.0.pow(-it.absoluteValue / 100) }
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

    suspend fun push(chunk: Chunk, format: AudioFormat) {
        audioChannel.send(AudioData(chunk, format))
    }
}