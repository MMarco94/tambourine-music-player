package io.github.mmarco94.tambourine.mpris

import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.utils.variant
import org.freedesktop.dbus.types.Variant
import org.mpris.MediaPlayer2.TrackId
import java.io.File
import kotlin.time.Duration

// See https://www.freedesktop.org/wiki/Specifications/mpris-spec/metadata/
data class MPRISMetadata(
    val trackId: TrackId,
    val length: Duration,
    val artist: List<String>,
    val albumArtist: List<String>,
    val album: String,
    val title: String,
    val trackNumber: Long?,
    val discNumber: Long?,
    val artUrl: File?,
    val lyrics: String?,
) {

    val variant: Variant<*> = buildMap {
        put("mpris:trackid", trackId.variant())
        put("mpris:length", length.inWholeMicroseconds.variant())
        put("xesam:artist", artist.variant())
        put("xesam:albumArtist", albumArtist.variant())
        put("xesam:album", album.variant())
        put("xesam:title", title.variant())
        if (trackNumber != null) {
            put("xesam:trackNumber", trackNumber.variant())
        }
        if (discNumber != null) {
            put("xesam:discNumber", discNumber.variant())
        }
        if (artUrl != null) {
            val artUri = artUrl.toURI().toString().replace("file:", "file://")
            //val artUri = "file://${artUrl.absolutePath}"
            put("mpris:artUrl", artUri.variant())
        }
        if (lyrics != null) {
            put("xesam:asText", lyrics.variant())
        }
    }.variant()
}


fun Song.mprisTrackId() = "/io/github/MMarco94/music-player/" + hashCode().toString()
fun Song.mprisMetadata(): MPRISMetadata {
    return MPRISMetadata(
        trackId = mprisTrackId(),
        length = length,
        artist = listOf(artist.name),
        albumArtist = listOf(artist.name),
        album = album.title,
        title = title,
        trackNumber = track?.toLong(),
        discNumber = disk?.toLong(),
        artUrl = cover?.file,
        lyrics = lyrics?.rawString,
    )
}