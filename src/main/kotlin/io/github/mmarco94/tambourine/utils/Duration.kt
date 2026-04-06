package io.github.mmarco94.tambourine.utils

import androidx.compose.runtime.Composable
import io.github.mmarco94.tambourine.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import java.text.NumberFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val secondsWithDecimalsFormatter = NumberFormat.getNumberInstance().apply {
    this.minimumFractionDigits = 1
    this.maximumFractionDigits = 1
}

@Composable
fun Duration.format(): String {
    if (isNegative()) return stringResource(Res.string.duration_negative, (-this).format())
    else if (isInfinite()) return stringResource(Res.string.duration_infinite)

    val rounded = (this.inWholeMilliseconds / 100f).roundToLong()
    val min = rounded / 600
    val sec = rounded.mod(600) / 10

    return if (min == 0L && sec.absoluteValue < 60) {
        val secondsStr = secondsWithDecimalsFormatter.format(this.inWholeMilliseconds / 1000f)
        stringResource(Res.string.duration_seconds, secondsStr)
    } else if (min < 60) {
        stringResource(Res.string.duration_minutes, min) + " " +
                stringResource(Res.string.duration_seconds, sec)
    } else {
        stringResource(Res.string.duration_hours, min / 60) + " " +
                stringResource(Res.string.duration_minutes, min.mod(60)) + " " +
                stringResource(Res.string.duration_seconds, sec)
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
