package io.github.mmarco94.tambourine.color

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow

data class VBox(
    val redStart: Int,
    val redEnd: Int,
    val greenStart: Int,
    val greenEnd: Int,
    val blueStart: Int,
    val blueEnd: Int,
    val histogram: ColorHistogram,
) {
    init {
        require(
            redStart >= 0 && greenStart >= 0 && blueStart >= 0 &&
                    redEnd >= redStart && greenEnd >= greenStart && blueEnd >= blueStart
        ) {
            "Index out of bounds"
        }
    }

    val redWidth = redEnd - redStart + 1
    val greenWidth = greenEnd - greenStart + 1
    val blueWidth = blueEnd - blueStart + 1
    val volume = redWidth * greenWidth * blueWidth
    val length get() = volume.toDouble().pow(1.0 / 3.0)
    val count: Int = histogram.getOccurrences(
        redStart, greenStart, blueStart,
        redEnd, greenEnd, blueEnd,
    )

    fun trim(): VBox {
        return ColorAxis.entries.fold(this) { box, axis -> VboxView(box, axis).trim() }
    }

    private enum class ColorAxis { RED, GREEN, BLUE }
    private data class VboxView(
        val vbox: VBox,
        val axis: ColorAxis,
    ) {
        val start = when (axis) {
            ColorAxis.RED -> vbox.redStart
            ColorAxis.GREEN -> vbox.greenStart
            ColorAxis.BLUE -> vbox.blueStart
        }
        val end = when (axis) {
            ColorAxis.RED -> vbox.redEnd
            ColorAxis.GREEN -> vbox.greenEnd
            ColorAxis.BLUE -> vbox.blueEnd
        }
        val width = (end - start) + 1
        val count = vbox.count
        fun changeBounds(start: Int = this.start, end: Int = this.end): VboxView {
            return copy(
                vbox = when (axis) {
                    ColorAxis.RED -> vbox.copy(redStart = start, redEnd = end)
                    ColorAxis.GREEN -> vbox.copy(greenStart = start, greenEnd = end)
                    ColorAxis.BLUE -> vbox.copy(blueStart = start, blueEnd = end)
                }
            )
        }

        fun changeEnd(end: Int): VboxView {
            return changeBounds(end = end)
        }

        fun changeStart(start: Int): VboxView {
            return changeBounds(start = start)
        }

        fun trim(): VBox {
            val newStart = binarySearch(start, end, preferStart = false) {
                (count - changeBounds(start = it).count).compareTo(0)
            } - 1
            val newEnd = binarySearch(newStart, end, preferStart = true) {
                changeBounds(end = it).count.compareTo(count)
            }
            return changeBounds(newStart, newEnd).vbox
        }

        fun medianCut(): Pair<VBox, VBox> {
            val split = (start..end).minBy { abs(changeEnd(it).count - count / 2) }
            return changeEnd(split).vbox.trim() to changeStart(split + 1).vbox.trim()
        }
    }

    fun medianCut(): Pair<VBox, VBox> {
        val view = if (redWidth >= greenWidth && redWidth >= blueWidth) {
            VboxView(this, ColorAxis.RED)
        } else if (greenWidth >= blueWidth) {
            VboxView(this, ColorAxis.GREEN)
        } else {
            VboxView(this, ColorAxis.BLUE)
        }
        return view.medianCut()
    }

    fun avg(): Color {
        val colorRestorationFactor = (1 shl (8 - histogram.bitsPerColor)) / 256f

        return if (this.count > 0) {
            var rsum = 0
            var gsum = 0
            var bsum = 0

            for (r in redStart..redEnd) {
                for (g in greenStart..greenEnd) {
                    for (b in blueStart..blueEnd) {
                        val hval = histogram.getOccurrences(r, g, b)
                        rsum += hval * r
                        gsum += hval * g
                        bsum += hval * b
                    }
                }
            }
            Color(
                ((rsum + 0.5f) * colorRestorationFactor / this.count),
                ((gsum + 0.5f) * colorRestorationFactor / this.count),
                ((bsum + 0.5f) * colorRestorationFactor / this.count),
            )
        } else {
            Color(
                colorRestorationFactor * redWidth / 2f,
                colorRestorationFactor * greenWidth / 2f,
                colorRestorationFactor * blueWidth / 2f,
            )
        }
    }
}

/**
 * Binary search on an IntRange.
 *
 * @param compare function that compares the mid-index to the target.
 *        return <0 if mid is too small,
 *               >0 if mid is too large,
 *                0 if mid is the answer
 * @return the index in the range that satisfies compare(mid) == 0, or the insertion point
 */
internal fun binarySearch(
    start: Int,
    end: Int,
    preferStart: Boolean,
    compare: (Int) -> Int,
): Int {
    var start = start
    var end = end
    while (start <= end) {
        val mid = start + (end - start) / 2
        val c = compare(mid)
        if (c < 0 || (c == 0 && !preferStart)) {
            start = mid + 1
        } else {
            end = mid - 1
        }
    }
    return start
}