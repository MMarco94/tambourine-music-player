package io.github.musicplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    chunks: Int,
    boost: Float = 1f,
    style: SpectrometerStyle = AREA,
    color: Color = Color.White,
) {
    Canvas(modifier) {
        when (style) {
            BOXES -> {
                spectrometerDrawer(chunks, frequencies, boost) { s, e, a ->
                    val h = a * size.height
                    drawRect(
                        color,
                        topLeft = Offset(s, size.height - h),
                        size = Size(e - s, h),
                    )
                }
            }

            AREA -> {
                val p = Path().apply {
                    moveTo(0f, size.height)
                    spectrometerDrawer(chunks, frequencies, boost) { s, e, a ->
                        val h = a * size.height
                        lineTo((s + e) / 2f, size.height - h)
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(p, color)
            }
        }
    }
}

private fun DrawScope.spectrometerDrawer(
    chunks: Int,
    frequencies: DoubleArray,
    boost: Float,
    drawer: (start: Float, end: Float, amplitude: Float) -> Unit,
) {
    repeat(chunks) { chunk ->
        val start = chunk.toFloat() / chunks
        val end = (chunk + 1).toFloat() / chunks
        val frequencyStart = 2.0.pow(humanHearingRangeLog.progress(start))
        val frequencyEnd = 2.0.pow(humanHearingRangeLog.progress(end))
        val amplitude = frequencies.avgInRange(frequencyStart, frequencyEnd)
        val heightPercentage = (amplitude * boost).toFloat().coerceIn(0f, size.height)

        drawer(start * size.width, end * size.width, heightPercentage)
    }
}

@Composable
fun SmallSpectrometers(
    modifier: Modifier,
    frequencies: DoubleArray,
) {
    val boost = 2f
    val chunks = 3
    val padding = 2.dp
    Canvas(modifier.background(Color.Black.copy(alpha = 0.5f)).padding(6.dp)) {
        val paddingPx = padding.toPx()
        spectrometerDrawer(chunks, frequencies, boost) { s, e, a ->
            val h = (.1f + a * .9f) * (size.height - 2 * paddingPx)
            drawRect(
                Color.White,
                topLeft = Offset(s + paddingPx, size.height - h - paddingPx),
                size = Size(e - s - 2 * paddingPx, h),
            )
        }
    }
}