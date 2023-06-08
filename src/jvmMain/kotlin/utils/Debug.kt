package utils

import mu.KotlinLogging
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds


@PublishedApi
internal val logger = KotlinLogging.logger {}

inline fun <T> debugElapsed(tag: String, f: () -> T): T {
    val ret: T
    val took = measureTimeMillis {
        ret = f()
    }
    logger.debug { "$tag took ${took.milliseconds}" }
    return ret
}