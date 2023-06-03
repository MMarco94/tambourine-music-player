package audio

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import javazoom.jl.player.Player
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.skia.Image
import java.io.File

data class Artist(
    val name: String,
)

data class Album(
    val title: String,
    val artist: Artist,
)

data class Song(
    val file: File,
    val track: Int? = null,
    val title: String,
    val album: Album,
    val cover: ImageBitmap?,
) {

    fun play() {
        val p = Player(file.inputStream())
        p.play()
    }
}

data class Library(
    val songs: Set<Song>,
) {
    val albums: Map<Album, Set<Song>> = songs.groupBy { it.album }.mapValues { it.value.toSet() }
    val artists: Map<Artist, Set<Album>> = albums.keys.groupBy { it.artist }.mapValues { it.value.toSet() }

    companion object {

        suspend fun from(metadata: Collection<RawMetadataSong>): Library = coroutineScope {
            val artists = mutableMapOf<String, Artist>()
            val albums = mutableMapOf<Pair<Artist, String>, Album>()
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

            val songs = metadata.mapTo(mutableSetOf()) { song ->
                val artist = artists.getOrPut(song.nnArtist) {
                    Artist(song.nnArtist)
                }
                val album = albums.getOrPut(artist to song.nnAlbum) {
                    Album(song.nnAlbum, artist)
                }
                Song(song.file, song.track, song.nnTitle, album, covers[song.cover])
            }
            Library(songs)
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