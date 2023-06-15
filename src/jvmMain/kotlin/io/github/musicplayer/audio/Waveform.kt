package io.github.musicplayer.audio

import io.github.musicplayer.data.Song
import io.github.musicplayer.utils.decode
import io.github.musicplayer.utils.mapInPlace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

data class Waveform(
    val waveformsPerChannel: List<DoubleArray>,
) {
    companion object {
        const val summaryLength = 480

        suspend fun fromStream(
            audio: AsyncAudioInputStream.Reader,
            format: AudioFormat,
            song: Song,
        ): Waveform {
            val totalApproximateFrames = song.length.toDouble(DurationUnit.SECONDS) * format.sampleRate
            val framesPerSample = totalApproximateFrames / summaryLength
            val summaries = List(format.channels) {
                DoubleArray(summaryLength)
            }
            var decodedFrames = 0
            while (true) {
                val chunk = audio.read(Int.MAX_VALUE) ?: break
                summaries.forEachIndexed { channel, waveform ->
                    decode(chunk.readData, chunk.offset, chunk.length, format, channel) { frame, _, value ->
                        val idx =
                            ((decodedFrames + frame).toLong() * summaryLength / totalApproximateFrames).roundToInt()
                        if (idx in waveform.indices) {
                            waveform[idx] += value.absoluteValue / framesPerSample
                        }
                    }
                }
                decodedFrames += chunk.length / format.frameSize
            }

            // Rescaling, as the avg will bring the max down
            return coroutineScope {
                val scaled = summaries.map {
                    async {
                        val max = maxOf(.5, it.max())
                        it.mapInPlace { it / max }
                    }
                }.awaitAll()
                Waveform(scaled)
            }
        }
    }
}