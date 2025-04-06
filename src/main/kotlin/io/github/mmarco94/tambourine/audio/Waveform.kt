package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.utils.decode
import io.github.mmarco94.tambourine.utils.mapInPlace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.DurationUnit


data class Waveform(
    val waveformsPerChannel: List<DoubleArray>,
    val waveformsPerChannelHiRes: List<DoubleArray>,
) {
    companion object {
        const val WAVEFORM_LOW_RES_SIZE = 240
        private const val WAVEFORM_HIGH_RES_MULTIPLIER = 20

        suspend fun fromStream(
            audio: AsyncAudioInputStream.Reader,
            format: AudioFormat,
            song: Song,
        ): Waveform {
            val resolution = WAVEFORM_LOW_RES_SIZE * WAVEFORM_HIGH_RES_MULTIPLIER
            val totalApproximateFrames = song.length.toDouble(DurationUnit.SECONDS) * format.sampleRate
            val framesPerSample = totalApproximateFrames / resolution
            val summaries = List(format.channels) {
                DoubleArray(resolution)
            }
            var decodedFrames = 0L
            while (true) {
                val chunk = audio.read(Int.MAX_VALUE) ?: break
                summaries.forEachIndexed { channel, waveform ->
                    decode(chunk.readData, chunk.offset, chunk.length, format, channel) { frame, _, value ->
                        val idx =
                            ((decodedFrames + frame) * resolution / totalApproximateFrames).roundToInt()
                        if (idx in waveform.indices) {
                            waveform[idx] += value.absoluteValue / framesPerSample
                        }
                    }
                }
                decodedFrames += chunk.length / format.frameSize
            }

            // Rescaling, as the avg will bring the max down
            return coroutineScope {
                val hiRes = summaries.map {
                    async {
                        val max = maxOf(.5, it.max())
                        it.mapInPlace { it / max }
                    }
                }.awaitAll()
                val lowRes = hiRes.map {
                    async {
                        val compressed = DoubleArray(WAVEFORM_LOW_RES_SIZE)
                        for ((index, d) in it.withIndex()) {
                            compressed[index / WAVEFORM_HIGH_RES_MULTIPLIER] += d / WAVEFORM_HIGH_RES_MULTIPLIER
                        }
                        compressed
                    }
                }.awaitAll()
                Waveform(lowRes, hiRes)
            }
        }
    }
}