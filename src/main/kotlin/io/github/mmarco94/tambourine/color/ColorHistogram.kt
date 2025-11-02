package io.github.mmarco94.tambourine.color

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap

class ColorHistogram(
    val bitsPerColor: Int,
    val histogram: IntArray
) {
    private val colorRange = 0 until (1 shl bitsPerColor)
    private val partialSums: IntArray = run {
        val ret = IntArray(histogram.size)
        for (r in colorRange) {
            for (g in colorRange) {
                for (b in colorRange) {
                    var sum = getOccurrences(r, g, b)
                    if (r > 0) sum += ret[idx(r - 1, g, b)]
                    if (g > 0) sum += ret[idx(r, g - 1, b)]
                    if (b > 0) sum += ret[idx(r, g, b - 1)]
                    if (r > 0 && g > 0) sum -= ret[idx(r - 1, g - 1, b)]
                    if (r > 0 && b > 0) sum -= ret[idx(r - 1, g, b - 1)]
                    if (g > 0 && b > 0) sum -= ret[idx(r, g - 1, b - 1)]
                    if (r > 0 && g > 0 && b > 0) sum += ret[idx(r - 1, g - 1, b - 1)]
                    ret[idx(r, g, b)] = sum
                }
            }
        }
        ret
    }


    @Suppress("NOTHING_TO_INLINE")
    private inline fun ps(r: Int, g: Int, b: Int): Int {
        if (r < 0 || g < 0 || b < 0) return 0
        return partialSums[idx(r, g, b)]
    }

    fun getOccurrences(
        r1: Int, g1: Int, b1: Int,
        r2: Int, g2: Int, b2: Int,
    ): Int {
        return ps(r2, g2, b2) -
                (ps(r1 - 1, g2, b2) + ps(r2, g1 - 1, b2) + ps(r2, g2, b1 - 1)) +
                (ps(r1 - 1, g1 - 1, b2) + ps(r1 - 1, g2, b1 - 1) + ps(r2, g1 - 1, b1 - 1)) -
                ps(r1 - 1, g1 - 1, b1 - 1)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun getOccurrences(red: Int, green: Int, blue: Int): Int {
        val index = idx(red, green, blue)
        return histogram[index]
    }

    @Suppress("NOTHING_TO_INLINE")
    @PublishedApi
    internal inline fun idx(r: Int, g: Int, b: Int): Int {
        return (r shl bitsPerColor shl bitsPerColor) + (g shl bitsPerColor) + b
    }

    companion object {

        @Suppress("NOTHING_TO_INLINE")
        private inline fun idx(r: Int, g: Int, b: Int, bitsPerColor: Int): Int {
            return (r shl (2 * bitsPerColor)) + (g shl bitsPerColor) + b
        }

        fun build(image: ImageBitmap, sampling: Int, bitsPerColor: Int): VBox {
            val bitmap = image.asSkiaBitmap()
            val histogram = IntArray(1 shl (3 * bitsPerColor))
            val maxColorValue = (1 shl bitsPerColor) - 1
            val shift: Int = 8 - bitsPerColor
            val imgWidth = bitmap.width
            for (i in 0 until imgWidth * image.height step sampling) {
                val pixel = bitmap.getColor(i % imgWidth, i / imgWidth)
                val r = (pixel shr 16 and 0xFF) shr shift
                val g = (pixel shr 8 and 0xFF) shr shift
                val b = (pixel and 0xFF) shr shift
                val index = idx(r, g, b, bitsPerColor)
                histogram[index]++
            }
            return VBox(
                0,
                maxColorValue,
                0,
                maxColorValue,
                0,
                maxColorValue,
                ColorHistogram(bitsPerColor, histogram),
            ).trim()
        }
    }
}