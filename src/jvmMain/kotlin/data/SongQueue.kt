package data

import data.RepeatMode.*

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

    fun previous(): SongQueue {
        return copy(position = (position - 1).mod(songs.size))
    }

    fun next(): SongQueue {
        return copy(position = (position + 1).mod(songs.size))
    }

    fun nextInQueue(): SongQueue? {
        return when (repeatMode) {
            DO_NOT_REPEAT -> if (position == songs.size - 1) {
                null
            } else {
                copy(position = position + 1)
            }

            REPEAT_QUEUE -> copy(position = (position + 1).mod(songs.size))
            REPEAT_SONG -> this
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
            songs = listOf(currentSong) + (songs.subList(0, position + 1) + songs.subList(
                position + 1,
                songs.size
            )).shuffled(),
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
    }
}