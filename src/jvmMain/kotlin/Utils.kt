import androidx.compose.material.Colors
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

fun Duration.rounded(): Duration {
    return this.inWholeSeconds.seconds
}