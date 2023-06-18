package io.github.musicplayer.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import de.androidpit.colorthief.MMCQ
import org.jetbrains.skia.Bitmap
import kotlin.math.roundToInt

fun ImageBitmap.palette(reduction: Int = 10): List<Color> {
    val pixels = getPixels(asSkiaBitmap(), reduction)
    return MMCQ.quantize(pixels, 3)
        .palette()
        .map { (r, g, b) -> Color(r, g, b) }
}

private fun getPixels(
    sourceImage: Bitmap,
    reduction: Int,
): Array<IntArray> {
    val width = sourceImage.width
    val height = sourceImage.height
    val pixelCount = width * height
    // numRegardedPixels must be rounded up to avoid an ArrayIndexOutOfBoundsException if all
    // pixels are good.
    val numRegardedPixels = (pixelCount + reduction - 1) / reduction
    return Array(numRegardedPixels) { idx ->
        val i = idx * reduction
        val row = i / width
        val col = i % width
        val rgb = sourceImage.getColor(col, row)
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        intArrayOf(r, g, b)
    }
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