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
    DATE(
        Res.string.album_sorter_date,
        Res.string.album_sorter_date_description,
        false,
        compareBy<Album, PartialDate?>(COMPARE_NEWEST) { it.stats.dateRange?.newest }
            .thenBy(COMPARE_NEWEST) { it.stats.dateRange?.oldest },
    ),
    DATE_DESC(
        Res.string.album_sorter_date,
        Res.string.album_sorter_date_inverse_description,
        true,
        compareBy<Album, PartialDate?>(COMPARE_OLDEST) { it.stats.dateRange?.oldest }
            .thenBy(COMPARE_OLDEST) { it.stats.dateRange?.newest },
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
    DATE(
        Res.string.song_sorter_date,
        Res.string.song_sorter_date_description,
        true,
        compareBy<Song, PartialDate?>(COMPARE_NEWEST) { it.date }
            .thenByDescending { it.disk }
            .thenByDescending { it.track }),
    DATE_DESC(
        Res.string.song_sorter_date,
        Res.string.song_sorter_date_inverse_description,
        false,
        compareBy<Song, PartialDate?>(COMPARE_OLDEST) { it.date }
            .thenBy { it.disk }
            .thenBy { it.track }),
}
