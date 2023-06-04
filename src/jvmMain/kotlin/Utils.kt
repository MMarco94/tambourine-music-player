import androidx.compose.material.Colors
import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import kotlin.math.abs
import kotlin.math.log10
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T> noopComparator(): Comparator<T> = compareBy { 0 }
fun <T> Comparator<T>?.orNoop(): Comparator<T> = this ?: noopComparator()

val Colors.onSurfaceSecondary get() = onSurface.copy(alpha = 0.5f)

fun <T> Collection<T>.rangeOfOrNull(f: (T) -> Int?): IntRange? {
    var min: Int? = null
    var max: Int? = null
    forEach {
        val new = f(it)
        if (new != null) {
            if (min == null || new < min!!) {
                min = new
            }
            if (max == null || new > max!!) {
                max = new
            }
        }
    }
    return if (min != null) (min!!..max!!)
    else null
}

fun Int.digits() = when (this) {
    0 -> 1
    else -> log10(abs(toDouble())).toInt() + 1
}

fun Duration.rounded(): Duration {
    return this.inWholeSeconds.seconds
}

fun ID3v2.recordingYear(): String? {
    return if (this is ID3v24Tag) {
        year ?: recordingTime
    } else {
        year
    }
}

fun <T> Collection<T>.mostCommonOrNull(): T? {
    val numbersByElement = groupingBy { it }.eachCount()
    return numbersByElement.maxByOrNull { it.value }?.key
}