package data

import kotlin.time.Duration

interface BaseSong {
    val length: Duration
    val year: Int?
}