package io.github.musicplayer.utils

typealias DoubleRange = ClosedFloatingPointRange<Double>

val DoubleRange.size get() = endInclusive - start
fun DoubleRange.progress(progress: Float): Double {
    return start + progress * (endInclusive - start)
}
