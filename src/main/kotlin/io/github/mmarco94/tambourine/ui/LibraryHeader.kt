package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.*
import io.github.mmarco94.tambourine.ui.LibraryHeaderTab.*
import io.github.mmarco94.tambourine.utils.animateContentHeight
import io.github.mmarco94.tambourine.utils.noopComparator
import io.github.mmarco94.tambourine.utils.orNoop
import kotlin.math.roundToInt

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
private fun TagForOptions(
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
    Tag(
        active = active,
        enabled = enabled,
        showAsSubtitle = selected is SortFilterOption.Sort,
        icon = icon,
        description = description,
        selectedLabel = selected.name,
        selectedIcon = (selected as? SortFilterOption.Sort)?.icon,
        reset = reset?.let { { setOptions(it); close() } },
        onClick = onClick,
    )
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
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
            }
        )
    }

    @Composable
    fun contentColor(item: SortFilterOption): State<Color> {
        return animateColorAsState(
            if (item == selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }

    inline fun render(f: FilterSortPopupRenderer.() -> Unit) {
        f()
    }
}

enum class LibraryHeaderTab {
    ARTIST, ALBUM, SONG, SEARCH;
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun LibraryHeader(
    modifier: Modifier,
    library: Library,
    options: SongListOptions,
    setOptions: (SongListOptions) -> Unit,
    tab: LibraryHeaderTab?,
    setTab: (LibraryHeaderTab?) -> Unit,
    showSettingsButton: Boolean,
    openSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val artistRenderer = remember(library, options, setOptions) {
        ArtistOptionsRenderer(ARTIST, library, options, setOptions)
    }
    val albumRenderer = remember(library, options, setOptions) {
        AlbumOptionsRenderer(ALBUM, library, options, setOptions)
    }
    val songRenderer = remember(options, setOptions) {
        SongOptionsRenderer(SONG, options, setOptions)
    }
    val queryTransition = updateTransition(options.queryFilter)

    val searchBarMode = updateTransition(
        when {
            tab == SEARCH -> LibrarySearchBarMode.EXPANDED
            options.queryFilter.isEmpty() -> LibrarySearchBarMode.ICON
            else -> LibrarySearchBarMode.TAG
        }
    )
    Column(modifier) {
        var parentPos by remember { mutableStateOf(Offset(0f, 0f)) }
        var parentSize by remember { mutableStateOf(IntSize(0, 0)) }
        var searchTagPos by remember { mutableStateOf(Offset(0f, 0f)) }
        var searchTagSize by remember { mutableStateOf(IntSize(0, 0)) }
        var searchIconPos by remember { mutableStateOf(Offset(0f, 0f)) }
        Box(
            Modifier.heightIn(min = 64.dp).animateContentHeight().onGloballyPositioned {
                parentPos = it.localToWindow(Offset.Zero)
                parentSize = it.size
            },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    Modifier.padding(2.dp).weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    artistRenderer.Tag(tab, setTab)
                    albumRenderer.Tag(tab, setTab)
                    songRenderer.Tag(tab, setTab)
                    Box(Modifier.onGloballyPositioned {
                        searchTagPos = it.localToWindow(Offset.Zero)
                        searchTagSize = it.size
                    }) {
                        queryTransition.Crossfade(contentKey = { it.isNotEmpty() }) { q ->
                            if (q.isNotEmpty()) {
                                Tag(
                                    tab == null || tab == SEARCH, true,
                                    false, Icons.Default.Search,
                                    "Search", q, null,
                                    { setTab(null);setOptions(options.removeSearch()) },
                                    { setTab(SEARCH) },
                                )
                            }
                        }
                    }
                }
                Box(Modifier.onGloballyPositioned {
                    searchIconPos = it.localToWindow(Offset.Zero)
                }) {
                    queryTransition.AnimatedContent(
                        contentKey = { it.isEmpty() },
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut() using SizeTransform()
                        }) { q ->
                        Box(Modifier.height(48.dp)) {
                            if (q.isEmpty()) {
                                IconButton(
                                    { setTab(SEARCH) }, Modifier.alpha(
                                        if (
                                            searchBarMode.currentState == LibrarySearchBarMode.EXPANDED ||
                                            searchBarMode.targetState == LibrarySearchBarMode.EXPANDED
                                        ) 0f else 1f
                                    )
                                ) {
                                    Icon(Icons.Filled.Search, "Search")
                                }
                            }
                        }
                    }
                }
                val otherButtonState = tab to showSettingsButton
                val secondButtonTransition = updateTransition(otherButtonState)
                secondButtonTransition.AnimatedContent(
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut() using SizeTransform()
                    },
                    contentKey = { (tab, _) -> tab != null },
                ) { (tab, showSettingsButton) ->
                    if (tab != null && tab != SEARCH) {
                        IconButton({ setTab(null) }) {
                            Icon(Icons.Filled.Close, "Close")
                        }
                    } else if (showSettingsButton) {
                        SettingsButton(Modifier, openSettings)
                    }
                }
            }
            LibrarySearchBar(
                mode = searchBarMode,
                collapse = { setTab(null) },
                iconOffset = searchIconPos - parentPos,
                tagOffset = searchTagPos - parentPos,
                tagSize = searchTagSize,
                expandedSize = parentSize,
                library = library,
                query = options.queryFilter,
            ) {
                setOptions(options.copy(queryFilter = it))
            }
        }
        HorizontalDivider()

        AnimatedContent(tab, transitionSpec = {
            if (this.initialState == null || this.targetState == null) {
                fadeIn() togetherWith fadeOut()
            } else {
                val mult = if (this.initialState!! < this.targetState!!) 1 else -1
                slideInHorizontally { mult * it } togetherWith slideOutHorizontally { mult * -it }
            } using SizeTransform(sizeAnimationSpec = { _, _ -> spring(stiffness = Spring.StiffnessLow) })
        }) { t ->
            if (t != null) {
                val renderer = when (t) {
                    ARTIST -> artistRenderer
                    ALBUM -> albumRenderer
                    SONG -> songRenderer
                    SEARCH -> null
                }
                if (renderer != null) {
                    Layout(content = {
                        Box {
                            with(renderer) { Render { setTab(null) } }
                            HorizontalDivider(Modifier.align(Alignment.BottomCenter))
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
            } else {
                Spacer(Modifier.fillMaxWidth())
            }
        }

        val shouldHide = tab != null && tab != SEARCH
        val alpha by animateFloatAsState(if (shouldHide) inactiveAlpha else 1f)
        val blur by animateFloatAsState(if (shouldHide) 2.dp.toPxApprox() else 0f)
        Box(
            Modifier.graphicsLayer {
                this.alpha = alpha
                this.renderEffect = if (blur > 0) {
                    BlurEffect(blur, blur)
                } else null
            }
        ) {
            content()
            if (shouldHide) {
                Box(Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { setTab(null) }
                )
            }
        }
    }
}

private interface TabRenderer {

    @Composable
    fun BoxScope.Render(close: () -> Unit)

}

private interface OptionsRenderer : TabRenderer {
    val description: String
    val icon: ImageVector
    val tab: LibraryHeaderTab
    val activeFilter: Any?
    val withoutFilter: SongListOptions
    val selectedOption: SortFilterOption
    val setOptions: (SongListOptions) -> Unit

    @Composable
    fun Tag(currentTab: LibraryHeaderTab?, setTab: (LibraryHeaderTab?) -> Unit) {
        TagForOptions(
            active = currentTab == null || tab == currentTab,
            selected = selectedOption,
            enabled = activeFilter != null,
            description = description,
            icon = icon,
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
    override val tab: LibraryHeaderTab,
    library: Library,
    private val options: SongListOptions,
    override val setOptions: (SongListOptions) -> Unit,
) : OptionsRenderer {
    override val description = "Artists"
    override val icon = Icons.Default.Groups
    override val activeFilter = options.artistFilter
    override val withoutFilter = options.withArtistFilter(null)
    private val libForArtists = library.sort(
        options.artistSorter.comparator ?: compareBy { it.name },
        noopComparator(), noopComparator()
    )
    private val artistsSorters = ArtistSorter.entries.associateWith { sorter ->
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
    override fun BoxScope.Render(close: () -> Unit) {
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
    override val tab: LibraryHeaderTab,
    library: Library,
    private val options: SongListOptions,
    override val setOptions: (SongListOptions) -> Unit,
) : OptionsRenderer {
    override val description = "Albums"
    override val icon = Icons.Default.Album
    override val activeFilter = options.albumFilter
    override val withoutFilter = options.withAlbumFilter(null)
    private val libForAlbums = library.sort(
        options.artistSorter.comparator.orNoop(),
        options.albumSorter.comparator ?: compareBy { it.title },
        noopComparator()
    ).filter(options.artistFilter, null, "")
    private val albumsSorters = AlbumSorter.entries.associateWith { sorter ->
        SortFilterOption.Sort(sorter) {
            it.withAlbumFilter(null).copy(albumSorter = sorter)
        }
    }
    private val albumsFilters = libForAlbums.albums.associateWith { album ->
        SortFilterOption.Filter(
            album.title, album
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
    override fun BoxScope.Render(close: () -> Unit) {
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
    override val tab: LibraryHeaderTab,
    private val options: SongListOptions,
    override val setOptions: (SongListOptions) -> Unit,
) : OptionsRenderer {
    override val description = "Songs"
    override val icon = Icons.Default.MusicNote
    override val activeFilter = null
    override val withoutFilter = options
    private val isGroupingByAlbum = options.isInAlbumMode
    private val songSorters =
        SongSorter.entries.filter { !it.inAlbumOnly || isGroupingByAlbum }.associateWith { sorter ->
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
    override fun BoxScope.Render(close: () -> Unit) {
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
        SingleLineText(name, style = MaterialTheme.typography.labelLarge)
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
        SingleLineText(text, style = MaterialTheme.typography.bodyMedium)
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
                    item.element.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
                )
                SingleLineText(
                    item.element.artist.name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center
                )
            }
        }
    }
}