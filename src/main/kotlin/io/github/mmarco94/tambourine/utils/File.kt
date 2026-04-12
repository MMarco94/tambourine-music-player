package io.github.mmarco94.tambourine.utils

import java.nio.file.Path

fun pathSimilarity(first: Path, second: Path): Int {
    var ret = 0
    var p1: Path? = first
    var p2: Path? = second
    while (p1 != null && p2 != null && p1.fileName == p2.fileName) {
        p1 = p1.parent
        p2 = p2.parent
        ret++
    }
    return ret
}

val Path.withoutExtension: String
    get() = toString().substringBeforeLast(".")
