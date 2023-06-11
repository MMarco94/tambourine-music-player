package io.github.musicplayer.utils

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.log2

fun Int.digits() = when (this) {
    0 -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}

fun Long.toIntOrMax(): Int {
    return if (this >= Int.MAX_VALUE) Int.MAX_VALUE
    else toInt()
}

fun IntRange.toLogRange(): ClosedFloatingPointRange<Double> = log2(start.toDouble())..log2(endInclusive.toDouble())


fun DoubleArray.sumInRange(fromIndex: Double, toIndex: Double): Double {
    val fromInclusive = fromIndex.toInt()
    val endInclusive = toIndex.toInt()
    var sum = 0.0
    for (i in fromInclusive..endInclusive) {
        sum += this.getOrZero(i)
    }
    sum -= this.getOrZero(fromInclusive) * (fromIndex - fromInclusive)
    sum -= this.getOrZero(endInclusive) * (endInclusive + 1 - toIndex)
    return sum
}

fun DoubleArray.avgInRange(fromIndex: Double, toIndex: Double): Double {
    return sumInRange(fromIndex, toIndex) / (toIndex - fromIndex)
}