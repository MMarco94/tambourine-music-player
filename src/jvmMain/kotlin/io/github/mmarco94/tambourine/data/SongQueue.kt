package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.data.RepeatMode.*

enum class RepeatMode {
    DO_NOT_REPEAT,
    REPEAT_QUEUE,
    REPEAT_SONG,
}

data class SongQueue(
    val originalSongs: List<Song>,
    val songs: List<Song>,
    val position: Int,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = REPEAT_QUEUE,
) {
    val currentSong get() = songs[position]

    init {
        require(position in songs.indices)
        require(songs.isNotEmpty())
    }

    val remainingSongs = songs.subList(position, songs.size)

    fun previous(): SongQueue {
        return copy(position = (position - 1).mod(songs.size))
    }

    fun next(): SongQueue {
        return copy(position = (position + 1).mod(songs.size))
    }

    /**
     * The next queue, plus whether it can continue playing, or it should stop
     */
    fun nextInQueue(): Pair<SongQueue, Boolean> {
        return when (repeatMode) {
            DO_NOT_REPEAT -> copy(position = (position + 1).mod(songs.size)) to (position < songs.size - 1)
            REPEAT_QUEUE -> copy(position = (position + 1).mod(songs.size)) to true
            REPEAT_SONG -> this to true
        }
    }

    fun toggleRepeat(): SongQueue {
        return copy(
            repeatMode = when (repeatMode) {
                DO_NOT_REPEAT -> REPEAT_QUEUE
                REPEAT_QUEUE -> REPEAT_SONG
                REPEAT_SONG -> DO_NOT_REPEAT
            }
        )
    }

    fun toggleShuffle(): SongQueue {
        return if (isShuffled) unshuffled() else shuffled()
    }

    fun shuffled(): SongQueue {
        return copy(
            songs = listOf(currentSong) + (
                    songs.subList(0, position) +
                            songs.subList(position + 1, songs.size)
                    ).shuffled(),
            position = 0,
            isShuffled = true,
        )
    }

    fun unshuffled(): SongQueue {
        return copy(
            songs = originalSongs,
            position = originalSongs.indexOf(currentSong),
            isShuffled = false,
        )
    }

    fun skipTo(song: Song): SongQueue {
        val iof = songs.indexOf(song)
        require(iof >= 0)
        return copy(position = iof)
    }

    companion object {
        fun of(library: Library, song: Song): SongQueue {
            return SongQueue(
                library.songs,
                library.songs,
                library.songs.indexOf(song)
            )
        }

        fun of(songs: List<Song>, song: Song): SongQueue {
            return SongQueue(
                songs,
                songs,
                songs.indexOf(song)
            )
        }
    }
}