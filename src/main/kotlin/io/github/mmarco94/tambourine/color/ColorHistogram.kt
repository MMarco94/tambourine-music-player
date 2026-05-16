package io.github.mmarco94.tambourine.color

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap

@PublishedApi
internal const val BITS_PER_COLOR = 5
private val colorRange = 0 until (1 shl BITS_PER_COLOR)

class ColorHistogram(
    val partialSums: IntArray
) {

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

    fun getOccurrences(red: Int, green: Int, blue: Int): Int {
        return getOccurrences(red, green, blue, red, green, blue)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun idx(r: Int, g: Int, b: Int): Int {
        return (r shl BITS_PER_COLOR shl BITS_PER_COLOR) + (g shl BITS_PER_COLOR) + b
    }

    companion object {

        @Suppress("NOTHING_TO_INLINE")
        private inline fun idx(r: Int, g: Int, b: Int): Int {
            return (r shl (2 * BITS_PER_COLOR)) + (g shl BITS_PER_COLOR) + b
        }

        fun build(image: ImageBitmap, sampling: Int): VBox {
            val bitmap = image.asSkiaBitmap()
            val histogram = IntArray(1 shl (3 * BITS_PER_COLOR))
            val maxColorValue = (1 shl BITS_PER_COLOR) - 1
            val shift: Int = 8 - BITS_PER_COLOR
            val imgWidth = bitmap.width
            for (i in 0 until imgWidth * image.height step sampling) {
                val pixel = bitmap.getColor(i % imgWidth, i / imgWidth)
                val r = (pixel shr 16 and 0xFF) shr shift
                val g = (pixel shr 8 and 0xFF) shr shift
                val b = (pixel and 0xFF) shr shift
                val index = idx(r, g, b)
                histogram[index]++
            }
            // Transforming histogram to a tensor of partial sums
            for (r in colorRange) {
                for (g in colorRange) {
                    for (b in colorRange) {
                        var sum = histogram[idx(r, g, b)]
                        if (r > 0) sum += histogram[idx(r - 1, g, b)]
                        if (g > 0) sum += histogram[idx(r, g - 1, b)]
                        if (b > 0) sum += histogram[idx(r, g, b - 1)]
                        if (r > 0 && g > 0) sum -= histogram[idx(r - 1, g - 1, b)]
                        if (r > 0 && b > 0) sum -= histogram[idx(r - 1, g, b - 1)]
                        if (g > 0 && b > 0) sum -= histogram[idx(r, g - 1, b - 1)]
                        if (r > 0 && g > 0 && b > 0) sum += histogram[idx(r - 1, g - 1, b - 1)]
                        histogram[idx(r, g, b)] = sum
                    }
                }
            }
            return VBox(
                0,
                maxColorValue,
                0,
                maxColorValue,
                0,
                maxColorValue,
                ColorHistogram(partialSums = histogram),
            ).trim()
        }
    }
}