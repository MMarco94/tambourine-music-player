package io.github.mmarco94.tambourine.utils


fun <T> Collection<T>.mostCommonOrNull(): T? {
    val numbersByElement = groupingBy { it }.eachCount()
    return numbersByElement.maxByOrNull { it.value }?.key
}

fun DoubleArray.getOrZero(index: Int) = getOrElse(index) { 0.0 }

/**
 * Returns the map with the entries that were added or changed
 */
fun <K, V> Map<K, V>.diff(another: Map<K, V>): Map<K, V> {
    val new = this
    return buildMap {
        new.forEach { (k, v) ->
            if (k !in another || another.getValue(k) != v) {
                put(k, v)
            }
        }
    }
}