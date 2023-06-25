package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.audio.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class SongQueueController(
    val cs: CoroutineScope,
    val defaultQueue: List<Song>,
    val sortedLibrary: Library,
    val player: PlayerController,
    val onAction: () -> Unit = {},
) {
    fun play(song: Song, shuffle: Boolean? = null) {
        val newQueue = SongQueue.of(player.queue, defaultQueue, song).maybeShuffled(shuffle)
        onAction()
        cs.launch {
            player.changeQueue(newQueue, Position.Beginning)
            player.play()
        }
    }

    fun playAlbum(song: Song, shuffle: Boolean? = null) {
        play(sortedLibrary.songsByAlbum[song.album], song, shuffle)
    }

    fun playArtist(song: Song, shuffle: Boolean? = null) {
        play(sortedLibrary.songsByArtist[song.artist], song, shuffle)
    }

    private fun play(songs: List<Song>?, song: Song, shuffle: Boolean? = null) {
        val newQueue = SongQueue.of(player.queue, songs ?: listOf(song), song).maybeShuffled(shuffle)
        onAction()
        cs.launch {
            player.changeQueue(newQueue, Position.Beginning)
            player.play()
        }
    }

    fun playQueued(indexInQueue: Int, shuffle: Boolean? = null) {
        val newQueue = player.queue!!.copy(position = indexInQueue).maybeShuffled(shuffle)
        onAction()
        cs.launch {
            player.changeQueue(newQueue, Position.Beginning)
            player.play()
        }
    }

    fun playNext(song: Song) {
        var newQueue = player.queue
        newQueue = if (newQueue != null) {
            val possible = newQueue.songs.withIndex().indexOfLast { (index, s) ->
                index != newQueue!!.position && s == song
            }
            if (possible >= 0) {
                newQueue.move(possible, newQueue.position + 1)
            } else {
                newQueue.add(newQueue.position + 1, song)
            }
        } else {
            SongQueue.of(null, defaultQueue, song)
        }
        onAction()
        cs.launch {
            player.changeQueue(newQueue)
        }
    }

    fun addToQueue(song: Song) {
        var newQueue = player.queue
        newQueue = if (newQueue != null) {
            newQueue.add(newQueue.songs.size, song)
        } else {
            SongQueue.of(null, defaultQueue, song)
        }
        onAction()
        cs.launch {
            player.changeQueue(newQueue)
        }
    }
}
