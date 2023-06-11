package io.github.musicplayer.utils


fun <T> Collection<T>.rangeOfOrNull(f: (T) -> Int?): IntRange? {
    var min: Int? = null
    var max: Int? = null
    forEach {
        val new = f(it)
        if (new != null) {
            if (min == null || new < min!!) {
                min = new
            }
            if (max == null || new > max!!) {
                max = new
            }
        }
    }
    return if (min != null) (min!!..max!!)
    else null
}

fun <T> Collection<T>.mostCommonOrNull(): T? {
    val numbersByElement = groupingBy { it }.eachCount()
    return numbersByElement.maxByOrNull { it.value }?.key
}

fun ByteArray.countZeros(start: Int = 0, end: Int = size): Int {
    for (i in start until end) {
        if (this[i] != 0.toByte()) return i
    }
    return end - start
}

fun List<DoubleArray>.concatenate(): DoubleArray {
    var arr = 0
    var offset = 0
    return DoubleArray(sumOf { it.size }) {
        if (offset >= this[arr].size) {
            arr++
            offset = 0
        }
        this[arr][offset++]
    }
}

fun DoubleArray.getOrZero(index: Int) = getOrElse(index) { 0.0 }

inline fun DoubleArray.mapInPlace(f: (Double) -> Double): DoubleArray {
    onEachIndexed { index, d -> this[index] = f(d) }
    return this
}