package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.ParseException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

sealed interface Lyrics {
    val rawString: String

    data class Plain(val lyrics: String) : Lyrics {
        override val rawString: String get() = lyrics

        override fun toString(): String {
            return lyrics
        }
    }

    data class Synchronized(val lines: List<Line>) : Lyrics {
        override val rawString: String get() = toString()
        override fun toString(): String {
            return lines.joinToString(separator = "\n")
        }

        data class Line(
            val start: Duration,
            val content: String,
        ) {
            override fun toString(): String {
                val mm = start.inWholeMinutes.toString().padStart(2, '0')
                val ss = (start.inWholeSeconds - start.inWholeMinutes * 60).toString().padStart(2, '0')
                val xx = ((start.inWholeMilliseconds - start.inWholeSeconds * 1000) / 10).toString().padStart(2, '0')
                return "[$mm:$ss:$xx]$content"
            }
        }
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
            return Plain(trimmed)
        }
    }
}

data class LrcFile(
    val rawString: String,
    val entries: List<Entry>,
) {
    data class Entry(
        val tagKey: String,
        val tagValue: String,
        val content: String?,
    ) {
        fun toSynchronizedLine(offsetMs: Int): Lyrics.Synchronized.Line? {
            val mm = if (tagKey.isEmpty()) 0 else tagKey.toPositiveIntOrMinusOne()
            return if (mm < 0 || content == null) {
                null
            } else {
                val ios = tagValue.indexOf('.')
                val startMs = if (ios >= 0) {
                    val ss = tagValue.toPositiveIntOrMinusOne(end = ios).takeIfPositive { return null }
                    val xx = tagValue.toPositiveIntOrMinusOne(start = ios + 1).takeIfPositive { 0 }
                    mm * 60 * 1000 + ss * 1000 + (xx * 10) - offsetMs
                } else {
                    val ss = tagValue.toPositiveIntOrMinusOne().takeIfPositive { return null }
                    mm * 60 * 1000 + ss * 1000 - offsetMs
                }
                Lyrics.Synchronized.Line(
                    startMs.milliseconds,
                    content,
                )
            }
        }
    }

    fun toSynchronized(): Lyrics.Synchronized {
        val offsetMs = entries
            .filter { it.tagKey.equals("offset", ignoreCase = true) }
            .mapNotNull { it.tagValue.toIntOrNull() }
            .sum()
        val lines = entries.mapNotNull { it.toSynchronizedLine(offsetMs) }
        if (lines.isEmpty()) {
            throw ParseException("Empty lines in Lrc", 0)
        }
        return Lyrics.Synchronized(lines)
    }

    companion object {

        private const val BOM = '\uFEFF'

        private fun parseEntry(lyrics: String, start: Int, end: Int): Entry? {
            var tagStart = start
            while (tagStart < end && (lyrics[tagStart].isWhitespace() || lyrics[tagStart] == BOM)) {
                tagStart++
            }
            if (tagStart == end) return null

            if (lyrics[tagStart] != '[') {
                throw ParseException("Line '${lyrics.subSequence(start, end)}' doesn't match", 0)
            }

            val colon = lyrics.indexOf(':', tagStart + 1)
            val tagEnd = lyrics.indexOf(']', colon + 1)

            if (colon < 0 || tagEnd < 0) {
                throw ParseException("Line '${lyrics.subSequence(start, end)}' doesn't match", 0)
            }

            return Entry(
                lyrics.substringTrimmed(start + 1, colon),
                lyrics.substringTrimmed(colon + 1, tagEnd),
                lyrics.substringTrimmed(tagEnd + 1, end).ifEmpty { null },
            )
        }

        fun of(lyrics: String): LrcFile {
            val entries = ArrayList<Entry>(lyrics.countAllLines())
            lyrics.forAllLines { start, end ->
                parseEntry(lyrics, start, end)?.let { entries += it }
            }
            if (entries.isEmpty()) {
                throw ParseException("Empty Lrc", 0)
            }
            return LrcFile(lyrics, entries)
        }
    }
}
