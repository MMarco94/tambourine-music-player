package io.github.mmarco94.tambourine.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform
import kotlin.time.Duration

/**
 * This emits value immediately but throttles if upstream is emitting them too fast
 */
fun <T> Flow<T>.throttle(throttle: Duration): Flow<T> {
    return conflate()
        .transform {
            emit(it)
            delay(throttle)
        }
}