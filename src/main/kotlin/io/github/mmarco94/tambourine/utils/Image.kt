package io.github.mmarco94.tambourine.utils

import org.jetbrains.skia.*
import kotlin.math.roundToInt


/**
 * See the implementation of Image.toBitmap() for inspiration
 */
internal fun Image.toBitmap(width: Int): Bitmap {
    val height = ((width * this.height).toFloat() / this.width).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))
    val canvas = Canvas(bitmap)
    canvas.drawImageRect(
        this,
        Rect.makeWH(this.width.toFloat(), this.height.toFloat()),
        Rect.makeWH(width.toFloat(), height.toFloat()),
        SamplingMode.DEFAULT,
        null,
        true,
    )
    bitmap.setImmutable()
    return bitmap
}
