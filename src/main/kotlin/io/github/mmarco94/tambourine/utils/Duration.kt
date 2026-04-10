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

fun Duration.decimalsRounded(): Long {
    val ms = inWholeMilliseconds
    return if (ms == Long.MAX_VALUE || ms == Long.MIN_VALUE) {
        ms
    } else {
        (ms / 100f).roundToLong()
    }
}

@Composable
fun Duration.format(): String {
    return formatDuration(decimalsRounded())
}

@Composable
fun formatDuration(decimals: Long): String {
    if (decimals < 0) return stringResource(Res.string.duration_negative, formatDuration(-decimals))
    else if (decimals == Long.MAX_VALUE) return stringResource(Res.string.duration_infinite)

    val min = decimals / 600
    val sec = decimals.mod(600) / 10

    return if (min == 0L && sec.absoluteValue < 60) {
        val secondsStr = secondsWithDecimalsFormatter.format(decimals / 10f)
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
