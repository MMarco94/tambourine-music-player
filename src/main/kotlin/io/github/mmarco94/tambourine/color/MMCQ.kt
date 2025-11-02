/*
 * This code was heavily inspired by:
 * Java Color Thief
 * by Sven Woltmann, Fonpit AG
 */

package io.github.mmarco94.tambourine.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

object MMCQ {

    fun quantize(image: ImageBitmap, maxColors: Int, sampling: Int, bitsPerColor: Int = 5): CMap {
        val fullVbox = ColorHistogram.build(image, sampling, bitsPerColor)
        val pq = mutableListOf(fullVbox)
        iter(pq, compareBy { it.count }, maxColors)
        pq.sortByDescending { it.count.toFloat() / it.volume }
        return CMap(pq)
    }

    private fun iter(lh: MutableList<VBox>, comparator: Comparator<VBox>, target: Int) {
        // TODO: use priority queue?
        while (lh.size < target) {
            val vbox = lh.filter { it.volume > 1 }.maxWithOrNull(comparator)
            if (vbox == null) {
                // This can't be split further
                return
            } else {
                lh.remove(vbox)
                val (vbox1, vbox2) = vbox.medianCut()
                lh.add(vbox1)
                lh.add(vbox2)
            }
        }
    }

    data class CMap(val vboxes: List<VBox>) {
        fun palette(): List<Color> {
            return vboxes.map { it.avg() }
        }
    }
}