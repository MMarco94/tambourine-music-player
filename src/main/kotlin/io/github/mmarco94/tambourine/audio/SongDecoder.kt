package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.mmarco94.tambourine.utils.decode
import io.github.mmarco94.tambourine.utils.durationToFrames
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

// Anything below these threshold is considered silence
private val SILENCE_THRESHOLDS = listOf(
    0.0 to 1000.milliseconds,
    0.01 to 650.milliseconds,
    0.04 to 150.milliseconds,
)
val MAX_SILENCE_PADDING = SILENCE_THRESHOLDS.maxOf { it.second }


class SongDecoder(
    val audio: AsyncAudioInputStream.Reader,
    val format: AudioFormat,
    val song: Song,
) {

    data class SilencePadding(
        val threshold: Double,
        val silenceFrameStart: Long,
        val silenceFrameEnd: Long,
        val silenceCapFrames: Long,
    ) {
        fun silenceAtStart(): Long {
            return silenceFrameStart.coerceAtMost(silenceCapFrames)
        }

        fun silenceAtEnd(): Long {
            return silenceFrameEnd.coerceAtMost(silenceCapFrames)
        }
    }

    data class DecodedSongData(
        val format: AudioFormat,
        val waveformsPerChannel: List<DoubleArray>,
        val maxAmplitude: Double,
        val analyzedFrames: Long,
        val silencePaddings: List<SilencePadding>,
        val done: Boolean = false,
    ) {
        fun songSilenceStart(): Duration {
            val padStart = silencePaddings.maxOf { it.silenceAtStart() }
            return format.framesToDuration(padStart)
        }

        fun songSilenceEnd(): Duration {
            val padEnd = silencePaddings.maxOf { it.silenceAtEnd() }
            return format.framesToDuration(padEnd)
        }

        fun songNonSilentLengthFrames(): Long {
            val padEnd = silencePaddings.maxOf { it.silenceAtEnd() }
            return analyzedFrames - padEnd
        }
    }

    private val waveformsPerChannel: List<DoubleArray> = List(format.channels) {
        DoubleArray(WAVEFORM_SIZE)
    }
    private val decodedSongDataFlow: MutableStateFlow<DecodedSongData?> = MutableStateFlow(null)
    val decodedSongData: StateFlow<DecodedSongData?> = decodedSongDataFlow
    private val decodedSongDataChannel = Channel<DecodedSongData>(CONFLATED)

    private class SilenceCalculator(val threshold: Double, val silenceCapFrames: Long) {
        var firstNonZero = Long.MAX_VALUE
        var lastNonZero = 0L
        fun toPadding(totalFrames: Long, done: Boolean) = SilencePadding(
            threshold = threshold,
            silenceFrameStart = if (firstNonZero == Long.MAX_VALUE) totalFrames else firstNonZero,
            silenceFrameEnd = if (done) totalFrames - lastNonZero - 1 else 0L,
            silenceCapFrames = silenceCapFrames,
        )

        fun register(value: Double, frameIndex: Long) {
            if (value.absoluteValue > threshold) {
                firstNonZero = minOf(firstNonZero, frameIndex)
                lastNonZero = maxOf(lastNonZero, frameIndex)
            }
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch {
            logger.debugElapsed("Computing waveform for ${song.title}") {
                val totalApproximateFrames = song.length.toDouble(DurationUnit.SECONDS) * format.sampleRate
                val framesPerSample = totalApproximateFrames / WAVEFORM_SIZE
                var decodedFrames = 0L
                val thresholds = SILENCE_THRESHOLDS.map { (threshold, duration) ->
                    SilenceCalculator(threshold, format.durationToFrames(duration))
                }
                var maxValue = 0.5
                while (true) {
                    val chunk = audio.read(Int.MAX_VALUE) ?: break
                    waveformsPerChannel.forEachIndexed { channel, waveform ->
                        decode(chunk.readData, chunk.offset, chunk.length, format, channel) { frame, _, value ->
                            val frameIndex = decodedFrames + frame
                            for (i in thresholds.indices) {
                                thresholds[i].register(value, frameIndex)
                            }
                            val idx = (frameIndex * WAVEFORM_SIZE / totalApproximateFrames).roundToInt()
                            if (idx in waveform.indices) {
                                waveform[idx] += value.absoluteValue / framesPerSample
                            }
                        }
                    }
                    maxValue = waveformsPerChannel.maxOf { it.max() }.coerceAtLeast(maxValue)
                    decodedFrames += chunk.length / format.frameSize
                    val newData = DecodedSongData(
                        format = format,
                        waveformsPerChannel = waveformsPerChannel,
                        maxAmplitude = maxValue,
                        analyzedFrames = decodedFrames,
                        silencePaddings = thresholds.map { it.toPadding(decodedFrames, false) },
                    )
                    decodedSongDataFlow.value = newData
                    decodedSongDataChannel.send(newData)
                }
                val finalData = DecodedSongData(
                    format = format,
                    maxAmplitude = maxValue,
                    waveformsPerChannel = waveformsPerChannel,
                    analyzedFrames = decodedFrames,
                    silencePaddings = thresholds.map { it.toPadding(decodedFrames, true) },
                    done = true,
                )
                decodedSongDataFlow.value = finalData
                decodedSongDataChannel.send(finalData)
                logger.debug {
                    "${finalData.songSilenceStart()} silence at the beginning; " +
                            "${finalData.songSilenceEnd()} silence at the end"
                }
            }
        }
    }

    companion object {
        const val WAVEFORM_SIZE = 240
    }
}