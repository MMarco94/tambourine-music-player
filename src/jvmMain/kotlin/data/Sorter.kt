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
}