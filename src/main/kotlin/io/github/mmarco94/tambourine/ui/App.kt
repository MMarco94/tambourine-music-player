package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.*
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.ui.LibraryUIState.*
import io.github.mmarco94.tambourine.ui.Panel.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class Panel(
    val icon: ImageVector,
    val label: StringResource,
) {
    LIBRARY(Icons.Filled.LibraryMusic, Res.string.main_menu_library),
    QUEUE(Icons.AutoMirrored.Default.QueueMusic, Res.string.main_menu_queue),
    PLAYER(Icons.Filled.PlayCircleFilled, Res.string.main_menu_player),
}

@Composable
fun App(
    library: Library?,
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
    closeApp: () -> Unit,
    libraryTab: LibraryHeaderTab?,
    selectLibraryTab: (LibraryHeaderTab?) -> Unit,
) {
    var listOptions by remember(library) { mutableStateOf(SongListOptions()) }
    var openSettings by remember { mutableStateOf(false) }
    val player = playerController.current
    val mainImage = player.queue?.currentSong?.cover ?: library?.songs?.firstOrNull()?.cover

    MaterialTheme(
        typography = MusicPlayerTheme.typography,
        colorScheme = mainImage?.colorScheme ?: MusicPlayerTheme.defaultScheme,
        shapes = MusicPlayerTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                thickness = 12.dp,
                shape = RoundedCornerShape(6.dp),
                unhoverColor = Color.White.copy(alpha = .12f),
                hoverColor = Color.White.copy(alpha = .5f),
            ),
            LocalContextMenuRepresentation provides MenuContextRepresentation,
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BlurredFadeAlbumCover(mainImage, Modifier.fillMaxSize().paperNoise())
                val libUIState = if (player.queue != null) NORMAL else library.toUIState()
                LibraryContainer(
                    state = libUIState,
                    library = library,
                    openSettings = { openSettings = true },
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
                            MainContent(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                large = large,
                                selectedPanel = selectedPanel,
                                visiblePanels = visiblePanels,
                                selectPanel = selectPanel,
                                library = lib,
                                listOptions = listOptions,
                                setListOptions = { listOptions = it },
                                openSettings = { openSettings = true },
                                closeApp = closeApp,
                                libraryTab = libraryTab,
                                selectLibraryTab = selectLibraryTab,
                            )
                            if (!large) {
                                HorizontalDivider()
                                BottomBar(selectedPanel, selectPanel)
                            }
                        }
                    }
                }
                if (openSettings) {
                    AppSettingsWindow { openSettings = false }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    large: Boolean,
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
    val libraryScrollState = rememberLazyListState()
    var showLyrics by remember { mutableStateOf(true) }
    PanelContainer(modifier, Panel.entries.toSet(), visiblePanels) { panel ->
        val showToolbar = !large || panel == PLAYER
        when (panel) {
            LIBRARY -> LibraryContainer(
                library = library,
                openSettings = openSettings,
                closeApp = closeApp,
            ) { library ->
                val sortedLib = remember(library, listOptions) {
                    library.sort(listOptions)
                }
                val lib = remember(sortedLib, listOptions) {
                    sortedLib.filter(listOptions)
                }
                val items = remember(lib, listOptions) {
                    lib.toListItems(listOptions)
                }
                val controller = SongQueueController(cs, lib.songs, sortedLib, player, onAction = {
                    selectLibraryTab(null)
                })
                LibraryHeader(
                    modifier = Modifier.fillMaxSize(),
                    library = library,
                    options = listOptions,
                    setOptions = setListOptions,
                    tab = libraryTab,
                    setTab = selectLibraryTab,
                    showToolbar = showToolbar,
                    openSettings = openSettings,
                    closeApp = closeApp,
                ) {
                    Crossfade(listOptions.queryFilter.isNotEmpty() && lib.songs.isEmpty()) {
                        if (it) {
                            LibraryNoSearchResultsComposable { setListOptions(listOptions.removeSearch()) }
                        } else {
                            SongListUI(lib.stats.maxTrackNumber, items, libraryScrollState, controller)
                        }
                    }
                }
            }

            QUEUE ->
                LibraryContainer(
                    state = library.toUIState(false),
                    library = library,
                    openSettings = openSettings,
                    closeApp = closeApp,
                ) { library ->
                    val sortedLib = remember(library, listOptions) {
                        (library as Library).sort(listOptions)
                    }
                    SongQueueUI(
                        sortedLibrary = sortedLib,
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
private fun LibraryContainer(
    state: LibraryUIState,
    library: Library?,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    f: @Composable (Library?) -> Unit
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
    library: Library?,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    f: @Composable (Library) -> Unit
) {
    LibraryContainer(
        state = library.toUIState(),
        library = library,
        openSettings = openSettings,
        closeApp = closeApp,
    ) { lib ->
        f(lib as Library)
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
