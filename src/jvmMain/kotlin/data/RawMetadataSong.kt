package data

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jetbrains.skia.Image
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    override val track: Int?,
    override val length: Duration,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumArtist: String?,
    override val year: Int?,
    // TODO: make it so they're not all in memory
    val cover: RawImage?,
) : BaseSong {
    val nnTitle get() = title ?: file.nameWithoutExtension
    val nnAlbum get() = album ?: "Unknown"
    val nnArtist get() = artist ?: "Unknown"
    val nnAlbumArtist get() = albumArtist ?: nnArtist

    companion object{

        fun fromMusicFile(file: File): RawMetadataSong {
            val f = AudioFileIO.read(file)
            val tag = f.tag
            val header = f.audioHeader

            return RawMetadataSong(
                file = file,
                track = tag.getFirst(FieldKey.TRACK)?.toIntOrNull(),
                length = header.preciseTrackLength.seconds,
                title = tag.getFirst(FieldKey.TITLE),
                album = tag.getFirst(FieldKey.ALBUM),
                artist = tag.getFirst(FieldKey.ARTIST),
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST),
                year = tag.getFirst(FieldKey.YEAR)?.toIntOrNull(),
                cover = tag.firstArtwork?.binaryData?.let { RawImage(it) }
            )
        }
    }
}
