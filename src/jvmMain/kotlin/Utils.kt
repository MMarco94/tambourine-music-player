import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioFormat
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

fun <T> noopComparator(): Comparator<T> = compareBy { 0 }
fun <T> Comparator<T>?.orNoop(): Comparator<T> = this ?: noopComparator()

inline fun <T> debugElapsed(tag: String, f: () -> T): T {
    val ret: T
    val took = measureTimeMillis {
        ret = f()
    }
    println("$tag took ${took.milliseconds}")
    return ret
}

fun AudioFormat.framesToDuration(frames: Long) = (frames / frameRate * 1000000000L).roundToLong().nanoseconds
fun AudioFormat.durationToFrames(duration: Duration) = ((duration / 1.seconds) * frameRate).roundToLong()

fun Long.chunked(chunk: Int, f: (chunk: Int) -> Unit) {
    require(this >= 0)
    require(chunk >= 0)
    var done = 0L
    while (done < this) {
        val c = minOf(chunk.toLong(), this - done).toInt()
        f(c)
        done += c
    }
}

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
    val sec = this.inWholeSeconds
    return if (sec.absoluteValue < 1) {
        this.inWholeMilliseconds.milliseconds
    } else if (sec.absoluteValue < 10) {
        (this.inWholeMilliseconds * 10 / 1000 * 100).milliseconds
    } else if (sec.absoluteValue < 3600) {
        sec.seconds
    } else {
        (sec / 60 * 60).seconds
    }
}

fun <T> Collection<T>.mostCommonOrNull(): T? {
    val numbersByElement = groupingBy { it }.eachCount()
    return numbersByElement.maxByOrNull { it.value }?.key
}

@Composable
fun animateOrSnapFloatAsState(
    targetValue: Pair<Boolean, Float>,
    visibilityThreshold: Float = 0.01f,
    label: String = "FloatAnimation",
): State<Float> {
    val resolvedAnimSpec = remember(visibilityThreshold) { spring(visibilityThreshold = visibilityThreshold) }
    return animateOrSnapValueAsState(
        targetValue,
        Float.VectorConverter,
        resolvedAnimSpec,
        visibilityThreshold,
        label,
    )
}

@Composable
fun <T, V : AnimationVector> animateOrSnapValueAsState(
    targetValue: Pair<Boolean, T>,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = remember { spring() },
    visibilityThreshold: T? = null,
    label: String = "ValueAnimation",
): State<T> {
    //See animateValueAsState
    val toolingOverride = remember { mutableStateOf<State<T>?>(null) }
    val animatable = remember { Animatable(targetValue.second, typeConverter, visibilityThreshold, label) }
    val animSpec: AnimationSpec<T> by rememberUpdatedState(
        animationSpec.run {
            if (visibilityThreshold != null && this is SpringSpec &&
                this.visibilityThreshold != visibilityThreshold
            ) {
                spring(dampingRatio, stiffness, visibilityThreshold)
            } else {
                this
            }
        }
    )
    val channel = remember { Channel<Pair<Boolean, T>>(Channel.CONFLATED) }
    SideEffect {
        channel.trySend(targetValue)
    }
    LaunchedEffect(channel) {
        for (target in channel) {
            // This additional poll is needed because when the channel suspends on receive and
            // two values are produced before consumers' dispatcher resumes, only the first value
            // will be received.
            // It may not be an issue elsewhere, but in animation we want to avoid being one
            // frame late.
            val (snap, newTarget) = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    if (snap) {
                        animatable.snapTo(newTarget)
                    } else {
                        animatable.animateTo(newTarget, animSpec)
                    }
                }
            }
        }
    }
    return toolingOverride.value ?: animatable.asState()
}
