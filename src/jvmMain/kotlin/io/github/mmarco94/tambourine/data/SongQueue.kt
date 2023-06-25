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

    fun move(oldIndex: Int, newIndex: Int): SongQueue {
        val newSongs = songs.toMutableList().apply {
            val s = removeAt(oldIndex)
            add(if (newIndex <= oldIndex) newIndex else newIndex - 1, s)
        }
        val newPos = when {
            position == oldIndex -> newIndex
            position == newIndex -> oldIndex
            oldIndex < position && newIndex > position -> position - 1
            oldIndex > position && newIndex < position -> position + 1
            else -> position
        }
        return copy(
            position = newPos,
            songs = newSongs,
        )
    }

    fun add(index: Int, song: Song): SongQueue {
        return copy(
            position = if (index <= position) position + 1 else position,
            songs = songs.toMutableList().apply {
                add(index, song)
            },
        )
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

    fun maybeShuffled(shuffled: Boolean?): SongQueue {
        return when (shuffled) {
            true -> shuffled()
            false -> unshuffled()
            null -> this
        }
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

    companion object {

        fun of(currentQueue: SongQueue?, songs: List<Song>, song: Song): SongQueue {
            return if (currentQueue != null && currentQueue.originalSongs.toSet() == songs.toSet()) {
                currentQueue.copy(
                    originalSongs = songs,
                    position = if (song == currentQueue.currentSong) {
                        currentQueue.position
                    } else {
                        currentQueue.songs.indexOf(song)
                    },
                )
            } else {
                SongQueue(
                    songs,
                    songs,
                    songs.indexOf(song)
                )
            }
        }
    }
}