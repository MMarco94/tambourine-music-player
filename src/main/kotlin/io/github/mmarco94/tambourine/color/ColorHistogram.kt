package io.github.mmarco94.tambourine.color

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import io.github.mmarco94.tambourine.color.ColorHistogram.Companion.idx

@PublishedApi
internal const val BITS_PER_COLOR = 5
private const val COLOR_RANGE_SIZE = 1 shl BITS_PER_COLOR

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

        private const val R_IDX_INCREMENTS = 1 shl (2 * BITS_PER_COLOR)
        private const val G_IDX_INCREMENTS = 1 shl BITS_PER_COLOR
        private const val B_IDX_INCREMENTS = 1
        private const val RG_IDX_INCREMENTS = R_IDX_INCREMENTS + G_IDX_INCREMENTS
        private const val RB_IDX_INCREMENTS = R_IDX_INCREMENTS + B_IDX_INCREMENTS
        private const val GB_IDX_INCREMENTS = G_IDX_INCREMENTS + B_IDX_INCREMENTS
        private const val RGB_IDX_INCREMENTS = R_IDX_INCREMENTS + G_IDX_INCREMENTS + B_IDX_INCREMENTS

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
            return VBox(
                0,
                maxColorValue,
                0,
                maxColorValue,
                0,
                maxColorValue,
                ColorHistogram(partialSums = histogram.apply { toPartialSum(this) }),
            ).trim()
        }

        /**
         * Transforming histogram in-place to a tensor of partial sums.
         * This is a bit mad, but it's a hotspot, and writing without ifs is a big speed-up.
         *
         * This is rughly equivalent to, but unrolled so the first iteration of the loop is separate:
         *             var idx = 0
         *             for (r in colorRange) {
         *                 for (g in colorRange) {
         *                     for (b in colorRange) {
         *                         var sum = 0
         *                         if (r > 0) {
         *                             sum += histogram[idx - R_IDX_INCREMENTS]
         *                             if (g > 0) sum -= histogram[idx - RG_IDX_INCREMENTS]
         *                             if (b > 0) {
         *                                 sum -= histogram[idx - RB_IDX_INCREMENTS]
         *                                 if (g > 0) sum += histogram[idx - RGB_IDX_INCREMENTS]
         *                             }
         *                         }
         *                         if (g > 0) sum += histogram[idx - G_IDX_INCREMENTS]
         *                         if (b > 0) {
         *                             sum += histogram[idx - B_IDX_INCREMENTS]
         *                             if (g > 0) sum -= histogram[idx - GB_IDX_INCREMENTS]
         *                         }
         *                         histogram[idx] += sum
         *                         idx += 1
         *                     }
         *                 }
         *             }
         */
        private fun toPartialSum(histogram: IntArray) {
            var idx = 1
            repeat(COLOR_RANGE_SIZE - 1) {
                histogram[idx] += histogram[idx - B_IDX_INCREMENTS]
                idx++
            }
            repeat(COLOR_RANGE_SIZE - 1) {
                histogram[idx] += histogram[idx - G_IDX_INCREMENTS]
                idx++
                repeat(COLOR_RANGE_SIZE - 1) {
                    histogram[idx] += histogram[idx - G_IDX_INCREMENTS] +
                            histogram[idx - B_IDX_INCREMENTS] -
                            histogram[idx - GB_IDX_INCREMENTS]
                    idx++
                }
            }
            repeat(COLOR_RANGE_SIZE - 1) {
                histogram[idx] += histogram[idx - R_IDX_INCREMENTS]
                idx++
                repeat(COLOR_RANGE_SIZE - 1) {
                    histogram[idx] += histogram[idx - R_IDX_INCREMENTS] -
                            histogram[idx - RB_IDX_INCREMENTS] +
                            histogram[idx - B_IDX_INCREMENTS]
                    idx++
                }
                repeat(COLOR_RANGE_SIZE - 1) {
                    histogram[idx] += histogram[idx - R_IDX_INCREMENTS] -
                            histogram[idx - RG_IDX_INCREMENTS] +
                            histogram[idx - G_IDX_INCREMENTS]
                    idx++
                    repeat(COLOR_RANGE_SIZE - 1) {
                        histogram[idx] += histogram[idx - R_IDX_INCREMENTS] -
                                histogram[idx - RG_IDX_INCREMENTS] -
                                histogram[idx - RB_IDX_INCREMENTS] +
                                histogram[idx - RGB_IDX_INCREMENTS] +
                                histogram[idx - G_IDX_INCREMENTS] +
                                histogram[idx - B_IDX_INCREMENTS] -
                                histogram[idx - GB_IDX_INCREMENTS]
                        idx++
                    }
                }
            }
        }
    }
}