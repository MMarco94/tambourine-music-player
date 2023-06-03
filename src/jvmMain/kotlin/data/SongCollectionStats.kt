package data

import kotlin.time.Duration

data class SongCollectionStats(
    val songsCount: Int,
    val totalLength: Duration,
) {
    companion object {
        fun of(songs: Collection<BaseSong>): SongCollectionStats {
            return SongCollectionStats(
                songsCount = songs.size,
                totalLength = songs.fold(Duration.ZERO) { a, b -> a + b.length },
            )
        }
    }
}