package io.github.musicplayer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.musicplayer.audio.Position
import io.github.musicplayer.data.*
import io.github.musicplayer.playerController
import io.github.musicplayer.ui.Panel.*
import kotlinx.coroutines.launch


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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun App(
    library: LibraryState?,
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit
) {
    var listOptions by remember(library as? Library) { mutableStateOf(SongListOptions()) }
    var openSettings by remember { mutableStateOf(false) }
    val player = playerController.current
    val mainImage = player.queue?.currentSong?.cover ?: (library as? Library)?.songs?.random()?.cover

    MaterialTheme(
        typography = MusicPlayerTheme.typography,
        colorScheme = MusicPlayerTheme.colors(mainImage),
        shapes = MusicPlayerTheme.shapes,
    ) {
        CompositionLocalProvider(
            LocalScrollbarStyle provides defaultScrollbarStyle().copy(
                thickness = 12.dp,
                shape = RoundedCornerShape(6.dp),
                unhoverColor = Color.White.copy(alpha = .12f),
                hoverColor = Color.White.copy(alpha = .5f),
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BlurredFadeAlbumCover(mainImage, Modifier.fillMaxSize())
                DelayDraw { shouldRenderQuickly ->
                    val transition = updateTransition(Triple(shouldRenderQuickly, library, player.queue))
                    transition.Crossfade(contentKey = { (srq, lib, q) ->
                        srq || (lib !is Library && q == null)
                    }) { (srq, lib, q) ->
                        if (srq || (lib !is Library && q == null)) {
                            LoadingLibraryComposable(lib) { openSettings = true }
                        } else {
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
                                        visiblePanels,
                                        lib,
                                        listOptions,
                                        { listOptions = it }) {
                                        openSettings = true
                                    }
                                    Divider()
                                    BottomBar(large, selectedPanel, visiblePanels, selectPanel)
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
    visiblePanels: List<Panel>,
    library: LibraryState?,
    listOptions: SongListOptions,
    setListOptions: (SongListOptions) -> Unit,
    openSettings: () -> Unit,
) {
    val cs = rememberCoroutineScope()
    val player = playerController.current
    PanelContainer(modifier, values().toSet(), visiblePanels) { panel ->
        when (panel) {
            LIBRARY -> LibraryContainer(library, openSettings) { library ->
                val lib by derivedStateOf {
                    library.filterAndSort(listOptions)
                }
                val items by derivedStateOf {
                    lib.toListItems(listOptions)
                }
                LibraryHeader(Modifier.fillMaxSize(), library, listOptions, setListOptions, openSettings) {
                    SongListUI(lib.stats.maxTrackNumber, items) { song ->
                        cs.launch {
                            player.changeQueue(SongQueue.of(lib, song), Position.Beginning)
                            player.play()
                        }
                    }
                }
            }

            QUEUE -> SongQueueUI(Modifier.fillMaxSize()) { queue ->
                cs.launch {
                    player.changeQueue(queue, Position.Beginning)
                    player.play()
                }
            }

            PLAYER -> PlayerUI(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BottomBar(
    large: Boolean,
    selectedPanel: Panel,
    visiblePanels: List<Panel>,
    selectPanel: (Panel) -> Unit,
) {
    val doFancy = large && selectedPanel != PLAYER
    Row(Modifier.height(IntrinsicSize.Max)) {
        values().forEach { panel ->
            key(panel) {
                val isSelected = panel in visiblePanels
                val alpha by animateFloatAsState(if (isSelected) 1f else inactiveAlpha)
                val bg by animateColorAsState(Color.Black.copy(alpha = if (panel != PLAYER && doFancy) 0.3f else 0.2f))
                val weight by animateFloatAsState(if (panel == PLAYER && doFancy) 2f else 1f)
                Column(
                    Modifier
                        .weight(weight)
                        .background(bg)
                        .fillMaxHeight()
                        .clickable {
                            selectPanel(if (large && selectedPanel == panel) PLAYER else panel)
                        }
                        .alpha(alpha)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(panel.icon, null)
                    SingleLineText(
                        panel.label,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryContainer(library: LibraryState?, openSettings: () -> Unit, f: @Composable (Library) -> Unit) {
    if (library !is Library) {
        LoadingLibraryComposable(library, openSettings)
    } else {
        f(library)
    }
}

@Composable
private fun LoadingLibraryComposable(library: LibraryState?, openSettings: () -> Unit) {
    Box {
        BigMessage(
            Modifier.fillMaxSize(),
            Icons.Default.LibraryMusic,
            "Loading...",
        ) {
            if (library is Library.Companion.LoadingProgress) {
                SingleLineText("${library.loaded} songs loaded", style = MaterialTheme.typography.labelMedium)
            }
        }
        IconButton(openSettings, Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Default.Settings, "Settings")
        }
    }
}
