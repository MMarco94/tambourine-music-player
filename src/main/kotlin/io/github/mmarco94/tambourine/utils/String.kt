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

private val lineFeed = charArrayOf('\r', '\n')

fun String.forAllLines(f: (start: Int, end: Int) -> Unit) {
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
