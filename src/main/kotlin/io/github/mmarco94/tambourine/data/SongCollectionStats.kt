package io.github.mmarco94.tambourine.data

import kotlin.time.Duration

data class SongCollectionStats(
    val songsCount: Int,
    val totalLength: Duration,
    val dateRange: PartialDateRange?,
    val maxTrackNumber: Int?,
) {
    companion object {
        fun of(songs: Collection<BaseSong>): SongCollectionStats {
            return SongCollectionStats(
                songsCount = songs.size,
                totalLength = songs.fold(Duration.ZERO) { a, b -> a + b.length },
                dateRange = PartialDateRange.of(songs) { it.date },
                maxTrackNumber = songs.mapNotNull { it.track }.maxOrNull(),
            )
        }
    }
}