package io.github.mmarco94.tambourine.utils

import io.github.oshai.kotlinlogging.KLogger
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