package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.rangeOfOrNull
import kotlin.time.Duration

data class SongCollectionStats(
    val songsCount: Int,
    val totalLength: Duration,
    val year: IntRange?,
    val maxTrackNumber: Int?,
) {
    companion object {
        fun of(songs: Collection<BaseSong>): SongCollectionStats {
            return SongCollectionStats(
                songsCount = songs.size,
                totalLength = songs.fold(Duration.ZERO) { a, b -> a + b.length },
                year = songs.rangeOfOrNull { it.year },
                maxTrackNumber = songs.mapNotNull { it.track }.maxOrNull(),
            )
        }
    }
}