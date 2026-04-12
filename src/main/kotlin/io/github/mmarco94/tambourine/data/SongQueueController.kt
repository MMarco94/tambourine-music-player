package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.audio.PlayerController
import io.github.mmarco94.tambourine.audio.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class SongQueueController(
    val cs: CoroutineScope,
    val sortedLibrary: Library,
    val defaultSongQueue: List<SongKey>,
    val player: PlayerController,
    val onAction: () -> Unit = {},
) {
    fun play(song: Song, shuffle: Boolean? = null) {
        require(song.uniqueKey in defaultSongQueue)
        onAction()
        cs.launch {
            player.transformQueue { queue ->
                val withSong = if (queue == null || song.uniqueKey !in queue.originalSongsKeys) {
                    SongQueue(
                        originalSongsKeys = defaultSongQueue,
                        songs = defaultSongQueue,
                        songsByKey = sortedLibrary.songsByKey,
                        position = defaultSongQueue.indexOf(song.uniqueKey),
                    )
                } else {
                    queue
                }
                if (withSong.currentSongKey == song.uniqueKey) {
                    withSong
                } else {
                    withSong.copy(position = withSong.songs.indexOf(song.uniqueKey))
                }.maybeShuffled(shuffle) to Position.Beginning
            }
            player.play()
        }
    }

    fun playAlbum(song: Song) {
        playSongs(sortedLibrary.songsByAlbum.getValue(song.album), song)
    }

    fun playArtist(song: Song) {
        playSongs(sortedLibrary.songsByArtist.getValue(song.artist), song)
    }

    fun playSongs(songs: List<Song>, song: Song?, shuffle: Boolean = false) {
        require(song == null || song in songs)
        require(songs.isNotEmpty())
        onAction()
        cs.launch {
            player.transformQueue { queue ->
                val songKeys = songs.map { it.uniqueKey }
                val queue = SongQueue(
                    originalSongsKeys = songs.map { it.uniqueKey },
                    songs = songKeys,
                    songsByKey = songs.associateBy { it.uniqueKey },
                    position = if (song == null) if (shuffle) songs.indices.random() else 0 else songs.indexOf(song),
                    repeatMode = queue?.repeatMode ?: RepeatMode.DEFAULT,
                ).maybeShuffled(shuffle)
                queue to Position.Beginning
            }
            player.play()
        }
    }

    fun playQueued(song: Song, tentativeIndex: Int, shuffle: Boolean? = null) {
        onAction()
        cs.launch {
            player.transformQueue { queue ->
                val withSong = queue.addIfMissing(song)
                if (withSong.songs.elementAtOrNull(tentativeIndex) == song.uniqueKey) {
                    withSong.copy(position = tentativeIndex)
                } else {
                    withSong.copy(position = withSong.songs.indexOf(song.uniqueKey))
                }.maybeShuffled(shuffle) to Position.Beginning
            }
            player.play()
        }
    }

    fun playNext(song: Song) {
        onAction()
        cs.launch {
            player.transformQueue { queue ->
                val withSong = queue.addIfMissing(song)
                val possible = withSong.songs.withIndex().indexOfLast { (index, s) ->
                    index != withSong.position && s == song.uniqueKey
                }
                if (possible >= 0) {
                    withSong.move(possible, withSong.position + 1)
                } else {
                    withSong.add(withSong.position + 1, song)
                } to Position.Current
            }
        }
    }

    fun addToQueue(song: Song) {
        onAction()
        cs.launch {
            player.transformQueue { queue ->
                queue.append(song) to Position.Current
            }
        }
    }
}
