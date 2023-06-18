package io.github.mmarco94.tambourine.utils

import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit


fun Duration.format(): String {
    if (isNegative()) return "-" + (-this).format()
    else if (isInfinite()) return "Inf"

    val rounded = (this.inWholeMilliseconds / 100f).roundToLong()
    val min = rounded / 600
    val sec = rounded.mod(600) / 10
    val decimal = rounded.mod(10)

    return if (min == 0L && sec.absoluteValue < 60) {
        "$sec.${decimal}s"
    } else if (min < 60) {
        "${min}m ${sec}s"
    } else {
        val h = min / 60
        "${h}h ${min.mod(60)}m ${sec}s"
    }
}

inline fun <T> Iterable<T>.sumOfDuration(selector: (T) -> Duration): Duration {
    var sum: Duration = Duration.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Duration.toFloat(unit: DurationUnit) = toDouble(unit).toFloat()
