@file:OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)

package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import data.*
import noopComparator
import orNoop

@Composable
private fun Tag(
    enabled: Boolean, content: @Composable () -> Unit, onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (enabled) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.primary.copy(alpha = 0f)
        }
    )
    val contentColor by animateColorAsState(
        if (enabled) {
            MaterialTheme.colors.onPrimary
        } else {
            MaterialTheme.colors.onSurface
        }
    )
    Card(
        Modifier,
        backgroundColor = bg,
        contentColor = contentColor,
        elevation = 0.dp,
    ) {
        Box(Modifier.clickable { onClick() }) {
            content()
        }
    }
}

@Composable
private fun TagWithPopup(
    enabled: Boolean,
    content: @Composable () -> Unit,
    open: Boolean,
    setOpen: (Boolean) -> Unit,
    popup: @Composable () -> Unit,
) {
    Box {
        Tag(enabled, content) {
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

private data class SortFilterOption(
    val name: String,
    val isEnabled: Boolean,
    val transform: (SongListOptions) -> SongListOptions,
)

@Composable
private fun TagWithOptions(
    selected: SortFilterOption,
    icon: ImageVector,
    reset: SongListOptions?,
    optionsComposable: @Composable (dismiss: () -> Unit) -> Unit,
    setOptions: (SongListOptions) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Box(Modifier.padding(2.dp)) {
        TagWithPopup(
            selected.isEnabled,
            open = open,
            setOpen = { open = it },
            content = {
                Row(
                    Modifier.height(IntrinsicSize.Max).heightIn(min = 40.dp).animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.padding(8.dp)) {
                        Icon(icon, null)
                        Spacer(Modifier.width(8.dp))
                        Text(selected.name)
                    }
                    if (reset != null) {
                        IconButton({
                            setOptions(reset)
                        }, Modifier.size(40.dp).fillMaxHeight()) {
                            Image(Icons.Filled.Close, "Reset", Modifier.padding(8.dp))
                        }
                    }
                }
            },
            popup = {
                Card(Modifier.width(400.dp)) {
                    if (open) {
                        optionsComposable { open = false }
                    }
                }
            },
        )
    }
}

@Composable
private fun FilterSortPopup(
    listOptions: SongListOptions,
    selected: SortFilterOption,
    sortOptions: List<SortFilterOption>,
    filterOptions: List<SortFilterOption>,
    dismissPopup: () -> Unit,
    setOptions: (SongListOptions) -> Unit,
) {
    LazyColumn {
        if (sortOptions.isNotEmpty()) {
            item { CategorySeparatorSort() }
        }
        items(sortOptions) { item ->
            SimpleListItem(item.name, item == selected) {
                dismissPopup()
                setOptions(item.transform(listOptions))
            }
        }
        if (filterOptions.isNotEmpty()) {
            item { CategorySeparatorFilter() }
        }
        items(filterOptions) { item ->
            SimpleListItem(item.name, item == selected) {
                dismissPopup()
                setOptions(item.transform(listOptions))
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
    FlowRow(Modifier.padding(2.dp)) {
        val libForArtists = library.sort(
            options.artistSorter.comparator ?: compareBy { it.name },
            noopComparator(), noopComparator(), noopComparator(),
        )
        val artistsSorters = ArtistSorter.values().associateWith { sorter ->
            SortFilterOption(sorter.label ?: "Do not sort", false) {
                it.withArtistFilter(null).copy(artistSorter = sorter)
            }
        }
        val artistsFilters = libForArtists.artists.associateWith { artist ->
            SortFilterOption(artist.name, true) {
                it.withArtistFilter(artist)
            }
        }
        val selectedArtistOption = if (options.artistFilter == null) {
            artistsSorters.getValue(options.artistSorter)
        } else {
            artistsFilters.getValue(options.artistFilter)
        }
        TagWithOptions(selectedArtistOption, icon = Icons.Filled.Groups, reset = if (options.artistFilter != null) {
            options.withArtistFilter(null)
        } else null, setOptions = setOptions, optionsComposable = { dismissPopup ->
            FilterSortPopup(
                options,
                selectedArtistOption,
                artistsSorters.values.toList(),
                artistsFilters.values.toList(),
                dismissPopup,
                setOptions
            )
        })


        val libForAlbums = library.sort(
            options.artistSorter.comparator.orNoop(),
            options.albumSorter.comparator ?: compareBy { it.title },
            noopComparator(), noopComparator(),
        ).filter(options.artistFilter, null)
        val albumsSorters = AlbumSorter.values().associateWith { sorter ->
            SortFilterOption(sorter.label ?: "Do not sort", false) {
                it.withAlbumFilter(null).copy(albumSorter = sorter)
            }
        }
        val albumsFilters = libForAlbums.albums.associateWith { album ->
            SortFilterOption(
                album.title(options.artistFilter == null && options.artistSorter != ArtistSorter.NONE), true
            ) {
                it.withAlbumFilter(album)
            }
        }
        val selectedAlbumOptions = if (options.albumFilter == null) {
            albumsSorters.getValue(options.albumSorter)
        } else {
            albumsFilters.getValue(options.albumFilter)
        }
        TagWithOptions(selectedAlbumOptions, icon = Icons.Filled.Album, reset = if (options.albumFilter != null) {
            options.withAlbumFilter(null)
        } else null, setOptions = setOptions, optionsComposable = { dismissPopup ->
            LazyVerticalGrid(GridCells.Adaptive(128.dp)) {
                val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxCurrentLineSpan) }
                item(span = fullSpan, content = { CategorySeparatorSort() })
                items(albumsSorters.values.toList(), span = { fullSpan() }) { item ->
                    SimpleListItem(item.name, item == selectedAlbumOptions) {
                        dismissPopup()
                        setOptions(item.transform(options))
                    }
                }
                item(span = fullSpan, content = { CategorySeparatorFilter() })
                items(albumsFilters.values.toList()) { item ->
                    SimpleListItem(item.name, item == selectedAlbumOptions) {
                        dismissPopup()
                        setOptions(item.transform(options))
                    }
                }
            }
        })

        val isGroupingByAlbum = options.isInAlbumMode
        val songSorters = SongSorter.values().filter { !it.inAlbumOnly || isGroupingByAlbum }.associateWith { sorter ->
            SortFilterOption(sorter.label, false) {
                it.withSongSorter(sorter)
            }
        }
        val selectedSongOption = if (isGroupingByAlbum) {
            songSorters.getValue(options.songSorterInAlbum)
        } else {
            songSorters.getValue(options.songSorter)
        }
        TagWithOptions(selectedSongOption,
            icon = Icons.Filled.MusicNote,
            reset = null,
            setOptions = setOptions,
            optionsComposable = { dismissPopup ->
                FilterSortPopup(
                    options, selectedSongOption, songSorters.values.toList(), emptyList(), dismissPopup, setOptions
                )
            })
    }
}


@Composable
private fun CategorySeparatorSort() = CategorySeparator(Icons.Filled.Sort, "Sort")

@Composable
private fun CategorySeparatorFilter() = CategorySeparator(Icons.Filled.FilterList, "Filter")

@Composable
private fun CategorySeparator(
    icon: ImageVector,
    name: String,
) {
    Row(Modifier.padding(8.dp)) {
        Icon(icon, name)
        Spacer(Modifier.padding(8.dp))
        Text(name)
    }
}

@Composable
private fun SimpleListItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val targetBg by animateColorAsState(
        if (selected) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.primary.copy(alpha = 0f)
        }
    )
    Row(Modifier.selectable(selected) { onClick() }.background(targetBg).fillMaxWidth().padding(8.dp)) {
        Text(text)
    }
}