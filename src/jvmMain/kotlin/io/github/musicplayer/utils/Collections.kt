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

fun DoubleArray.getOrZero(index: Int) = getOrElse(index) { 0.0 }

inline fun DoubleArray.mapInPlace(f: (Double) -> Double): DoubleArray {
    onEachIndexed { index, d -> this[index] = f(d) }
    return this
}

/**
 * Returns the map with the entries that were added or changed
 */
fun <K, V> Map<K, V>.diff(another: Map<K, V>): Map<K, V> {
    val new = this
    return buildMap {
        new.forEach { k, v ->
            if (k !in another || another.getValue(k) != v) {
                put(k, v)
            }
        }
    }
}