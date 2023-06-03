package data

interface Sorter<T> {
    val label: String?
    val comparator: Comparator<T>?
}

enum class ArtistSorter(
    override val label: String?,
    override val comparator: Comparator<Artist>?,
) : Sorter<Artist> {
    NONE(null, null),
    ALPHABETICAL("Name", compareBy { it.name }),
    ALPHABETICAL_DESC("Name Z->A", compareByDescending { it.name }),
}

enum class AlbumSorter(
    override val label: String?,
    override val comparator: Comparator<Album>?,
) : Sorter<Album> {
    NONE(null, null),
    ALPHABETICAL("Title", compareBy { it.title }),
    ALPHABETICAL_DESC("Title Z->A", compareByDescending { it.title }),
    YEAR("Year", compareByDescending { it.stats.year?.last ?: Int.MIN_VALUE }),
    YEAR_DESC("Year (older first)", compareBy { it.stats.year?.first ?: Int.MAX_VALUE }),
}

enum class SongSorter(
    override val label: String,
    override val comparator: Comparator<Song>,
    val inAlbumOnly: Boolean = false,
) : Sorter<Song> {
    TRACK("Track", compareBy { it.track }, true),
    ALPHABETICAL("Title", compareBy { it.title }),
    ALPHABETICAL_DESC("Title Z->A", compareByDescending { it.title }),
    YEAR("Year", compareByDescending<Song> { it.year ?: Int.MIN_VALUE }.thenByDescending { it.track }),
    YEAR_DESC("Year (older first)", compareBy<Song> { it.year ?: Int.MAX_VALUE }.thenBy { it.track }),
    ;
}