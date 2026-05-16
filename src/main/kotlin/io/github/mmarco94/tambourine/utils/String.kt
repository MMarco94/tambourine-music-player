package io.github.mmarco94.tambourine.utils

fun String.trimToNull(): String? {
    val s = this.trim()
    return s.ifEmpty { null }
}

fun String.substringTrimmed(start: Int, endExclusive: Int = length): String {
    var s = start
    var e = endExclusive

    while (s < e && this[s].isWhitespace()) {
        s++
    }

    while (e > s && this[e - 1].isWhitespace()) {
        e--
    }

    return substring(s, e)
}

@PublishedApi
internal val lineFeed = charArrayOf('\r', '\n')

fun String.countAllLines(): Int {
    var ret = 0
    forAllLines { _, _ -> ret++ }
    return ret
}

inline fun String.forAllLines(f: (start: Int, end: Int) -> Unit) {
    var start = 0
    while (start < length && this[start] in lineFeed) start++

    while (start < length) {
        var next = indexOfAny(lineFeed, start)
        if (next == -1) next = length
        f(start, next)
        start = next + 1

        while (start < length && this[start] in lineFeed) start++
    }
}

fun String.toPositiveIntOrMinusOne(start: Int = 0, end: Int = length): Int {
    if (start >= end) return -1
    var ret = 0
    for (i in start until end) {
        val c = this[i]
        if (c in '0'..'9') {
            ret = ret * 10 + (c - '0')
        } else {
            return -1
        }
    }
    return ret
}
