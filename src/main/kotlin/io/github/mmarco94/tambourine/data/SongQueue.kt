package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.data.RepeatMode.*

enum class RepeatMode {
    DO_NOT_REPEAT,
    REPEAT_QUEUE,
    REPEAT_SONG,
    ;

    val next
        get() = when (this) {
            DO_NOT_REPEAT -> REPEAT_QUEUE
            REPEAT_QUEUE -> REPEAT_SONG
            REPEAT_SONG -> DO_NOT_REPEAT
        }

    companion object {
        val DEFAULT = REPEAT_QUEUE
    }
}

data class SongQueue(
    val originalSongsKeys: List<SongKey>,
    val songs: List<SongKey>,
    val position: Int,
    val songsByKey: Map<SongKey, Song>,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.DEFAULT,
) {
    val currentSong get() = songsByKey.getValue(songs[position])
    val currentSongKey get() = songs[position]

    init {
        require(position in songs.indices)
        require(songs.isNotEmpty())
        require(songs.size == originalSongsKeys.size)
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
            originalSongsKeys = originalSongsKeys + song.uniqueKey,
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
        return setRepeatMode(repeatMode.next)
    }

    fun setRepeatMode(repeatMode: RepeatMode): SongQueue {
        return copy(repeatMode = repeatMode)
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
            songs = listOf(currentSongKey) + (
                    songs.subList(0, position) +
                            songs.subList(position + 1, songs.size)
                    ).shuffled(),
            songsByKey = songsByKey,
            position = 0,
            isShuffled = true,
        )
    }

    fun unshuffled(): SongQueue {
        if (!this.isShuffled) return this
        return copy(
            songs = originalSongsKeys,
            position = originalSongsKeys.indexOf(currentSongKey),
            isShuffled = false,
        )
    }

    fun updateLibrary(library: Library): SongQueue {
        // Note: by design, this doesn't remove songs that are no longer in the library
        val newMap = songsByKey.mapValues {
            library.songsByKey[it.key] ?: it.value
        }
        return copy(songsByKey = newMap)
    }
}

fun SongQueue?.addIfMissing(song: Song): SongQueue {
    return when {
        this == null -> SongQueue(
            originalSongsKeys = listOf(song.uniqueKey),
            songs = listOf(song.uniqueKey),
            songsByKey = mapOf(song.uniqueKey to song),
            position = 0,
        )

        song.uniqueKey in originalSongsKeys -> this
        else -> add(originalSongsKeys.size, song)
    }
}

fun SongQueue?.append(song: Song): SongQueue {
    return this?.add(originalSongsKeys.size, song) ?: addIfMissing(song)
}
