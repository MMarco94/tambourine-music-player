import java.util.Comparator

fun <T> noopComparator(): Comparator<T> = compareBy<T> { 0 }
fun <T> Comparator<T>?.orNoop(): Comparator<T> = this  ?: noopComparator()