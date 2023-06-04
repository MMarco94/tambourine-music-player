package data

import androidx.compose.ui.graphics.ImageBitmap
import javazoom.jl.player.Player
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mostCommonOrNull
import noopComparator
import java.io.File
import kotlin.time.Duration


data class Artist(
    val name: String,
    val stats: SongCollectionStats,
) {
    fun matches(artist: Artist?): Boolean {
        return (artist == null || this == artist)
    }
}

data class Album(
    val title: String,
    val artist: Artist,
    val cover: ImageBitmap?,
    val stats: SongCollectionStats,
) {
    fun title(withArtist: Boolean): String {
        return if (withArtist) "${artist.name} - $title"
        else title
    }

    fun matches(artist: Artist?, album: Album?): Boolean {
        return (artist == null || this.artist == artist) && (album == null || this == album)
    }
}

data class Song(
    val file: File,
    val track: Int? = null,
    val title: String,
    val album: Album,
    val cover: ImageBitmap?,
    override val length: Duration,
    override val year: Int?,
) : BaseSong {

    val artist get() = album.artist

    fun matches(artist: Artist?, album: Album?): Boolean {
        return (artist == null || this.artist == artist) && (album == null || this.album == album)
    }

    fun play() {
        val p = Player(file.inputStream())
        p.play()
    }
}


data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val songsByArtist: Map<Artist, List<Song>> = songs.groupBy { it.artist },
    val songsByAlbum: Map<Album, List<Song>> = songs.groupBy { it.album },
) {

    val statistics = SongCollectionStats.of(songs)

    fun filter(artist: Artist?, album: Album?): Library {
        return Library(
            songs = songs.filter { it.matches(artist, album) },
            albums = albums.filter { it.matches(artist, album) },
            artists = artists.filter { it.matches(artist) },
            songsByArtist = songsByArtist.filterKeys { it.matches(artist) },
            songsByAlbum = songsByAlbum.filterKeys { it.matches(artist, album) },
        )
    }

    fun sort(
        artist: Comparator<Artist>,
        album: Comparator<Album>,
        song: Comparator<Song>,
        songInAlbum: Comparator<Song>,
    ): Library {
        val baseSongComparator = noopComparator<Song>()
            .thenBy(artist) { it.artist }
            .thenBy(album) { it.album }
        val songComparator = baseSongComparator.then(song)
        val songComparatorInAlbum = baseSongComparator.then(songInAlbum)
        return Library(
            songs = songs.sortedWith(songComparator),
            albums = albums.sortedWith(noopComparator<Album>()
                .thenBy(artist) { it.artist }
                .then(album)
            ),
            artists = artists.sortedWith(artist),
            songsByArtist = songsByArtist.mapValues { it.value.sortedWith(songComparator) },
            songsByAlbum = songsByAlbum.mapValues { it.value.sortedWith(songComparatorInAlbum) },
        )
    }

    companion object {

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
            covers: Map<RawImage, ImageBitmap>,
        ): Map<Pair<Artist, String>, Album> {
            return metadata
                .groupBy { artists.getValue(it.nnAlbumArtist) to it.nnAlbum }
                .mapValues { (album, songs) ->
                    val cover = songs.mapNotNull { it.cover }.mostCommonOrNull()
                    Album(album.second, album.first, covers[cover], SongCollectionStats.of(songs))
                }
        }

        suspend fun from(metadata: Collection<RawMetadataSong>): Library = coroutineScope {
            val covers: Map<RawImage, ImageBitmap> = metadata
                .mapNotNull { it.cover }
                .distinct()
                .map { img ->
                    async {
                        img to img.decode()
                    }
                }
                .awaitAll()
                .associate { it }
            val artists = buildArtists(metadata)
            val albums = buildAlbums(metadata, artists, covers)

            val songs = metadata.map { song ->
                val albumArtist = artists.getValue(song.nnAlbumArtist)
                val album = albums.getValue(albumArtist to song.nnAlbum)
                Song(
                    file = song.file,
                    track = song.track,
                    title = song.nnTitle,
                    album = album,
                    cover = covers[song.cover],
                    length = song.length,
                    year = song.year,
                )
            }
            Library(
                songs,
                albums.values.toList(),
                artists.values.toList(),
            )
        }

        suspend fun fromFolder(folder: File): Library = coroutineScope {
            val songs = folder.walk()
                .filter { it.isFile }
                .map { file ->
                    async {
                        if (file.extension.equals("mp3", true)) {
                            try {
                                RawMetadataSong.fromMp3(file)
                            } catch (e: Exception) {
                                // TODO: better log
                                e.printStackTrace()
                                null
                            }
                        } else null
                    }
                }
                .toList()
                .awaitAll()
                .filterNotNull()
            from(songs)
        }
    }
}