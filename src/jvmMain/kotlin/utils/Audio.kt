package utils

import javax.sound.sampled.AudioFormat
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds


fun AudioFormat.framesToDuration(frames: Long) = (frames / frameRate * 1000000000L).roundToLong().nanoseconds
fun AudioFormat.durationToFrames(duration: Duration) = ((duration / 1.seconds) * frameRate).roundToLong()
