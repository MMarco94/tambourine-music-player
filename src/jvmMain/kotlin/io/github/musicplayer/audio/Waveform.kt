package io.github.musicplayer.audio

import io.github.musicplayer.utils.avgInRange
import io.github.musicplayer.utils.decode
import io.github.musicplayer.utils.mapInPlace
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.sqrt

data class Waveform(
    val decodedAudioChannel: List<DoubleArray>,
    val summaryChannel: List<DoubleArray>,
) {
    companion object {
        const val summaryLength = 1000

        fun fromBytes(audio: ByteArray, format: AudioFormat): Waveform {
            val decoded = (0 until format.channels).map { channel ->
                decode(audio, audio.size, format, channel)
                    .mapInPlace { sqrt(it.absoluteValue) }
            }
            return Waveform(
                decoded,
                decoded.map { summarize(it, summaryLength) }
            )
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