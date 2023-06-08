package utils


fun Long.chunked(chunk: Int, f: (chunk: Int) -> Unit) {
    require(this >= 0)
    require(chunk >= 0)
    var done = 0L
    while (done < this) {
        val c = minOf(chunk.toLong(), this - done).toInt()
        f(c)
        done += c
    }
}

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