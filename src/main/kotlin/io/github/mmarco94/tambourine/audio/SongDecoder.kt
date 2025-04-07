package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.mmarco94.tambourine.utils.decode
import io.github.mmarco94.tambourine.utils.framesToDuration
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

private val logger = KotlinLogging.logger {}

// Anything below this threshold is considered silence
private const val SILENCE_THRESHOLD = 0.01

// If there's more than half a second of silence, it's probably intentional
val SILENCE_PADDING_LIMIT = 500.milliseconds


class SongDecoder(
    val audio: AsyncAudioInputStream.Reader,
    val format: AudioFormat,
    val song: Song,
) {

    data class DecodedSongData(
        val format: AudioFormat,
        val waveformsPerChannel: List<DoubleArray>,
        val waveformsPerChannelHiRes: List<DoubleArray>,
        val maxAmplitude: Double,
        val analyzedFrames: Long,
        val paddingStartFrames: Long,
        val paddingEndFrames: Long,
        val done: Boolean = false,
    ) {
        fun songStartingSilence(): Duration {
            return format.framesToDuration(paddingStartFrames).coerceAtMost(SILENCE_PADDING_LIMIT)
        }
    }

    private val waveformsPerChannel: List<DoubleArray> = List(format.channels) {
        DoubleArray(RESOLUTION)
    }
    private val waveformsPerChannelHiRes: List<DoubleArray> = List(format.channels) {
        DoubleArray(RESOLUTION)
    }
    private val decodedSongDataFlow: MutableStateFlow<DecodedSongData?> = MutableStateFlow(null)
    val decodedSongData: StateFlow<DecodedSongData?> = decodedSongDataFlow
    private val decodedSongDataChannel = Channel<DecodedSongData>(CONFLATED)

    fun start(scope: CoroutineScope) {
        scope.launch {
            logger.debugElapsed("Computing waveform for ${song.title}") {
                val totalApproximateFrames = song.length.toDouble(DurationUnit.SECONDS) * format.sampleRate
                val framesPerSample = totalApproximateFrames / RESOLUTION
                val framesPerSampleLowRes = totalApproximateFrames / WAVEFORM_LOW_RES_SIZE
                var decodedFrames = 0L
                var maxValue = 0.5
                var firstNonZero = Long.MAX_VALUE
                var lastNonZero = 0L
                while (true) {
                    val chunk = audio.read(Int.MAX_VALUE) ?: break
                    waveformsPerChannelHiRes.forEachIndexed { channel, waveform ->
                        val waveformLowRes = waveformsPerChannel[channel]
                        decode(chunk.readData, chunk.offset, chunk.length, format, channel) { frame, _, value ->
                            val frameIndex = decodedFrames + frame
                            if (value.absoluteValue > SILENCE_THRESHOLD) {
                                firstNonZero = minOf(firstNonZero, frameIndex)
                                lastNonZero = maxOf(lastNonZero, frameIndex)
                            }
                            val idxHiRes =
                                (frameIndex * RESOLUTION / totalApproximateFrames).roundToInt()
                            val idxLowRes =
                                (frameIndex * WAVEFORM_LOW_RES_SIZE / totalApproximateFrames).roundToInt()
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
                    val newData = DecodedSongData(
                        format = format,
                        waveformsPerChannel = waveformsPerChannel,
                        waveformsPerChannelHiRes = waveformsPerChannelHiRes,
                        maxAmplitude = maxValue,
                        analyzedFrames = decodedFrames,
                        paddingStartFrames = if (firstNonZero == Long.MAX_VALUE) decodedFrames else firstNonZero,
                        // Until I've reached the end, I cannot know for sure
                        paddingEndFrames = 0L,
                    )
                    decodedSongDataFlow.value = newData
                    decodedSongDataChannel.send(newData)
                }
                val finalData = DecodedSongData(
                    format = format,
                    maxAmplitude = maxValue,
                    waveformsPerChannel = waveformsPerChannel,
                    waveformsPerChannelHiRes = waveformsPerChannelHiRes,
                    analyzedFrames = decodedFrames,
                    paddingStartFrames = if (firstNonZero == Long.MAX_VALUE) decodedFrames else firstNonZero,
                    paddingEndFrames = decodedFrames - lastNonZero - 1,
                    done = true,
                )
                decodedSongDataFlow.value = finalData
                decodedSongDataChannel.send(finalData)
                logger.debug {
                    "${format.framesToDuration(finalData.paddingStartFrames)} zeros at the beginning; " +
                            "${format.framesToDuration(finalData.paddingEndFrames)} zeros at the end"
                }
            }
        }
    }

    companion object {
        const val WAVEFORM_LOW_RES_SIZE = 240
        private const val WAVEFORM_HIGH_RES_MULTIPLIER = 20
        private const val RESOLUTION = WAVEFORM_LOW_RES_SIZE * WAVEFORM_HIGH_RES_MULTIPLIER
    }
}