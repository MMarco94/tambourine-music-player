package data

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.mpatric.mp3agic.Mp3File
import org.jetbrains.skia.Image
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RawImage(
    val bytes: ByteArray,
) {
    fun decode() = Image.makeFromEncoded(bytes).toComposeImageBitmap()

    override fun equals(other: Any?): Boolean {
        return other is RawImage && other.bytes.contentEquals(bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class RawMetadataSong(
    val file: File,
    val track: Int?,
    override val length: Duration,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumArtist: String?,
    override  val year: Int?,
    // TODO: make it so they're not all in memory
    val cover: RawImage?,
) : BaseSong {
    val nnTitle get() = title ?: file.nameWithoutExtension
    val nnAlbum get() = album ?: "Unknown"
    val nnArtist get() = artist ?: "Unknown"
    val nnAlbumArtist get() = albumArtist ?: nnArtist

    companion object {
        fun fromMp3(file: File): RawMetadataSong {
            val mp3 = Mp3File(file)
            val id3v2Tag = mp3.id3v2Tag
            val id3v1Tag = mp3.id3v1Tag
            val duration = mp3.lengthInMilliseconds.milliseconds
            return if (id3v2Tag != null) {
                RawMetadataSong(
                    file,
                    id3v2Tag.track.toIntOrNull(),
                    duration,
                    id3v2Tag.title,
                    id3v2Tag.album,
                    id3v2Tag.artist,
                    id3v2Tag.albumArtist,
                    id3v2Tag.year?.toIntOrNull(),
                    id3v2Tag.albumImage?.let { RawImage(it) },
                )
            } else if (id3v1Tag != null) {
                RawMetadataSong(
                    file,
                    id3v1Tag.track.toIntOrNull(),
                    duration,
                    id3v1Tag.title,
                    id3v1Tag.album,
                    id3v1Tag.artist,
                    null,
                    id3v1Tag.year?.toIntOrNull(),
                    null,
                )
            } else {
                RawMetadataSong(
                    file,
                    null,
                    duration,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                )
            }
        }
    }
}
