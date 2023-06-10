package io.github.musicplayer.utils

import kotlin.math.abs
import kotlin.math.log10


fun Int.digits() = when (this) {
    0 -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}

fun Long.toIntOrMax(): Int {
    return if (this >= Int.MAX_VALUE) Int.MAX_VALUE
    else toInt()
}