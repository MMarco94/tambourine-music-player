package io.github.musicplayer.utils

fun String.trimToNull(): String? {
    val s = this.trim()
    return s.ifEmpty { null }
}