package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.debugElapsed
import io.github.mmarco94.tambourine.utils.mostCommonOrNull
import io.github.mmarco94.tambourine.utils.orNoop
import io.github.mmarco94.tambourine.utils.readFolder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

data class Artist(
    val name: String,
    val stats: SongCollectionStats,
)

data class Album(
    val title: String,
    val artist: Artist,
    val cover: AlbumCover?,
    val stats: SongCollectionStats,
)

data class Song(
    val file: File,
    override val track: Int?,
    val title: String,
    val album: Album,
    val cover: AlbumCover?,
    override val length: Duration,
    override val year: Int?,
) : BaseSong {

    val artist get() = album.artist

    private fun matches(queryFilter: String): Boolean {
        return title.contains(queryFilter, ignoreCase = true) ||
                this.album.title.contains(queryFilter, ignoreCase = true) ||
                this.artist.name.contains(queryFilter, ignoreCase = true)
    }

    fun matches(artist: Artist?, album: Album?, queryFilter: List<String>): Boolean {
        return (artist == null || this.artist == artist) &&
                (album == null || this.album == album) &&
                (queryFilter.all { matches(it) })
    }

    fun audioStream(): AudioInputStream {
        val audioStream: AudioInputStream = AudioSystem.getAudioInputStream(file)
        val format: AudioFormat = audioStream.format
        val pcmFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            format.sampleRate,
            16,
            format.channels,
            2 * format.channels,
            format.sampleRate,
            format.isBigEndian,
        )
        return AudioSystem.getAudioInputStream(pcmFormat, audioStream)
    }
}

sealed interface LibraryState

data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val songsByAlbum: Map<Album, List<Song>> = songs.groupBy { it.album },
    val songsByArtist: Map<Artist, List<Song>> = songs.groupBy { it.artist },
) : LibraryState {

    val stats = SongCollectionStats.of(songs)

    fun filter(artist: Artist?, album: Album?, query: String): Library {
        val queryFilter = query.split(queryStringDelimiters)
        val songs = songs.filter { it.matches(artist, album, queryFilter) }
        val songsByAlbum = songs.groupBy { it.album }
        val songsByArtist = songs.groupBy { it.artist }
        return Library(
            songs = songs,
            albums = albums.filter { it in songsByAlbum },
            artists = artists.filter { it in songsByArtist },
            songsByAlbum = songsByAlbum,
            songsByArtist = songsByArtist,
        )
    }

    fun sort(
        artist: Comparator<Artist>?,
        album: Comparator<Album>?,
        song: Comparator<Song>?,
    ): Library {
        val newArtists = artists.sortedWith(artist.orNoop())
        val artistIndices = newArtists.withIndex().associate { it.value to it.index }

        val newAlbums = albums.sortedWith(
            compareBy<Album> { if (artist != null) artistIndices.getValue(it.artist) else 0 }
                .then(album.orNoop())
        )
        val albumIndices = newAlbums.withIndex().associate { it.value to it.index }

        val newSongs = songs.sortedWith(
            compareBy<Song> { if (artist != null) artistIndices.getValue(it.artist) else 0 }
                .thenBy { if (album != null) albumIndices.getValue(it.album) else 0 }
                .then(song.orNoop())
        )

        return Library(
            songs = newSongs,
            albums = newAlbums,
            artists = newArtists,
        )
    }

    companion object {
        private val queryStringDelimiters = "\\s+".toRegex()

        private fun buildArtists(metadata: Collection<RawMetadataSong>): Map<String, Artist> {
            return metadata
                .groupBy { it.nnAlbumArtist }
                .mapValues { (artist, songs) ->
                    Artist(artist, SongCollectionStats.of(songs))
                }
        }

        private suspend fun buildAlbums(
            metadata: Collection<RawMetadataSong>,
            artists: Map<String, Artist>,
        ): Map<Pair<Artist, String>, Album> {
            return metadata
                .groupBy { artists.getValue(it.nnAlbumArtist) to it.nnAlbum }
                .mapValues { (album, songs) ->
                    val cover = songs.map { it.cover }.awaitAll().filterNotNull().mostCommonOrNull()
                    Album(album.second, album.first, cover, SongCollectionStats.of(songs))
                }
        }

        private suspend fun from(metadata: Collection<RawMetadataSong>): Library {
            val artists = buildArtists(metadata)
            val albums = buildAlbums(metadata, artists)

            val songs = metadata.map { song ->
                val albumArtist = artists.getValue(song.nnAlbumArtist)
                val album = albums.getValue(albumArtist to song.nnAlbum)
                Song(
                    file = song.file,
                    track = song.track,
                    title = song.nnTitle,
                    album = album,
                    cover = song.cover.await(),
                    length = song.length,
                    year = song.year,
                )
            }
            return Library(
                songs,
                albums.values.toList(),
                artists.values.toList(),
            )
        }

        data class LoadingProgress(val loaded: Int) : LibraryState

        suspend fun fromFolder(
            folder: File,
            progress: Channel<LibraryState>,
        ): Library = coroutineScope {
            val decoder = CoversDecoder(this + Dispatchers.Default)

            val songs = logger.debugElapsed("Loading songs") {
                val songsChannel = Channel<RawMetadataSong>(Channel.UNLIMITED)
                val loaded = AtomicInteger(0)
                launch {
                    coroutineScope {
                        readFolder(folder) { file ->
                            launch(Dispatchers.IO) {
                                try {
                                    val song = RawMetadataSong.fromMusicFile(file, decoder)
                                    songsChannel.send(song)
                                    progress.send(LoadingProgress(loaded.incrementAndGet()))
                                } catch (e: Exception) {
                                    logger.error("Error while parsing music file: ${e.message}")
                                }
                            }
                        }
                    }
                    songsChannel.close()
                }
                songsChannel.toList()
            }

            val library = logger.debugElapsed("Creating library") {
                from(songs)
            }
            progress.send(library)
            progress.close()
            library
        }
    }
}