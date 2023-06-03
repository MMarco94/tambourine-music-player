import androidx.compose.material.Colors
import java.util.Comparator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T> noopComparator(): Comparator<T> = compareBy { 0 }
fun <T> Comparator<T>?.orNoop(): Comparator<T> = this  ?: noopComparator()

val Colors.onSurfaceSecondary get() = onSurface.copy(alpha =  0.5f)

fun Duration.rounded():Duration {
    return this.inWholeSeconds.seconds
}