package io.github.musicplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.musicplayer.audio.humanHearingRangeLog
import io.github.musicplayer.ui.SpectrometerStyle.AREA
import io.github.musicplayer.ui.SpectrometerStyle.BOXES
import io.github.musicplayer.utils.avgInRange
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
                spectrometerDrawer(chunks, frequencies, boost) { s, e, h ->
                    drawRect(
                        color,
                        topLeft = Offset(s, size.height - h),
                        size = Size(e, h),
                    )
                }
            }

            AREA -> {
                val p = Path().apply {
                    moveTo(0f, size.height)
                    spectrometerDrawer(chunks, frequencies, boost) { s, e, h ->
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
    drawer: (start: Float, end: Float, height: Float) -> Unit,
) {
    repeat(chunks) { chunk ->
        val start = chunk.toFloat() / chunks
        val end = (chunk + 1).toFloat() / chunks
        val frequencyStart = 2.0.pow(humanHearingRangeLog.progress(start))
        val frequencyEnd = 2.0.pow(humanHearingRangeLog.progress(end))
        val amplitude = frequencies.avgInRange(frequencyStart, frequencyEnd)
        val height = (amplitude * boost * size.height).toFloat().coerceIn(0f, size.height)

        drawer(start * size.width, end * size.width, height)
    }
}

@Composable
fun SmallSpectrometers(
    modifier: Modifier,
    frequencies: DoubleArray,
) {
    Spectrometer(modifier, frequencies, chunks = 3, boost = 2f, style = BOXES)
}