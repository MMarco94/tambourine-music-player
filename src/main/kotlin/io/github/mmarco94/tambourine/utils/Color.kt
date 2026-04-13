package io.github.mmarco94.tambourine.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.github.mmarco94.tambourine.color.MMCQ
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

fun ImageBitmap.palette(maxColors: Int, reduction: Int = 8): List<Color> {
    return MMCQ.quantize(this, maxColors, reduction).palette()
}

data class HSLColor(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
) {
    val color = Color.hsl(hue, saturation, lightness)

    fun darker(strength: Float = 1f) = copy(lightness = lightness / 2f.pow(strength))

    fun lighter(strength: Float = 1f) = copy(lightness = 1 - (1 - lightness) / 1.5f.pow(strength))

    fun makeContrasty(strength: Float = 3f): HSLColor {
        return HSLColor(
            hue,
            // Increasing saturation to compensate for brightness
            (saturation * 1.2f).coerceAtMost(1f),
            when {
                lightness < .5f -> lightness / strength
                else -> 1f - (1f - lightness) / strength
            }
        )
    }

    fun hueDistance(another: HSLColor): Float {
        return minOf(
            (hue - another.hue).absoluteValue,
            (hue - another.hue + 360f).absoluteValue,
            (hue - another.hue - 360f).absoluteValue,
        )
    }

    fun contrast(): HSLColor {
        return HSLColor(
            (hue + 30f).mod(360f),
            saturation / 2f,
            if (lightness < .5f) {
                0.8f + lightness * 2 * 0.2f
            } else {
                0.1f + (1 - lightness) * 2 * 0.15f
            },
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
        return interpolate(copy(saturation = 0.35f, lightness = 1f))
    }

    companion object {
        fun fromRgb(color: Triple<Double, Double, Double>): HSLColor {
            return Color(color.first.toFloat(), color.second.toFloat(), color.third.toFloat()).hsb()
        }
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