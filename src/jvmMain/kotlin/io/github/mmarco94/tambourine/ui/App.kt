package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.*
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.ui.LibraryUIState.*
import io.github.mmarco94.tambourine.ui.Panel.*


enum class Panel(
    val icon: ImageVector,
    val label: String,
) {
    LIBRARY(Icons.Filled.LibraryMusic, "Library"),
    QUEUE(Icons.Filled.QueueMusic, "Queue"),
    PLAYER(Icons.Filled.PlayCircleFilled, "Player"),
}

/**
 * If the first frames of the app are slow to draw, the windows is a bit buggy.
 * Use this function to know whether the composable should be fast to render
 */
@Composable
private fun DelayDraw(f: @Composable (shouldRenderQuickly: Boolean) -> Unit) {
    var drawCount by remember { mutableStateOf(0) }
    val shouldRenderQuickly = drawCount < 3
    if (shouldRenderQuickly) {
        Canvas(Modifier) {
            drawCount++
        }
    }
    f(shouldRenderQuickly)
}

@Composable
fun App(
    library: Library?,
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
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
                BlurredFadeAlbumCover(mainImage, Modifier.fillMaxSize())
                DelayDraw { shouldRenderQuickly ->
                    var libUIState = if (player.queue != null) NORMAL else library.toUIState()
                    if (shouldRenderQuickly && libUIState == NORMAL) libUIState = LOADING
                    LibraryContainer(
                        libUIState,
                        library,
                        { openSettings = true },
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
                                    Modifier.fillMaxWidth().weight(1f),
                                    large,
                                    selectedPanel,
                                    visiblePanels,
                                    selectPanel,
                                    lib,
                                    listOptions,
                                    { listOptions = it },
                                    { openSettings = true },
                                    libraryTab,
                                    selectLibraryTab,
                                )
                                if (!large) {
                                    Divider()
                                    BottomBar(selectedPanel, selectPanel)
                                }
                            }
                        }
                    }
                }
                if (openSettings) {
                    LibrarySettings { openSettings = false }
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
    libraryTab: LibraryHeaderTab?,
    selectLibraryTab: (LibraryHeaderTab?) -> Unit,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    val libraryScrollState = rememberLazyListState()
    PanelContainer(modifier, Panel.values().toSet(), visiblePanels) { panel ->
        val showSettings = !large || panel == PLAYER
        when (panel) {
            LIBRARY -> LibraryContainer(
                library,
                openSettings
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
                    Modifier.fillMaxSize(),
                    library,
                    listOptions,
                    setListOptions,
                    libraryTab,
                    selectLibraryTab,
                    showSettings,
                    openSettings,
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
                LibraryContainer(library.toUIState(false), library, openSettings) { library ->
                    val sortedLib = remember(library, listOptions) {
                        (library as Library).sort(listOptions)
                    }
                    SongQueueUI(Modifier.fillMaxSize(), sortedLib, showSettings, openSettings)
                }

            PLAYER -> {
                Box(Modifier.fillMaxSize()) {
                    PlayerUI(Modifier.fillMaxSize(), showSettings, openSettings)
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
        Panel.values().forEach { panel ->
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
        Panel.values().forEach { panel ->
            NavigationBarItem(
                panel == selectedPanel,
                onClick = { selectPanel(panel) },
                icon = { Icon(panel.icon, null) },
                alwaysShowLabel = false,
                label = {
                    SingleLineText(
                        panel.label,
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
    f: @Composable (Library?) -> Unit
) {
    val transition = updateTransition(state to library)
    transition.Crossfade(contentKey = { it.first }) { (state, lib) ->
        when (state) {
            EMPTY -> LibraryEmptyComposable(openSettings)
            LOADING -> LoadingLibraryComposable(openSettings)
            NORMAL -> f(lib)
        }
    }
}

@Composable
private fun LibraryContainer(
    library: Library?,
    openSettings: () -> Unit,
    f: @Composable (Library) -> Unit
) {
    LibraryContainer(library.toUIState(), library, openSettings) { lib ->
        f(lib as Library)
    }
}

@Composable
private fun LoadingLibraryComposable(openSettings: () -> Unit) {
    Box {
        BigMessage(
            Modifier.fillMaxSize(),
            Icons.Default.LibraryMusic,
            "Loading...",
        ) {}
        SettingsButton(Modifier.align(Alignment.TopEnd), openSettings)
    }
}

@Composable
private fun LibraryEmptyComposable(openSettings: () -> Unit) {
    BigMessage(
        Modifier.fillMaxSize(),
        Icons.Default.LibraryMusic,
        "Library is empty",
    ) {
        Button(openSettings) {
            Text("Open settings")
        }
    }
}

@Composable
private fun LibraryNoSearchResultsComposable(clearSearch: () -> Unit) {
    BigMessage(
        Modifier.fillMaxSize(),
        Icons.Default.SearchOff,
        "No search result",
    ) {
        Button(clearSearch) {
            Text("Remove filter")
        }
    }
}
