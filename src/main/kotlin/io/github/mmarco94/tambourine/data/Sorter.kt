package io.github.mmarco94.tambourine.data

interface Sorter<T> {
    val label: String?
    val fullDescription: String?
    val isInverse: Boolean?
    val comparator: Comparator<T>?
}

enum class ArtistSorter(
    override val label: String?,
    override val fullDescription: String?,
    override val isInverse: Boolean?,
    override val comparator: Comparator<Artist>?,
) : Sorter<Artist> {
    NONE(null, null, null, null),
    ALPHABETICAL(
        "Name",
        "By name",
        false,
        compareBy { it.name }),
    ALPHABETICAL_DESC("Name", "By name (inverse)", true, compareByDescending { it.name }),
}

enum class AlbumSorter(
    override val label: String?,
    override val fullDescription: String?,
    override val isInverse: Boolean?,
    override val comparator: Comparator<Album>?,
) : Sorter<Album> {
    NONE(null, null, null, null), ALPHABETICAL(
        "Title",
        "By title",
        false,
        compareBy { it.title }),
    ALPHABETICAL_DESC("Title", "By title (inverse)", true, compareByDescending { it.title }),
    YEAR("Year",
        "By year (newest first)",
        false,
        compareByDescending { it.stats.year?.last ?: Int.MIN_VALUE }),
    YEAR_DESC("Year", "By year (oldest first)", true, compareBy { it.stats.year?.first ?: Int.MAX_VALUE }),
}

enum class SongSorter(
    override val label: String,
    override val fullDescription: String?,
    override val isInverse: Boolean?,
    override val comparator: Comparator<Song>,
    val inAlbumOnly: Boolean = false,
) : Sorter<Song> {
    TRACK(
        "Track",
        "By position in album",
        false,
        compareBy<Song> { it.disk }.thenBy { it.track },
        true
    ),
    TRACK_DESC(
        "Track",
        "By position in album (inverse)",
        true,
        compareByDescending<Song> { it.disk }.thenByDescending { it.track },
        true
    ),
    ALPHABETICAL("Title", "By title", true, compareBy { it.title }),
    ALPHABETICAL_DESC(
        "Title",
        "By title (inverse)",
        false,
        compareByDescending { it.title }),
    YEAR("Year",
        "By year (newest first)",
        true,
        compareByDescending<Song> { it.year ?: Int.MIN_VALUE }.thenByDescending { it.disk }
            .thenByDescending { it.track }),
    YEAR_DESC("Year",
        "By year (oldest first)",
        false,
        compareBy<Song> { it.year ?: Int.MAX_VALUE }.thenBy { it.disk }.thenBy { it.track }), ;
}