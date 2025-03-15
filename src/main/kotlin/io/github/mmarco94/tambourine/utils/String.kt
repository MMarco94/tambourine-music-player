package io.github.mmarco94.tambourine.utils

fun String.trimToNull(): String? {
    val s = this.trim()
    return s.ifEmpty { null }
}