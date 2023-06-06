@file:OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)

package ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import data.*
import musicLibraryDirectory
import noopComparator
import orNoop
import ui.Tab.*
import java.io.File
import kotlin.math.roundToInt


@Composable
private fun Tag(
    active: Boolean,
    enabled: Boolean,
    content: @Composable () -> Unit,
    onClick: () -> Unit,
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
    val alpha by animateFloatAsState(if (active) 1f else inactiveAlpha)
    Card(
        Modifier.alpha(alpha),
        backgroundColor = bg,
        contentColor = contentColor,
        elevation = 0.dp,
    ) {
        Box(Modifier.clickable { onClick() }) {
            content()
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
    active: Boolean,
    enabled: Boolean,
    selected: SortFilterOption,
    icon: ImageVector,
    description: String,
    reset: SongListOptions?,
    onClick: () -> Unit,
    close: () -> Unit,
    setOptions: (SongListOptions) -> Unit,
) {
    Box(Modifier.padding(2.dp)) {
        Tag(
            active = active,
            enabled = enabled,
            onClick = onClick,
            content = {
                Row(
                    Modifier.height(IntrinsicSize.Max).heightIn(min = 40.dp).animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, description)
                        Spacer(Modifier.width(8.dp))
                        if (selected is SortFilterOption.Sort) {
                            Column {
                                SingleLineText(description)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SingleLineText(selected.name, style = MaterialTheme.typography.subtitle2)
                                    if (selected.icon != null) {
                                        Spacer(Modifier.width(2.dp))
                                        Icon(selected.icon, null, Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            SingleLineText(selected.name)
                        }
                    }
                    if (reset != null) {
                        IconButton({
                            setOptions(reset)
                            close()
                        }, Modifier.width(40.dp).fillMaxHeight()) {
                            Image(Icons.Filled.Close, "Reset", Modifier.padding(8.dp))
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
    fun CategorySeparatorSort() = CategorySeparator("Sort")

    @Composable
    fun CategorySeparatorFilter() = CategorySeparator("Select")

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
                modifier = Modifier.align(Alignment.CenterEnd), adapter = adapter
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

    @Composable
    fun contentColor(item: SortFilterOption): State<Color> {
        return animateColorAsState(
            if (item == selected) {
                MaterialTheme.colors.onPrimary
            } else {
                MaterialTheme.colors.onSurface
            }
        )
    }

    inline fun render(f: FilterSortPopupRenderer.() -> Unit) {
        f()
    }
}

private enum class Tab {
    ARTIST, ALBUM, SONG;
}

@Composable
fun LibraryHeader(
    modifier: Modifier,
    library: Library,
    options: SongListOptions,
    setOptions: (SongListOptions) -> Unit,
    openSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    var tab: Tab? by remember { mutableStateOf(null) }
    val artistRenderer = ArtistOptionsRenderer(ARTIST, library, options, setOptions)
    val albumRenderer = AlbumOptionsRenderer(ALBUM, library, options, setOptions)
    val songRenderer = SongOptionsRenderer(SONG, options, setOptions)

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                Modifier.padding(2.dp).weight(1f).animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                artistRenderer.Tag(tab) { tab = it }
                albumRenderer.Tag(tab) { tab = it }
                songRenderer.Tag(tab) { tab = it }
            }
            Box(Modifier.size(48.dp)) {
                Crossfade(tab != null) {
                    if (it) {
                        IconButton({ tab = null }, Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.Close, "Close")
                        }
                    } else {
                        IconButton({ openSettings() }, Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.Settings, "Settings")
                        }
                    }
                }
            }
        }
        Divider()

        AnimatedContent(tab, transitionSpec = {
            if (this.initialState == null || this.targetState == null) {
                fadeIn() with fadeOut()
            } else {
                val mult = if (this.initialState!! < this.targetState!!) 1 else -1
                slideInHorizontally { mult * it } with slideOutHorizontally { mult * -it }
            } using SizeTransform(sizeAnimationSpec = { _, _ -> spring(stiffness = Spring.StiffnessLow) })
        }) { t ->
            if (t != null) {
                val renderer = when (t) {
                    ARTIST -> artistRenderer
                    ALBUM -> albumRenderer
                    SONG -> songRenderer
                }
                Layout(content = {
                    Box {
                        with(renderer) { Options { tab = null } }
                        Divider(Modifier.align(Alignment.BottomCenter))
//                        IconButton({ tab = null }, Modifier.align(Alignment.TopEnd).size(40.dp)) {
//                            Icon(Icons.Filled.Close, "Close")
//                        }
                    }
                }) { measurables, constraints ->
                    val newC = constraints.copy(
                        maxHeight = constraints.maxHeight - 80.dp.toPx().roundToInt(),
                    )
                    val p = measurables.single().measure(newC)
                    layout(p.width, p.height) {
                        p.place(0, 0)
                    }
                }
            } else {
                Spacer(Modifier.fillMaxWidth())
            }
        }

        val alpha by animateFloatAsState(if (tab == null) 1f else inactiveAlpha)
        Box(
            Modifier.graphicsLayer { this.alpha = alpha }
        ) {
            content()
            if (tab != null) {
                Box(Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { tab = null }
                )
            }
        }
    }
}

private interface OptionsRenderer {
    val description: String
    val tab: Tab
    val activeFilter: Any?
    val withoutFilter: SongListOptions
    val selectedOption: SortFilterOption
    val setOptions: (SongListOptions) -> Unit

    @Composable
    fun BoxScope.Options(close: () -> Unit)

    @Composable
    fun Tag(currentTab: Tab?, setTab: (Tab?) -> Unit) {
        TagWithOptions(
            active = currentTab == null || tab == currentTab,
            selected = selectedOption,
            enabled = activeFilter != null,
            description = description,
            icon = Icons.Filled.Groups,
            reset = if (activeFilter != null) withoutFilter else null,
            setOptions = setOptions,
            onClick = { setTab(if (tab == currentTab) null else tab) },
            close = { setTab(null) },
        )
    }

    @Composable
    fun <T> BoxScope.FilterSortContent(
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
            LazyColumn(state = state, contentPadding = PaddingValues(bottom = 16.dp)) {
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
}

private class ArtistOptionsRenderer(
    override val tab: Tab,
    library: Library,
    private val options: SongListOptions,
    override val setOptions: (SongListOptions) -> Unit,
) : OptionsRenderer {
    override val description = "Artists"
    override val activeFilter = options.artistFilter
    override val withoutFilter = options.withArtistFilter(null)
    private val libForArtists = library.sort(
        options.artistSorter.comparator ?: compareBy { it.name },
        noopComparator(), noopComparator()
    )
    private val artistsSorters = ArtistSorter.values().associateWith { sorter ->
        SortFilterOption.Sort(sorter) {
            it.withArtistFilter(null).copy(artistSorter = sorter)
        }
    }
    private val artistsFilters = libForArtists.artists.associateWith { artist ->
        SortFilterOption.Filter(artist.name, artist) {
            it.withArtistFilter(artist)
        }
    }
    override val selectedOption = if (options.artistFilter == null) {
        artistsSorters.getValue(options.artistSorter)
    } else {
        artistsFilters.getValue(options.artistFilter)
    }

    @Composable
    override fun BoxScope.Options(close: () -> Unit) {
        FilterSortContent(
            options,
            selectedOption,
            artistsSorters.values.toList(),
            artistsFilters.values.toList(),
            close,
            setOptions
        )
    }
}

private class AlbumOptionsRenderer(
    override val tab: Tab,
    library: Library,
    private val options: SongListOptions,
    override val setOptions: (SongListOptions) -> Unit,
) : OptionsRenderer {
    override val description = "Albums"
    override val activeFilter = options.albumFilter
    override val withoutFilter = options.withAlbumFilter(null)
    private val libForAlbums = library.sort(
        options.artistSorter.comparator.orNoop(),
        options.albumSorter.comparator ?: compareBy { it.title },
        noopComparator()
    ).filter(options.artistFilter, null)
    private val albumsSorters = AlbumSorter.values().associateWith { sorter ->
        SortFilterOption.Sort(sorter) {
            it.withAlbumFilter(null).copy(albumSorter = sorter)
        }
    }
    private val albumsFilters = libForAlbums.albums.associateWith { album ->
        SortFilterOption.Filter(
            album.title(options.artistFilter == null && options.artistSorter != ArtistSorter.NONE), album
        ) {
            it.withAlbumFilter(album)
        }
    }
    override val selectedOption = if (options.albumFilter == null) {
        albumsSorters.getValue(options.albumSorter)
    } else {
        albumsFilters.getValue(options.albumFilter)
    }

    @Composable
    override fun BoxScope.Options(close: () -> Unit) {
        FilterSortPopupRenderer(
            options, selectedOption, close, setOptions
        ).render {
            val state = rememberLazyGridState()
            LazyVerticalGrid(
                GridCells.Adaptive(160.dp),
                state = state,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
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
}

private class SongOptionsRenderer(
    override val tab: Tab,
    private val options: SongListOptions,
    override val setOptions: (SongListOptions) -> Unit,
) : OptionsRenderer {
    override val description = "Songs"
    override val activeFilter = null
    override val withoutFilter = options
    private val isGroupingByAlbum = options.isInAlbumMode
    private val songSorters =
        SongSorter.values().filter { !it.inAlbumOnly || isGroupingByAlbum }.associateWith { sorter ->
            SortFilterOption.Sort(sorter) {
                it.withSongSorter(sorter)
            }
        }
    override val selectedOption = if (isGroupingByAlbum) {
        songSorters.getValue(options.songSorterInAlbum)
    } else {
        songSorters.getValue(options.songSorter)
    }

    @Composable
    override fun BoxScope.Options(close: () -> Unit) {
        FilterSortContent<Nothing>(
            options,
            selectedOption,
            songSorters.values.toList(),
            emptyList(),
            close,
            setOptions
        )
    }
}

@Composable
private fun CategorySeparator(name: String) {
    Row(
        Modifier
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp, bottom = 2.dp)
    ) {
        SingleLineText(name, style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
private fun FilterSortPopupRenderer.SimpleListItem(
    text: String,
    item: SortFilterOption,
) {
    val bg: Color by background(item)
    val content: Color by contentColor(item)
    Surface(
        color = bg,
        contentColor = content,
        modifier = Modifier
            .selectable(item == selected) { onClick(item) }
            .background(bg)
            .fillMaxWidth()
            .padding(8.dp)
            .padding(start = 32.dp),
    ) {
        SingleLineText(text)
    }
}

@Composable
private fun FilterSortPopupRenderer.AlbumGridItem(item: SortFilterOption.Filter<Album>) {
    val bg: Color by background(item)
    val content: Color by contentColor(item)
    Box(Modifier.padding(horizontal = 8.dp)) {
        Surface(
            color = bg,
            contentColor = content,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.selectable(item == selected) { onClick(item) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.padding(8.dp)) {
                    AlbumCover(
                        item.element.cover,
                        Modifier.fillMaxSize(),
                        MaterialTheme.shapes.medium,
                        elevation = 8.dp
                    )
                }
                SingleLineText(
                    item.element.title, style = MaterialTheme.typography.subtitle1, textAlign = TextAlign.Center
                )
                SingleLineText(
                    item.element.artist.name, style = MaterialTheme.typography.subtitle2, textAlign = TextAlign.Center
                )
            }
        }
    }
}