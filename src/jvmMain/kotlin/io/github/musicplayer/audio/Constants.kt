package io.github.musicplayer.audio

import io.github.musicplayer.utils.toLogRange

val humanHearingRange = (20..20000)
val humanHearingRangeLog = humanHearingRange.toLogRange()
