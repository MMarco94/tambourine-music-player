package io.github.musicplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.musicplayer.ui.SpectrometerStyle.AREA
import io.github.musicplayer.ui.SpectrometerStyle.BOXES
import io.github.musicplayer.utils.avgInRange
import io.github.musicplayer.utils.humanHearingRangeLog
import io.github.musicplayer.utils.progress
import kotlin.math.pow

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
fun SmallSpectrometers(
    modifier: Modifier,
    frequencies: DoubleArray,
    color: Color = Color.White,
) {
    val boost = 2f
    val chunks = 3
    val padding = 2.dp
    Canvas(modifier.padding(padding)) {
        val paddingPx = padding.toPx()
        spectrometerDrawer(chunks, frequencies, boost) { s, e, a ->
            val h = (.1f + a * .9f) * (size.height - 2 * paddingPx)
            drawRect(
                color,
                topLeft = Offset(s + paddingPx, size.height - h - paddingPx),
                size = Size(e - s - 2 * paddingPx, h),
            )
        }
    }
}