package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.trimToNull
import mu.KotlinLogging
import java.text.ParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

sealed interface Lyrics {
    val rawString: String

    data class Plain(override val rawString: String, val lyrics: String) : Lyrics {
        override fun toString(): String {
            return lyrics
        }
    }

    data class Synchronized(override val rawString: String, val lines: List<Line>) : Lyrics {
        override fun toString(): String {
            return lines.joinToString(separator = "\n") {
                "${it.start}: ${it.content}"
            }
        }

        data class Line(
            val start: Duration,
            val content: String,
        )
    }

    companion object {
        fun of(lyrics: String?): Lyrics? {
            if (lyrics == null) return null

            try {
                return LrcFile.of(lyrics).toSynchronized()
            } catch (e: ParseException) {
                logger.trace { "Cannot parse Lrc: ${e.message}" }
            }

            val trimmed = lyrics.trimToNull() ?: return null
            return Plain(lyrics, trimmed)
        }
    }
}

private data class LrcFile(
    val rawString: String,
    val entries: List<Entry>,
) {
    data class Entry(
        val tagKey: String,
        val tagValue: String,
        val content: String?,
    )

    fun toSynchronized(): Lyrics.Synchronized {
        val offset = entries
            .filter { it.tagKey.equals("offset", ignoreCase = true) }
            .mapNotNull { it.tagValue.toIntOrNull() }
            .sum()
            .milliseconds
        val lines = entries
            .mapNotNull {
                val mm = it.tagKey.toIntOrNull()
                val ssXX = ssXXRegex.matchEntire(it.tagValue)?.groupValues
                if (mm == null || ssXX == null || it.content == null) {
                    null
                } else {
                    val start = mm.minutes + ssXX[1].toInt().seconds + (ssXX[2].toInt() * 10).milliseconds - offset
                    Lyrics.Synchronized.Line(
                        start,
                        it.content,
                    )
                }
            }
        if (lines.isEmpty()) {
            throw ParseException("Empty lines in Lrc", 0)
        }
        return Lyrics.Synchronized(rawString, lines)
    }

    companion object {

        private val ssXXRegex = "(\\d+)\\.(\\d+)".toRegex()
        private val entryRegex = "[\\s\uFEFF]*\\[([^:]+):([^]]+)](.*)".toRegex()
        private fun parseEntry(line: String): Entry {
            val match = entryRegex.matchEntire(line) ?: throw ParseException("Line '$line' doesn't match", 0)
            return Entry(
                match.groupValues[1].trim(),
                match.groupValues[2].trim(),
                match.groupValues[3].trimToNull(),
            )
        }

        fun of(lyrics: String): LrcFile {
            val entries = lyrics
                .lines()
                .mapNotNull { it.trimToNull() }
                .map { parseEntry(it) }
            if (entries.isEmpty()) {
                throw ParseException("Empty Lrc", 0)
            }
            return LrcFile(lyrics, entries)
        }
    }
}
