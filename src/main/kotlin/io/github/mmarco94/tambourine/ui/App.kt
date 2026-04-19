package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.LocalAppearanceSettings
import io.github.mmarco94.tambourine.data.*
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.ui.LibraryUIState.*
import io.github.mmarco94.tambourine.ui.Panel.*
import io.github.mmarco94.tambourine.utils.HSLColor
import io.github.mmarco94.tambourine.utils.hsb
import io.github.mmarco94.tambourine.utils.throttle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

enum class Panel(
    val icon: ImageVector,
    val label: StringResource,
) {
    LIBRARY(Icons.Filled.Home, Res.string.main_menu_library),
    QUEUE(Icons.AutoMirrored.Default.QueueMusic, Res.string.main_menu_queue),
    PLAYER(Icons.Filled.PlayCircleFilled, Res.string.main_menu_player),
}

@Composable
fun App(
    library: Library?,
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    libraryTab: LibraryHeaderTab?,
    selectLibraryTab: (LibraryHeaderTab?) -> Unit,
) {
    var rawListOptions by remember { mutableStateOf(SongListOptions()) }
    val listOptions = rawListOptions.adjust(library)
    val player = playerController.current
    val accentColor = LocalAppearanceSettings.current.accentColor
    val mainImage by remember(library, accentColor) {
        snapshotFlow {
            player.queue?.currentSong?.cover ?: library?.findCoverByColor(accentColor)
        }.throttle(100.milliseconds)
    }.collectAsState(initial = null)

    MaterialTheme(
        typography = TambourineTheme.typography,
        colorScheme = mainImage?.colorScheme?.auto() ?: MaterialTheme.colorScheme,
        shapes = TambourineTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                thickness = 12.dp,
                shape = RoundedCornerShape(6.dp),
                unhoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .12f),
                hoverColor = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f),
            ),
            LocalContextMenuRepresentation provides MenuContextRepresentation,
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                AlbumCoverBackground(mainImage, Modifier.fillMaxSize())
                val libUIState by derivedStateOf {
                    if (player.queue != null) NORMAL else library.toUIState()
                }
                LibraryContainer(
                    state = libUIState,
                    library = library,
                    openSettings = openSettings,
                    closeApp = closeApp,
                ) { lib ->
                    BoxWithConstraints {
                        val w = constraints.maxWidth
                        val large = w >= (BIG_SONG_ROW_DESIRED_WIDTH * 2).toPxApprox()
                        val visiblePanels = if (large) {
                            listOf(selectedPanel, PLAYER).distinct()
                        } else {
                            listOf(selectedPanel)
                        }
                        Column {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) {
                                MainContent(
                                    modifier = Modifier.fillMaxSize(),
                                    large = large,
                                    height = maxHeight,
                                    selectedPanel = selectedPanel,
                                    visiblePanels = visiblePanels,
                                    selectPanel = selectPanel,
                                    library = lib,
                                    listOptions = listOptions,
                                    setListOptions = { rawListOptions = it },
                                    openSettings = openSettings,
                                    closeApp = closeApp,
                                    libraryTab = libraryTab,
                                    selectLibraryTab = selectLibraryTab,
                                )
                            }
                            if (!large) {
                                HorizontalDivider()
                                BottomBar(selectedPanel, selectPanel)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Library.findCoverByColor(color: Triple<Double, Double, Double>?): AlbumCover? {
    return if (color == null) {
        songs.firstOrNull()?.cover
    } else {
        val accentColor = HSLColor.fromRgb(color)
        songs
            .mapNotNullTo(mutableSetOf()) { it.cover }
            .minByOrNull {
                val color = it.colorPalette.first().hsb()
                // Multiplied by how important each component is
                5 * color.hueDistance(accentColor) / 180f +
                        (color.lightness - accentColor.lightness).absoluteValue +
                        (color.saturation - accentColor.saturation).absoluteValue
            }
    }
}

data class LibraryExtended(
    val library: Library,
    val sortedLib: Library,
    val filteredLib: Library,
    val listItems: List<SongListItem>,
)

@Composable
private fun MainContent(
    modifier: Modifier,
    large: Boolean,
    height: Dp,
    selectedPanel: Panel,
    visiblePanels: List<Panel>,
    selectPanel: (Panel) -> Unit,
    library: Library?,
    listOptions: SongListOptions,
    setListOptions: (SongListOptions) -> Unit,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    libraryTab: LibraryHeaderTab?,
    selectLibraryTab: (LibraryHeaderTab?) -> Unit,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    val libraryExtended = remember(library, listOptions) {
        library?.let {
            val sortedLib = library.sort(listOptions)
            val filteredLib = sortedLib.filter(listOptions)
            LibraryExtended(
                library = library,
                sortedLib = sortedLib,
                filteredLib = filteredLib,
                listItems = filteredLib.toListItems(listOptions),
            )
        }

    }

    // This is done like this to avoid recompositions when the song changes
    val libraryScrollState = remember { mutableStateOf<LazyListState?>(null) }
    rememberInjectedLazySongListState(
        height,
        libraryExtended?.listItems.orEmpty(),
        tryNotToScroll = true,
        libraryScrollState
    )

    var showLyrics by remember { mutableStateOf(true) }
    PanelContainer(modifier, Panel.entries.toSet(), visiblePanels) { panel ->
        val showToolbar = !large || panel == PLAYER
        when (panel) {
            LIBRARY -> LibraryContainer(
                library = libraryExtended,
                openSettings = openSettings,
                closeApp = closeApp,
            ) { libraryExtended ->
                val controller = SongQueueController(
                    cs,
                    libraryExtended.sortedLib,
                    libraryExtended.filteredLib.songs.map { it.uniqueKey },
                    player
                ) {
                    selectLibraryTab(null)
                }
                LibraryHeader(
                    modifier = Modifier.fillMaxSize(),
                    library = libraryExtended.library,
                    options = listOptions,
                    setOptions = setListOptions,
                    tab = libraryTab,
                    setTab = selectLibraryTab,
                    showToolbar = showToolbar,
                    openSettings = openSettings,
                    closeApp = closeApp,
                ) {
                    Crossfade(listOptions.queryFilter.isNotEmpty() && libraryExtended.filteredLib.songs.isEmpty()) {
                        if (it) {
                            LibraryNoSearchResultsComposable { setListOptions(listOptions.removeSearch()) }
                        } else {
                            SongListUI(
                                libraryExtended.filteredLib.stats.maxTrackNumber,
                                libraryExtended.listItems,
                                { checkNotNull(libraryScrollState.value) },
                                controller
                            )
                        }
                    }
                }
            }

            QUEUE ->
                LibraryContainer(
                    state = libraryExtended?.library.toUIState(false),
                    library = libraryExtended,
                    openSettings = openSettings,
                    closeApp = closeApp,
                ) { libraryExtended ->
                    libraryExtended as LibraryExtended
                    SongQueueUI(
                        sortedLibrary = libraryExtended.sortedLib,
                        showToolbar = showToolbar,
                        openSettings = openSettings,
                        closeApp = closeApp,
                    )
                }

            PLAYER -> {
                WindowDraggableArea {
                    PlayerUI(
                        showToolbar = showToolbar,
                        showLyrics = showLyrics,
                        openSettings = openSettings,
                        setShowLyrics = { showLyrics = it },
                        closeApp = closeApp,
                    )
                    if (large) {
                        RailBar(selectedPanel, selectPanel)
                    }
                }
            }
        }
    }
}

@Composable
private fun RailBar(
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
) {
    NavigationRail(
        Modifier.padding(vertical = 8.dp),
        containerColor = Color.Transparent,
    ) {
        Panel.entries.forEach { panel ->
            if (panel != PLAYER) {
                NavigationRailItem(
                    panel == selectedPanel,
                    onClick = { if (panel == selectedPanel) selectPanel(PLAYER) else selectPanel(panel) },
                    icon = { Icon(panel.icon, null) },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
) {
    NavigationBar {
        Panel.entries.forEach { panel ->
            NavigationBarItem(
                panel == selectedPanel,
                onClick = { selectPanel(panel) },
                icon = { Icon(panel.icon, null) },
                alwaysShowLabel = false,
                label = {
                    SingleLineText(
                        stringResource(panel.label),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

private enum class LibraryUIState {
    EMPTY, LOADING, NORMAL
}

private fun Library?.toUIState(handleEmpty: Boolean = true): LibraryUIState {
    return when {
        this == null -> LOADING
        handleEmpty && songs.isEmpty() -> EMPTY
        else -> NORMAL
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun <T> LibraryContainer(
    state: LibraryUIState,
    library: T?,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    f: @Composable (T?) -> Unit
) {
    val transition = updateTransition(state to library)
    transition.Crossfade(contentKey = { it.first }) { (state, lib) ->
        when (state) {
            EMPTY -> LibraryEmptyComposable(openSettings, closeApp)
            LOADING -> LoadingLibraryComposable(openSettings, closeApp)
            NORMAL -> f(lib)
        }
    }
}

@Composable
private fun LibraryContainer(
    library: LibraryExtended?,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    f: @Composable (LibraryExtended) -> Unit
) {
    LibraryContainer(
        state = library?.library.toUIState(),
        library = library,
        openSettings = openSettings,
        closeApp = closeApp,
    ) { lib ->
        f(lib as LibraryExtended)
    }
}

@Composable
private fun LoadingLibraryComposable(openSettings: () -> Unit, closeApp: () -> Unit) {
    WindowDraggableArea {
        Column {
            AppToolbar(openSettings = openSettings, closeApp = closeApp)
            BigMessage(
                Modifier.fillMaxSize(),
                Icons.Default.LibraryMusic,
                stringResource(Res.string.loading___),
            ) {}
        }
    }
}

@Composable
private fun LibraryEmptyComposable(openSettings: () -> Unit, closeApp: () -> Unit) {
    WindowDraggableArea {
        Column {
            AppToolbar(openSettings = openSettings, closeApp = closeApp)
            BigMessage(
                Modifier.fillMaxSize(),
                Icons.Default.LibraryMusic,
                stringResource(Res.string.empty_library),
            ) {
                Button(openSettings) {
                    Text(stringResource(Res.string.action_open_settings))
                }
            }
        }
    }
}

@Composable
private fun LibraryNoSearchResultsComposable(clearSearch: () -> Unit) {
    WindowDraggableArea {
        BigMessage(
            Modifier.fillMaxSize(),
            Icons.Default.SearchOff,
            stringResource(Res.string.no_search_results),
        ) {
            Button(clearSearch) {
                Text(stringResource(Res.string.action_remove_filter))
            }
        }
    }
}
