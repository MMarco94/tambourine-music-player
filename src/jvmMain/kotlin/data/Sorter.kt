package data

interface Sorter<T> {
    val label: String?
    val comparator: Comparator<T>?

    val allByThis get():String = label?.let { "all by $it" } ?: "all"
}

enum class ArtistSorter(
    override val label: String?,
    override val comparator: Comparator<Artist>?,
) : Sorter<Artist> {
    NONE(null, null),
    ALPHABETICAL("name", compareBy { it.name }),
    ALPHABETICAL_DESC("name Z->A", compareByDescending { it.name }),
}

enum class AlbumSorter(
    override val label: String?,
    override val comparator: Comparator<Album>?,
) : Sorter<Album> {
    NONE(null, null),
    ALPHABETICAL("title", compareBy { it.title }),
    ALPHABETICAL_DESC("title Z->A", compareByDescending { it.title }),
    NEWER("newer first", compareByDescending { it.stats.year?.last ?: Int.MIN_VALUE }),
    OLDER("older first", compareBy { it.stats.year?.first ?: Int.MAX_VALUE }),
}

enum class SongSorter(
    override val label: String,
    override val comparator: Comparator<Song>,
    val inAlbumOnly: Boolean = false,
) : Sorter<Song> {
    TRACK("track", compareBy { it.track }, true),
    ALPHABETICAL("title", compareBy { it.title }),
    ALPHABETICAL_DESC("title Z->A", compareByDescending { it.title }),
    NEWER("newer first", compareByDescending<Song> { it.year ?: Int.MIN_VALUE }.thenByDescending { it.track }),
    OLDER("older first", compareBy<Song> { it.year ?: Int.MAX_VALUE }.thenBy { it.track }),
    ;

    val byThis get():String = "by $label"
}