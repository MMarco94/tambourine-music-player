@file:OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)

package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private sealed interface SortFilterOption {
    val name: String
    val description: String get() = name
    val transform: (SongListOptions) -> SongListOptions

    data class Sort(
        val sorter: Sorter<*>,
        override val transform: (SongListOptions) -> SongListOptions,
    ) : SortFilterOption {
        override val name get() = sorter.label ?: "Do not sort"
        override val description get() = sorter.fullDescription ?: "Do not sort"
        val icon: ImageVector? = when (sorter.isInverse) {
            true -> Icons.Filled.ArrowUpward
            false -> Icons.Filled.ArrowDownward
            else -> null
        }
    }

    data class Filter<T>(
        override val name: String,
        val element: T,
        override val transform: (SongListOptions) -> SongListOptions,
    ) : SortFilterOption
}

@Composable
private fun TagWithOptions(
    selected: SortFilterOption,
    icon: ImageVector,
    description: String,
    reset: SongListOptions?,
    optionsComposable: @Composable BoxScope.(dismiss: () -> Unit) -> Unit,
    setOptions: (SongListOptions) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Box(Modifier.padding(2.dp)) {
        TagWithPopup(
            selected is SortFilterOption.Filter<*>,
            open = open,
            setOpen = { open = it },
            content = {
                Row(
                    Modifier.height(IntrinsicSize.Max).heightIn(min = 40.dp).animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, description, Modifier.padding(8.dp))
                    Column(Modifier.padding(8.dp)) {
                        if (selected is SortFilterOption.Sort) {
                            Text(description, style = MaterialTheme.typography.subtitle1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selected.name, style = MaterialTheme.typography.subtitle2)
                                if (selected.icon != null) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(selected.icon, null, Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Text(selected.name)
                        }
                    }
                    if (reset != null) {
                        IconButton({
                            setOptions(reset)
                        }, Modifier.width(40.dp).fillMaxHeight()) {
                            Image(Icons.Filled.Close, "Reset", Modifier.padding(8.dp))
                        }
                    }
                }
            },
            popup = {
                Card(Modifier.width(480.dp)) {
                    Box(Modifier
                        .clickable(remember { MutableInteractionSource() }, indication = null) { open = false }
                    ) {
                        optionsComposable { open = false }
                        IconButton({ open = false }, Modifier.align(Alignment.TopEnd).size(40.dp)) {
                            Icon(Icons.Filled.Close, "Close")
                        }
                    }
                }
            },
        )
    }
}

private class FilterSortPopupRenderer(
    val listOptions: SongListOptions,
    val selected: SortFilterOption,
    val dismissPopup: () -> Unit,
    val setOptions: (SongListOptions) -> Unit,
) {
    @Composable
    fun CategorySeparatorSort() = CategorySeparator(Icons.Filled.Sort, "Sort")

    @Composable
    fun CategorySeparatorFilter() = CategorySeparator(Icons.Filled.FilterList, "Filter")

    @Composable
    fun Row(item: SortFilterOption) {
        SimpleListItem(item.description, item)
    }

    @Composable
    fun BoxScope.ScrollBar(state: LazyListState) {
        ScrollBar(rememberScrollbarAdapter(state))
    }

    @Composable
    fun BoxScope.ScrollBar(state: LazyGridState) {
        ScrollBar(rememberScrollbarAdapter(state))
    }

    @Composable
    private fun BoxScope.ScrollBar(adapter: androidx.compose.foundation.v2.ScrollbarAdapter) {
        Box(Modifier.matchParentSize()) {
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                adapter = adapter
            )
        }
    }

    fun onClick(item: SortFilterOption) {
        dismissPopup()
        setOptions(item.transform(listOptions))
    }

    @Composable
    fun background(item: SortFilterOption): State<Color> {
        return animateColorAsState(
            if (item == selected) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.primary.copy(alpha = 0f)
            }
        )
    }

    inline fun render(f: FilterSortPopupRenderer.() -> Unit) {
        f()
    }
}

@Composable
private fun <T> BoxScope.FilterSortPopup(
    listOptions: SongListOptions,
    selected: SortFilterOption,
    sortOptions: List<SortFilterOption.Sort>,
    filterOptions: List<SortFilterOption.Filter<T>>,
    dismissPopup: () -> Unit,
    setOptions: (SongListOptions) -> Unit,
) {
    FilterSortPopupRenderer(
        listOptions, selected, dismissPopup, setOptions
    ).render {
        val state = rememberLazyListState()
        LazyColumn(state = state) {
            if (sortOptions.isNotEmpty()) {
                item { CategorySeparatorSort() }
            }
            items(sortOptions) { item ->
                Row(item)
            }
            if (filterOptions.isNotEmpty()) {
                item { CategorySeparatorFilter() }
            }
            items(filterOptions) { item ->
                Row(item)
            }
        }
        ScrollBar(state)
    }
}

@Composable
fun SongListOptionsController(
    library: Library,
    options: SongListOptions,
    setOptions: (SongListOptions) -> Unit,
) {
    FlowRow(Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
        val libForArtists = library.sort(
            options.artistSorter.comparator ?: compareBy { it.name },
            noopComparator(), noopComparator(), noopComparator(),
        )
        val artistsSorters = ArtistSorter.values().associateWith { sorter ->
            SortFilterOption.Sort(sorter) {
                it.withArtistFilter(null).copy(artistSorter = sorter)
            }
        }
        val artistsFilters = libForArtists.artists.associateWith { artist ->
            SortFilterOption.Filter(artist.name, artist) {
                it.withArtistFilter(artist)
            }
        }
        val selectedArtistOption = if (options.artistFilter == null) {
            artistsSorters.getValue(options.artistSorter)
        } else {
            artistsFilters.getValue(options.artistFilter)
        }
        TagWithOptions(
            selectedArtistOption,
            description = "Artists",
            icon = Icons.Filled.Groups,
            reset = if (options.artistFilter != null) {
                options.withArtistFilter(null)
            } else null,
            setOptions = setOptions,
            optionsComposable = { dismissPopup ->
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
            SortFilterOption.Sort(sorter) {
                it.withAlbumFilter(null).copy(albumSorter = sorter)
            }
        }
        val albumsFilters = libForAlbums.albums.associateWith { album ->
            SortFilterOption.Filter(
                album.title(options.artistFilter == null && options.artistSorter != ArtistSorter.NONE),
                album
            ) {
                it.withAlbumFilter(album)
            }
        }
        val selectedAlbumOptions = if (options.albumFilter == null) {
            albumsSorters.getValue(options.albumSorter)
        } else {
            albumsFilters.getValue(options.albumFilter)
        }
        TagWithOptions(
            selectedAlbumOptions,
            description = "Albums",
            icon = Icons.Filled.Album,
            reset = if (options.albumFilter != null) {
                options.withAlbumFilter(null)
            } else null,
            setOptions = setOptions,
            optionsComposable = { dismissPopup ->
                FilterSortPopupRenderer(
                    options,
                    selectedAlbumOptions,
                    dismissPopup,
                    setOptions
                ).render {
                    val state = rememberLazyGridState()
                    LazyVerticalGrid(GridCells.Adaptive(160.dp), state = state) {
                        val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxCurrentLineSpan) }
                        item(span = fullSpan, content = { CategorySeparatorSort() })
                        items(albumsSorters.values.toList(), span = { fullSpan() }) { item ->
                            Row(item)
                        }
                        item(span = fullSpan, content = { CategorySeparatorFilter() })
                        items(albumsFilters.values.toList()) { item ->
                            AlbumGridItem(item)
                        }
                    }
                    ScrollBar(state)
                }
            }
        )

        val isGroupingByAlbum = options.isInAlbumMode
        val songSorters = SongSorter.values().filter { !it.inAlbumOnly || isGroupingByAlbum }.associateWith { sorter ->
            SortFilterOption.Sort(sorter) {
                it.withSongSorter(sorter)
            }
        }
        val selectedSongOption = if (isGroupingByAlbum) {
            songSorters.getValue(options.songSorterInAlbum)
        } else {
            songSorters.getValue(options.songSorter)
        }
        TagWithOptions(selectedSongOption,
            description = "Songs",
            icon = Icons.Filled.MusicNote,
            reset = null,
            setOptions = setOptions,
            optionsComposable = { dismissPopup ->
                FilterSortPopup<Nothing>(
                    options,
                    selectedSongOption,
                    songSorters.values.toList(),
                    emptyList(),
                    dismissPopup,
                    setOptions
                )
            })
    }
}


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
private fun FilterSortPopupRenderer.SimpleListItem(
    text: String,
    item: SortFilterOption,
) {
    val bg: Color by background(item)
    Row(Modifier.selectable(item == selected) { onClick(item) }.background(bg).fillMaxWidth().padding(8.dp)) {
        Text(text)
    }
}

@Composable
private fun FilterSortPopupRenderer.AlbumGridItem(item: SortFilterOption.Filter<Album>) {
    val bg: Color by background(item)
    Column(
        Modifier
            .selectable(item == selected) { onClick(item) }
            .background(bg)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.padding(8.dp)) {
            AlbumCover(item.element.cover, Modifier.fillMaxSize(), MaterialTheme.shapes.medium)
        }
        Text(
            item.element.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )
        Text(
            item.element.artist.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center
        )
    }
}