package io.github.mmarco94.tambourine.data

import io.github.mmarco94.tambourine.generated.resources.*
import org.jetbrains.compose.resources.StringResource

interface Sorter<T> {
    val label: StringResource?
    val fullDescription: StringResource?
    val isInverse: Boolean?
    val comparator: Comparator<T>?
}

enum class ArtistSorter(
    override val label: StringResource?,
    override val fullDescription: StringResource?,
    override val isInverse: Boolean?,
    override val comparator: Comparator<Artist>?,
) : Sorter<Artist> {
    NONE(null, null, null, null),
    ALPHABETICAL(
        Res.string.artist_sorter_name,
        Res.string.artist_sorter_name_description,
        false,
        compareBy { it.name }),
    ALPHABETICAL_DESC(
        Res.string.artist_sorter_name,
        Res.string.artist_sorter_name_inverse_description,
        true,
        compareByDescending { it.name }
    ),
}

enum class AlbumSorter(
    override val label: StringResource?,
    override val fullDescription: StringResource?,
    override val isInverse: Boolean?,
    override val comparator: Comparator<Album>?,
) : Sorter<Album> {
    NONE(null, null, null, null),
    ALPHABETICAL(
        Res.string.album_sorter_title,
        Res.string.album_sorter_title_description,
        false,
        compareBy { it.title }),
    ALPHABETICAL_DESC(
        Res.string.album_sorter_title,
        Res.string.album_sorter_title_inverse_description,
        true,
        compareByDescending { it.title }
    ),
    YEAR(
        Res.string.album_sorter_year,
        Res.string.album_sorter_year_description,
        false,
        compareByDescending { it.stats.year?.last ?: Int.MIN_VALUE }),
    YEAR_DESC(
        Res.string.album_sorter_year,
        Res.string.album_sorter_year_inverse_description,
        true,
        compareBy { it.stats.year?.first ?: Int.MAX_VALUE }
    ),
}

enum class SongSorter(
    override val label: StringResource,
    override val fullDescription: StringResource?,
    override val isInverse: Boolean?,
    override val comparator: Comparator<Song>,
    val inAlbumOnly: Boolean = false,
) : Sorter<Song> {
    TRACK(
        Res.string.song_sorter_track,
        Res.string.song_sorter_track_description,
        false,
        compareBy<Song> { it.disk }.thenBy { it.track },
        true
    ),
    TRACK_DESC(
        Res.string.song_sorter_track,
        Res.string.song_sorter_track_inverse_description,
        true,
        compareByDescending<Song> { it.disk }.thenByDescending { it.track },
        true
    ),
    ALPHABETICAL(
        Res.string.song_sorter_title,
        Res.string.song_sorter_title_description,
        true,
        compareBy { it.title }
    ),
    ALPHABETICAL_DESC(
        Res.string.song_sorter_title,
        Res.string.song_sorter_title_inverse_description,
        false,
        compareByDescending { it.title }),
    YEAR(
        Res.string.song_sorter_year,
        Res.string.song_sorter_year_description,
        true,
        compareByDescending<Song> { it.year ?: Int.MIN_VALUE }.thenByDescending { it.disk }
            .thenByDescending { it.track }),
    YEAR_DESC(
        Res.string.song_sorter_year,
        Res.string.song_sorter_year_inverse_description,
        false,
        compareBy<Song> { it.year ?: Int.MAX_VALUE }.thenBy { it.disk }.thenBy { it.track }), ;
}
