package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.ui.SpectrometerStyle.AREA
import io.github.mmarco94.tambourine.ui.SpectrometerStyle.BOXES
import io.github.mmarco94.tambourine.utils.avgInRange
import io.github.mmarco94.tambourine.utils.humanHearingRangeLog
import io.github.mmarco94.tambourine.utils.progress
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

enum class SpectrometerStyle {
    BOXES, AREA
}

@Composable
fun Spectrometer(
    modifier: Modifier,
    frequencies: DoubleArray,
    chunks: Density.(Size) -> Int,
    boost: Float = 1f,
    style: SpectrometerStyle = AREA,
    invert: Boolean = false,
    linear: Boolean = false,
    brush: Density.(Size) -> Brush,
) {
    Canvas(modifier) {
        when (style) {
            BOXES -> {
                spectrometerDrawer(chunks(size), frequencies, boost, linear) { s, e, a ->
                    val h = a * size.height
                    drawRect(
                        brush(size),
                        topLeft = Offset(s, if (invert) 0f else size.height - h),
                        size = Size(e - s, h),
                    )
                }
            }

            AREA -> {
                val p = Path().apply {
                    moveTo(0f, if (invert) 0f else size.height)
                    spectrometerDrawer(chunks(size), frequencies, boost, linear) { s, e, a ->
                        val h = a * size.height
                        lineTo((s + e) / 2f, if (invert) h else size.height - h)
                    }
                    lineTo(size.width, if (invert) 0f else size.height)
                    close()
                }
                drawPath(p, brush(size))
            }
        }
    }
}

private inline fun DrawScope.spectrometerDrawer(
    chunks: Int,
    frequencies: DoubleArray,
    boost: Float,
    linear: Boolean = false,
    drawer: (start: Float, end: Float, amplitude: Float) -> Unit,
) {
    repeat(chunks) { chunk ->
        val start = chunk.toFloat() / chunks
        val end = (chunk + 1).toFloat() / chunks
        val amplitude = if (linear) {
            frequencies.avgInRange(start.toDouble() * frequencies.size, end.toDouble() * frequencies.size)
        } else {
            val frequencyStart = 2.0.pow(humanHearingRangeLog.progress(start))
            val frequencyEnd = 2.0.pow(humanHearingRangeLog.progress(end))
            frequencies.avgInRange(frequencyStart, frequencyEnd)
        }
        val heightPercentage = (amplitude * boost).toFloat().coerceIn(0f, size.height)

        drawer(start * size.width, end * size.width, heightPercentage)
    }
}

@Composable
fun SmallFakeSpectrometers(
    modifier: Modifier,
    player: PlayerController,
    color: Color = Color.White,
) {
    val position = player.Position()
    val songLength = player.queue?.currentSong?.length
    val waveform = player.waveform
    val amplitude: Float = if (player.pause) {
        0f
    } else
        if (songLength != null && waveform != null) {
            val percent = position / songLength
            val realAmpl = waveform.waveformsPerChannelHiRes.maxOf {
                val index = (it.size * percent).roundToInt().coerceIn(it.indices)
                it[index].toFloat()
            }
            0.1f + sqrt(realAmpl) * 0.9f
        } else {
            0.2f
        }

    val ampl by animateFloatAsState(amplitude)
    val low by FakeAnimatedValue(position / 400.milliseconds, spring(stiffness = Spring.StiffnessVeryLow))
    val mid by FakeAnimatedValue(position / 200.milliseconds, spring(stiffness = Spring.StiffnessMedium))
    val high by FakeAnimatedValue(position / 100.milliseconds, spring(stiffness = Spring.StiffnessHigh))
    val values = listOf(low * ampl, mid * ampl, high * ampl)

    val padding = 2.dp
    Canvas(modifier.padding(padding)) {
        val paddingPx = padding.toPx()
        for ((index, height) in values.withIndex()) {
            val start = index.toFloat() / values.size
            val end = (index + 1).toFloat() / values.size
            val s = start * size.width
            val e = end * size.width
            val h = height * size.height
            drawRect(
                color,
                topLeft = Offset(s + paddingPx, size.height - h - paddingPx),
                size = Size(e - s - 2 * paddingPx, h),
            )
        }
    }
}

@Composable
private fun FakeAnimatedValue(
    seed: Double,
    animationSpec: AnimationSpec<Float> = spring(),
): State<Float> {
    val value = Random(seed.roundToInt()).nextFloat()
    return animateFloatAsState(value, animationSpec)
}
