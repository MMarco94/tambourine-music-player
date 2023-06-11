package io.github.musicplayer.utils

import androidx.compose.ui.unit.Dp

typealias FloatRange = ClosedFloatingPointRange<Float>
typealias DoubleRange = ClosedFloatingPointRange<Double>
typealias DpRange = ClosedRange<Dp>

val DoubleRange.size get() = endInclusive - start
fun DoubleRange.progress(progress: Float): Double {
    return start + progress * (endInclusive - start)
}

fun FloatRange.progress(progress: Float): Float {
    return start + progress * (endInclusive - start)
}
