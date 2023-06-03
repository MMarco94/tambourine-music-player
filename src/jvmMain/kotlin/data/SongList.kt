package data

import orNoop

data class SongListOptions(
    val artistSorter: ArtistSorter = ArtistSorter.ALPHABETICAL,
    val albumSorter: AlbumSorter = AlbumSorter.ALPHABETICAL,
    val songSorter: Comparator<Song>? = compareBy { it.title },
    val artistFilter: Artist? = null,
    val albumFilter: Album? = null,
) {
    fun withArtistFilter(artistFilter: Artist?): SongListOptions {
        return if (artistFilter == null) {
            copy(artistFilter = null, albumFilter = null)
        } else copy(
            artistFilter = artistFilter,
            albumFilter = if (albumFilter != null && albumFilter.artist == artistFilter) albumFilter
            else null
        )
    }

    fun withAlbumFilter(albumFilter: Album?): SongListOptions {
        return if (albumFilter == null) {
            copy(albumFilter = null)
        } else copy(
            artistFilter = albumFilter.artist,
            albumFilter = albumFilter
        )
    }
}

sealed interface SongListItem {
    data class ArtistListItem(val artist: Artist, val songs: List<Song>) : SongListItem
    data class AlbumListItem(val album: Album, val songs: List<Song>) : SongListItem
    data class SingleSongListItem(
        val song: Song
    ) : SongListItem
}

fun generateSongList(
    library: Library,
    options: SongListOptions,
): List<SongListItem> {
    val lib = library
        .filter(options.artistFilter, options.albumFilter)
        .sort(
            options.artistSorter.comparator.orNoop(),
            options.albumSorter.comparator.orNoop(),
            options.songSorter.orNoop(),
        )

    return if (options.albumSorter == AlbumSorter.NONE) {
        if (options.artistFilter == null && options.artistSorter != ArtistSorter.NONE) {
            lib.artists.map { artist ->
                SongListItem.ArtistListItem(
                    artist,
                    lib.songsByArtist.getValue(artist),
                )
            }
        } else {
            lib.songs.map { song ->
                SongListItem.SingleSongListItem(song)
            }
        }
    } else {
        lib.albums.map { album ->
            SongListItem.AlbumListItem(
                album,
                // TODO: respect song sorting
                lib.songsByAlbum.getValue(album).sortedWith(compareBy<Song> { it.track }),
            )
        }
    }
}