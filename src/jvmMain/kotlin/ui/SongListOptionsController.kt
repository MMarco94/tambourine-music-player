@file:OptIn(ExperimentalLayoutApi::class)

package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import data.*
import noopComparator
import orNoop

@Composable
private fun Tag(
    enabled: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Box(Modifier.padding(8.dp)) {
        Card(
            Modifier,
            backgroundColor = if (enabled) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.surface
            }
        ) {
            Box(Modifier.clickable { onClick() }.padding(8.dp)) {
                Text(text)
            }
        }
    }
}

@Composable
private fun TagWithPopup(
    enabled: Boolean,
    text: String,
    open: Boolean,
    setOpen: (Boolean) -> Unit,
    popup: @Composable () -> Unit,
) {
    Box {
        Tag(enabled, text) {
            setOpen(!open)
        }
        if (open) {
            Popup(
                focusable = true,
                onDismissRequest = {
                    setOpen(false)
                },
            ) {
                popup()
            }
        }
    }
}

private data class SortOption(
    val name: String,
    val isEnabled: Boolean,
    val transform: (SongListOptions) -> SongListOptions,
)

@Composable
private fun TagWithOptions(
    listOptions: SongListOptions,
    selection: SortOption,
    text: String,
    items: List<SortOption>,
    setOptions: (SongListOptions) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val label = text + ": " + selection.name
    TagWithPopup(selection.isEnabled, label, open, { open = it }) {
        Card {
            LazyColumn {
                items(items) { item ->
                    SimpleListItem(item.name) {
                        open = false
                        setOptions(item.transform(listOptions))
                    }
                }
            }
        }
    }
}

@Composable
fun SongListOptionsController(
    library: Library,
    options: SongListOptions,
    setOptions: (SongListOptions) -> Unit,
) {
    FlowRow {
        val libForArtists = library.sort(
            options.artistSorter.comparator ?: compareBy { it.name },
            noopComparator(), noopComparator(), noopComparator(),
        )
        val artistsSorters = ArtistSorter.values().associateWith { sorter ->
            SortOption(sorter.allByThis, false) {
                it
                    .withArtistFilter(null)
                    .copy(artistSorter = sorter)
            }
        }
        val artistsFilters = libForArtists.artists.associateWith { artist ->
            SortOption(artist.name, true) {
                it.withArtistFilter(artist)
            }
        }
        TagWithOptions(
            options,
            if (options.artistFilter == null) {
                artistsSorters.getValue(options.artistSorter)
            } else {
                artistsFilters.getValue(options.artistFilter)
            },
            text = "Artist",
            items = artistsSorters.values + artistsFilters.values,
            setOptions = setOptions
        )


        val libForAlbums = library.sort(
            options.artistSorter.comparator.orNoop(),
            options.albumSorter.comparator ?: compareBy { it.title },
            noopComparator(), noopComparator(),
        ).filter(options.artistFilter, null)
        val albumsSorters = AlbumSorter.values().associateWith { sorter ->
            SortOption(sorter.allByThis, false) {
                it
                    .withAlbumFilter(null)
                    .copy(albumSorter = sorter)
            }
        }
        val albumsFilters = libForAlbums.albums.associateWith { album ->
            SortOption(album.title(options.artistFilter == null), true) {
                it.withAlbumFilter(album)
            }
        }
        TagWithOptions(
            options,
            if (options.albumFilter == null) {
                albumsSorters.getValue(options.albumSorter)
            } else {
                albumsFilters.getValue(options.albumFilter)
            },
            text = "Album",
            items = albumsSorters.values + albumsFilters.values,
            setOptions = setOptions
        )

        val isGroupingByAlbum = options.isInAlbumMode
        val songSorters = SongSorter.values()
            .filter { !it.inAlbumOnly || isGroupingByAlbum }
            .associateWith { sorter ->
                SortOption(sorter.byThis, false) {
                    it.withSongSorter(sorter)
                }
            }
        TagWithOptions(
            options,
            if (isGroupingByAlbum) {
                songSorters.getValue(options.songSorterInAlbum)
            } else {
                songSorters.getValue(options.songSorter)
            },
            text = "Songs",
            items = songSorters.values.toList(),
            setOptions = setOptions
        )
    }
}

@Composable
private fun SimpleListItem(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }.padding(8.dp)
    ) {
        Text(text)
    }
}