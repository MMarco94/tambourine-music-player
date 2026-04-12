package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.utils.formatSuspend
import io.github.mmarco94.tambourine.utils.trimToNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.datetime.LocalDate
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RawMetadataSong(
    val file: Path,
    override val disk: Int?,
    override val track: Int?,
    override val length: Duration,
    val formattedLength: String,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumArtist: String?,
    override val year: Int?,
    val cover: Deferred<AlbumCover?>,
    val lyrics: Lyrics?,
) : BaseSong {
    val nnTitle get() = title ?: file.nameWithoutExtension
    val nnAlbum get() = album ?: "Unknown"
    val nnArtist get() = artist ?: "Unknown"
    val nnAlbumArtist get() = albumArtist ?: nnArtist

    companion object {

        suspend fun fromMusicFile(file: Path, decoder: CoversDecoder): RawMetadataSong {
            val f = AudioFileIO.read(file.toFile())
            val tag = f.tag
            val header = f.audioHeader

            val yearStr = tag.getFirst(FieldKey.YEAR)
            val year = if (yearStr.isNotBlank()) {
                yearStr.toIntOrNull() ?: try {
                    LocalDate.parse(yearStr)
                } catch (_: IllegalArgumentException) {
                    null
                }?.year
            } else null

            val length = header.preciseTrackLength.seconds
            return RawMetadataSong(
                file = file,
                disk = tag.getFirst(FieldKey.DISC_NO)?.toIntOrNull(),
                track = tag.getFirst(FieldKey.TRACK)?.toIntOrNull(),
                length = length,
                formattedLength = length.formatSuspend(),
                title = tag.getFirst(FieldKey.TITLE)?.trimToNull(),
                album = tag.getFirst(FieldKey.ALBUM)?.trimToNull(),
                artist = tag.getFirst(FieldKey.ARTIST)?.trimToNull(),
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST)?.trimToNull(),
                year = year,
                cover = tag.firstArtwork?.binaryData?.let { decoder.decode(it) } ?: CompletableDeferred(value = null),
                lyrics = Lyrics.of(tag.getFirst(FieldKey.LYRICS)),
            )
        }
    }
}
