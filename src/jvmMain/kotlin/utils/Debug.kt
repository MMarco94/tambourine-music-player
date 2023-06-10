package utils

import mu.KLogger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds


inline fun <T> KLogger.debugElapsed(tag: String, f: () -> T): T {
    val ret: T
    val took = measureTimeMillis {
        ret = f()
    }
    debug { "$tag took ${took.milliseconds}" }
    return ret
}