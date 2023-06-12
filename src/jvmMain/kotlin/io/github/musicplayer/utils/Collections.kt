package io.github.musicplayer.utils

import kotlin.time.Duration


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

fun List<DoubleArray>.concatenate(): DoubleArray {
    var index = 0
    val ret = DoubleArray(sumOf { it.size })
    forEach {
        it.copyInto(ret, destinationOffset = index)
        index += it.size
    }
    return ret
}

fun DoubleArray.getOrZero(index: Int) = getOrElse(index) { 0.0 }

inline fun DoubleArray.mapInPlace(f: (Double) -> Double): DoubleArray {
    onEachIndexed { index, d -> this[index] = f(d) }
    return this
}

inline fun <T> Iterable<T>.sumOfDuration(selector: (T) -> Duration): Duration {
    var sum: Duration = Duration.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

