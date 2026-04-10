package io.github.mmarco94.tambourine.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

@PublishedApi
internal val logger = KotlinLogging.logger {}

inline fun <T> KLogger.debugElapsed(tag: String, f: () -> T): T {
    val ret: T
    val took = measureTimeMillis {
        ret = f()
    }
    debug { "$tag took ${took.milliseconds}" }
    return ret
}

@PublishedApi
internal class RecompositionCounter(
    var value: Int = 0,
    var time: Long = System.currentTimeMillis() / 1000,
)

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun printRecompositionsCount(debugLabel: String, reducePrints: Boolean = false) {
    val count = remember { RecompositionCounter() }
    val now = System.currentTimeMillis() / 1000
    if (now != count.time) {
        if (reducePrints) {
            logger.debug { "Recomposition of $debugLabel last second: ${count.value}" }
        }
        count.time = now
        count.value = 0
    }
    count.value++
    if (!reducePrints) {
        logger.debug { "Recomposition of $debugLabel this second: ${count.value}" }
    }
}
