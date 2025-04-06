package io.github.mmarco94.tambourine.audio

import androidx.compose.runtime.mutableStateOf
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.mmarco94.tambourine.utils.decode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

private val logger = KotlinLogging.logger {}

class WaveformComputer(
    val audio: AsyncAudioInputStream.Reader,
    val format: AudioFormat,
    val song: Song,
) : AutoCloseable {

    private val scope = CoroutineScope(Dispatchers.Default)

    data class Waveform(
        val max: Double,
        val waveformsPerChannel: List<DoubleArray>,
        val waveformsPerChannelHiRes: List<DoubleArray>,
        val analyzedFrames: Long,
    )

    private val waveformsPerChannel: List<DoubleArray> = List(format.channels) {
        DoubleArray(RESOLUTION)
    }
    private val waveformsPerChannelHiRes: List<DoubleArray> = List(format.channels) {
        DoubleArray(RESOLUTION)
    }
    val waveform = mutableStateOf(
        Waveform(
            max = 0.5,
            waveformsPerChannel = waveformsPerChannel,
            waveformsPerChannelHiRes = waveformsPerChannelHiRes,
            analyzedFrames = 0,
        )
    )
    private val waveformChannel = Channel<Waveform>(CONFLATED)

    fun start() {
        scope.launch(Dispatchers.Swing) {
            for (w in waveformChannel) {
                waveform.value = w
            }
        }
        scope.launch {
            logger.debugElapsed("Computing waveform for ${song.title}") {
                val totalApproximateFrames = song.length.toDouble(DurationUnit.SECONDS) * format.sampleRate
                val framesPerSample = totalApproximateFrames / RESOLUTION
                val framesPerSampleLowRes = totalApproximateFrames / WAVEFORM_LOW_RES_SIZE
                var decodedFrames = 0L
                var maxValue = 0.5
                while (true) {
                    val chunk = audio.read(Int.MAX_VALUE) ?: break
                    waveformsPerChannelHiRes.forEachIndexed { channel, waveform ->
                        val waveformLowRes = waveformsPerChannel[channel]
                        decode(chunk.readData, chunk.offset, chunk.length, format, channel) { frame, _, value ->
                            val idxHiRes = ((decodedFrames + frame) * RESOLUTION / totalApproximateFrames).roundToInt()
                            val idxLowRes =
                                ((decodedFrames + frame) * WAVEFORM_LOW_RES_SIZE / totalApproximateFrames).roundToInt()
                            if (idxHiRes in waveform.indices) {
                                waveform[idxHiRes] += value.absoluteValue / framesPerSample
                                maxValue = maxOf(maxValue, waveform[idxHiRes])
                            }
                            if (idxLowRes in waveformLowRes.indices) {
                                waveformLowRes[idxLowRes] += value.absoluteValue / framesPerSampleLowRes
                            }
                        }
                    }
                    decodedFrames += chunk.length / format.frameSize
                    waveformChannel.send(
                        Waveform(maxValue, waveformsPerChannel, waveformsPerChannelHiRes, decodedFrames)
                    )
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    companion object {
        const val WAVEFORM_LOW_RES_SIZE = 240
        private const val WAVEFORM_HIGH_RES_MULTIPLIER = 20
        private const val RESOLUTION = WAVEFORM_LOW_RES_SIZE * WAVEFORM_HIGH_RES_MULTIPLIER
    }
}