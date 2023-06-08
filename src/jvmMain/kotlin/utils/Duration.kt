package utils

import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


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