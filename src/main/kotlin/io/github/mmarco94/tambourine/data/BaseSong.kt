package io.github.mmarco94.tambourine.data

import kotlin.time.Duration

interface BaseSong {
    val length: Duration
    val year: Int?
    val track: Int?
}