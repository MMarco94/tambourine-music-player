package io.github.musicplayer.data

import androidx.compose.ui.graphics.ImageBitmap
import io.github.musicplayer.utils.trimToNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RawMetadataSong(
    val file: File,
    override val track: Int?,
    override val length: Duration,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumArtist: String?,
    override val year: Int?,
    val cover: Deferred<ImageBitmap?>,
) : BaseSong {
    val nnTitle get() = title ?: file.nameWithoutExtension
    val nnAlbum get() = album ?: "Unknown"
    val nnArtist get() = artist ?: "Unknown"
    val nnAlbumArtist get() = albumArtist ?: nnArtist

    companion object {

        suspend fun fromMusicFile(file: File, decoder: CoversDecoder): RawMetadataSong {
            val f = AudioFileIO.read(file)
            val tag = f.tag
            val header = f.audioHeader

            return RawMetadataSong(
                file = file,
                track = tag.getFirst(FieldKey.TRACK)?.toIntOrNull(),
                length = header.preciseTrackLength.seconds,
                title = tag.getFirst(FieldKey.TITLE)?.trimToNull(),
                album = tag.getFirst(FieldKey.ALBUM)?.trimToNull(),
                artist = tag.getFirst(FieldKey.ARTIST)?.trimToNull(),
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST)?.trimToNull(),
                year = tag.getFirst(FieldKey.YEAR)?.toIntOrNull(),
                cover = tag.firstArtwork?.binaryData?.let { decoder.decode(it) } ?: CompletableDeferred(value = null)
            )
        }
    }
}
