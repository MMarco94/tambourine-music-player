package io.github.musicplayer.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import de.androidpit.colorthief.MMCQ
import kotlin.math.roundToInt

fun ImageBitmap.palette(): List<Color> {
    val colors = IntArray(width * height)
    this.readPixels(colors)
    val colors2D = Array(width * height) { idx ->
        val color = colors[idx]
        intArrayOf(
            color.shr(16) and 0xff,
            color.shr(8) and 0xff,
            color and 0xff,
        )
    }
    return MMCQ.quantize(colors2D, 3)
        .palette()
        .map { (r, g, b) -> Color(r, g, b) }
}

data class HSLColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
) {
    val color = Color.hsl(hue, saturation, lightness)

    fun darker() = copy(lightness = lightness / 2f)

    fun contrast(): HSLColor {
        return HSLColor(
            (hue + 30f).mod(360f),
            saturation / 2f,
            when {
                lightness < .2f -> .8f
                lightness < .4f -> .9f
                lightness < .5f -> 1f
                lightness < .6f -> .0f
                lightness < .8f -> .1f
                else -> .2f
            }
        )
    }

    fun shift(hueDelta: Float = 0f, saturationDelta: Float = 0f, lightnessDelta: Float = 0f): HSLColor {
        return HSLColor(
            (hue + hueDelta).mod(360f),
            (saturation + saturationDelta).coerceIn(0f, 1f),
            (lightness + lightnessDelta).coerceIn(0f, 1f),
        )
    }

    fun interpolate(another: HSLColor, progress: Float = .5f): HSLColor {
        require(progress in 0.0..1.0)
        return HSLColor(
            (hue..another.hue).progress(progress),
            (saturation..another.saturation).progress(progress),
            (lightness..another.lightness).progress(progress),
        )
    }

    fun pastel(): HSLColor {
        val base = shift(saturationDelta = .1f)
        return base.interpolate(
            base.copy(saturation = 0.25f, lightness = 1f),
        )
    }
}

fun Color.hsb(): HSLColor {
    val r = (this.red * 255).roundToInt()
    val g = (this.green * 255).roundToInt()
    val b = (this.blue * 255).roundToInt()
    val hsb = java.awt.Color.RGBtoHSB(r, g, b, null)
    val lightness = arrayOf(r, g, b).apply {
        sort()
    }[1]
    return HSLColor(hsb[0] * 360, hsb[1], lightness / 255f)
}