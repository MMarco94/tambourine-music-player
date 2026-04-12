package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.mostCommonOrNull
import io.github.mmarco94.tambourine.utils.orNoop
import io.github.mmarco94.tambourine.utils.pathSimilarity
import io.github.mmarco94.tambourine.utils.withoutExtension
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import net.bjoernpetersen.m3u.model.M3uEntry
import net.bjoernpetersen.m3u.model.MediaPath
import net.bjoernpetersen.m3u.model.MediaUrl
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

private val IMAGES_HIGH_PRIORITY = listOf("cover", "folder", "album", "front")
private val IMAGES_LOW_PRIORITY = listOf("back", "disc", "artist")

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

data class Playlist(
    val name: String,
    val file: Path,
    val songs: List<Song>,
) {
    val songSet = songs.toSet()
}

data class Song(
    val file: Path,
    override val disk: Int?,
    override val track: Int?,
    val title: String,
    val album: Album,
    val cover: AlbumCover?,
    val lyrics: Lyrics?,
    override val length: Duration,
    override val year: Int?,
) : BaseSong {

    val artist get() = album.artist

    private val hashCode = super.hashCode()
    override fun hashCode() = hashCode
    override fun equals(other: Any?) = super.equals(other)

    private fun matches(queryFilter: String): Boolean {
        return title.contains(queryFilter, ignoreCase = true) ||
                this.album.title.contains(queryFilter, ignoreCase = true) ||
                this.artist.name.contains(queryFilter, ignoreCase = true)
    }

    fun matches(artist: Artist?, album: Album?, playlist: Playlist?, queryFilter: List<String>): Boolean {
        return (artist == null || this.artist == artist) &&
                (album == null || this.album == album) &&
                (playlist == null || this in playlist.songSet) &&
                (queryFilter.all { matches(it) })
    }

    fun audioStream(): AudioInputStream {
        val audioStream: AudioInputStream = AudioSystem.getAudioInputStream(file.toFile())
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

data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<Playlist>,
    val songsByAlbum: Map<Album, List<Song>> = songs.groupBy { it.album },
    val songsByArtist: Map<Artist, List<Song>> = songs.groupBy { it.artist },
) {
    val stats = SongCollectionStats.of(songs)

    fun filter(artist: Artist?, album: Album?, playlist: Path?, query: String): Library {
        val playlist = playlists.singleOrNull { it.file == playlist }
        val queryFilter = query.split(queryStringDelimiters)
        val songs = songs.filter { it.matches(artist, album, playlist, queryFilter) }
        val songsByAlbum = songs.groupBy { it.album }
        val songsByArtist = songs.groupBy { it.artist }
        return Library(
            songs = songs,
            albums = albums.filter { it in songsByAlbum },
            artists = artists.filter { it in songsByArtist },
            // Note: I'm not filtering playlists so their elements remain consistent even when filtered.
            // As of 2026-04-09, this has no real implication and could be changed if necessary
            playlists = playlists,
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
            playlists = playlists,
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

        private fun buildAlbums(
            metadata: Collection<RawMetadataSong>,
            artists: Map<String, Artist>,
            coverForSong: Map<Path, AlbumCover?>,
        ): Map<Pair<Artist, String>, Album> {
            return metadata
                .groupBy { artists.getValue(it.nnAlbumArtist) to it.nnAlbum }
                .mapValues { (album, songs) ->
                    val cover = songs.mapNotNull { coverForSong.getValue(it.file) }.mostCommonOrNull()
                    Album(album.second, album.first, cover, SongCollectionStats.of(songs))
                }
        }

        suspend fun from(
            metadata: Map<Path, RawMetadataSong>,
            rawPlaylists: Map<Path, List<M3uEntry>>,
            images: Map<Path, Deferred<AlbumCover?>>,
            lyrics: Map<Path, Lyrics>,
        ): Library {
            val songSet = metadata.values
            val imagesByPath = images.entries.groupBy { it.key.parent }
            val coverForSong = metadata.mapValues { (_, song) ->
                findCover(song, imagesByPath)
            }

            val lyricsByPath = lyrics.entries.groupBy { it.key.withoutExtension.lowercase() }

            val artists = buildArtists(songSet)
            val albums = buildAlbums(songSet, artists, coverForSong)


            val songs = songSet.map { song ->
                val albumArtist = artists.getValue(song.nnAlbumArtist)
                val album = albums.getValue(albumArtist to song.nnAlbum)
                val lyrics = song.lyrics
                    ?: lyricsByPath[song.file.withoutExtension.lowercase()]?.firstOrNull()?.value
                Song(
                    file = song.file,
                    disk = song.disk,
                    track = song.track,
                    title = song.nnTitle,
                    album = album,
                    cover = coverForSong.getValue(song.file),
                    length = song.length,
                    year = song.year,
                    lyrics = lyrics,
                )
            }
            val playlists = if (rawPlaylists.isNotEmpty()) {
                val songsByName = songs.groupBy { it.file.name }
                rawPlaylists.map { (file, entries) ->
                    Playlist(
                        name = file.nameWithoutExtension,
                        file = file,
                        songs = entries.mapNotNull { entry ->
                            findBestMatch(entry, songsByName)
                        },
                    )
                }.sortedBy { it.name }
            } else emptyList()
            return Library(
                songs = songs,
                albums = albums.values.toList(),
                artists = artists.values.toList(),
                playlists = playlists,
            )
        }

        fun findBestMatch(
            entry: M3uEntry,
            songsByName: Map<String, List<Song>>,
        ): Song? {
            val location = when (val loc = entry.location) {
                is MediaPath -> loc.path
                is MediaUrl -> {
                    logger.debug { "Playlist entry ${entry.location} ignored: only local files are supported, URL given" }
                    return null
                }
            }
            val candidates = songsByName[location.name]
            return if (candidates == null) {
                logger.debug { "Playlist entry ${entry.location} ignored: no songs found that match the name '${location.name}'" }
                null
            } else if (candidates.size == 1) {
                candidates.single()
            } else {
                logger.debug { "Multiple possible matches for playlist entry ${entry.location}: finding best possible song based on path similarity" }
                candidates.maxBy { song ->
                    pathSimilarity(location, song.file)
                }
            }
        }

        suspend fun findCover(
            song: RawMetadataSong,
            imagesByPath: Map<Path, List<Map.Entry<Path, Deferred<AlbumCover?>>>>
        ): AlbumCover? {
            val songCover = song.cover.await()
            if (songCover != null) return songCover

            val candidateImages = imagesByPath[song.file.parent] ?: return null
            val awaited = candidateImages.mapNotNull { (path, cover) ->
                cover.await()?.let { path to it }
            }
            return awaited.maxByOrNull { (path, _) ->
                val highPriorityIdx = IMAGES_HIGH_PRIORITY.indexOf(path.nameWithoutExtension.lowercase())
                if (highPriorityIdx >= 0) {
                    return@maxByOrNull IMAGES_HIGH_PRIORITY.size - highPriorityIdx
                }
                val lowPriorityIdx = IMAGES_LOW_PRIORITY.indexOf(path.nameWithoutExtension.lowercase())
                if (lowPriorityIdx >= 0) {
                    return@maxByOrNull -lowPriorityIdx
                }
                0
            }?.second
        }
    }
}