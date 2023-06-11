package io.github.musicplayer.audio

import io.github.musicplayer.utils.avgInRange
import io.github.musicplayer.utils.concatenate
import io.github.musicplayer.utils.decode
import io.github.musicplayer.utils.mapInPlace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.sqrt

data class Waveform(
    val decodedAudioChannel: List<DoubleArray>,
    val summaryChannel: List<DoubleArray>,
) {
    companion object {
        const val summaryLength = 480

        suspend fun fromStream(audio: AsyncAudioInputStream.Reader, format: AudioFormat): Waveform {
            val allDecoded = List(format.channels) {
                mutableListOf<DoubleArray>()
            }
            while (true) {
                val chunk = audio.read(Int.MAX_VALUE) ?: break
                allDecoded.forEachIndexed { channel, all ->
                    val decoded = decode(chunk.readData, chunk.offset, chunk.length, format, channel)
                        .mapInPlace { sqrt(it.absoluteValue) }
                    all.add(decoded)
                }
            }

            return coroutineScope {
                val concatenatedAndSummarized = allDecoded.map {
                    async {
                        val concatenate = it.concatenate()
                        concatenate to summarize(concatenate, summaryLength)
                    }
                }.awaitAll()
                Waveform(
                    concatenatedAndSummarized.map { it.first },
                    concatenatedAndSummarized.map { it.second },
                )
            }
        }

        private fun summarize(arr: DoubleArray, size: Int): DoubleArray {
            val ret = DoubleArray(size) { chunk ->
                val start = chunk.toDouble() / size
                val end = (chunk + 1).toDouble() / size
                arr.avgInRange(start * arr.size, end * arr.size)
            }
            // Rescaling, as the avg will bring the max down
            val max = maxOf(.5, ret.max())
            return ret.mapInPlace { it / max }
        }
    }
}