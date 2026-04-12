package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.mmarco94.tambourine.playerController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

/** The ideal position of the focused item in the screen */
private const val TARGET_POSITION_ON_SCREEN = 0.2f

@Composable
fun SongListUI(
    maxTrackNumber: Int?,
    items: List<SongListItem>,
    state: () -> LazyListState,
    controller: SongQueueController,
    contentPadding: PaddingValues = PaddingValues(bottom = 128.dp),
) {
    val state = state()
    Box {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            val counter = mutableMapOf<Any, Int>()
            val itemsKeyed = items.associateWith { item ->
                val key = when (item) {
                    is SongListItem.AlbumListItem -> item.album.uniqueKey
                    is SongListItem.ArtistListItem -> item.artist.uniqueKey
                    is SongListItem.QueuedSongListItem -> item.song.uniqueKey
                    is SongListItem.SongListItem -> item.song.uniqueKey
                }
                val count = counter.compute(key) { _, oldV ->
                    (oldV ?: 0) + 1
                }
                key to count
            }
            itemsIndexed(items, key = { _, item -> itemsKeyed.getValue(item) }) { index, item ->
                val offset = if (index == state.firstVisibleItemIndex) state.firstVisibleItemScrollOffset else 0
                when (item) {
                    is SongListItem.ArtistListItem -> {
                        ArtistRow(maxTrackNumber, item.artist, item.songs, offset, controller)
                    }

                    is SongListItem.AlbumListItem -> {
                        AlbumRow(maxTrackNumber, item.album, item.songs, offset, controller)
                    }

                    is SongListItem.SongListItem -> {
                        SongRow(
                            Modifier.padding(horizontal = 16.dp),
                            maxTrackNumber,
                            item,
                            showAlbumInfo = true,
                            showArtistInfo = true,
                            controller = controller,
                        )
                    }

                    is SongListItem.QueuedSongListItem -> {
                        SongRow(
                            Modifier.padding(horizontal = 16.dp),
                            maxTrackNumber,
                            item,
                            showAlbumInfo = true,
                            showArtistInfo = true,
                            controller = controller,
                        )
                    }
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberSongListScrollbarAdapter(items, state),
            style = LocalScrollbarStyle.current.copy(
                minimalHeight = 64.dp,
            )
        )
    }
}

@Composable
private fun computeApproximateItemHeights(items: List<SongListItem>): Pair<List<Dp>, List<Dp>> {
    val songHeight = songRowEstimateHeight()
    val songHeightWithInfo = songRowEstimateHeight(showInfo = true)
    val songHeightWithAll = songRowEstimateHeight(showInfo = true, showAlbum = true)

    val itemsHeight = remember(items, songHeight, songHeightWithInfo, songHeightWithAll) {
        items.map { item ->
            when (item) {
                is SongListItem.AlbumListItem -> when (item.songs.size) {
                    // Same as the item height
                    in 0..ALBUM_ROW_SHOW_ALBUM_INFO_THRESHOLD -> songHeightWithInfo * item.songs.size
                    // Max of items, album
                    else -> maxOf(240.dp, songHeight * item.songs.size)
                } + DividerDefaults.Thickness

                is SongListItem.ArtistListItem -> songHeightWithAll * item.songs.size + DividerDefaults.Thickness
                is SongListItem.QueuedSongListItem -> songHeightWithAll
                is SongListItem.SongListItem -> songHeightWithAll
            }
        }
    }
    val itemsOffsets = remember(itemsHeight) {
        itemsHeight.runningFold(0.dp) { a, b -> a + b }
    }
    return itemsHeight to itemsOffsets
}

@Composable
private fun rememberSongListScrollbarAdapter(
    items: List<SongListItem>,
    scrollState: LazyListState,
): ScrollbarAdapter {
    val (itemsHeight, itemsOffsets) = computeApproximateItemHeights(items)
    val density = LocalDensity.current
    // Inspired by LazyLineContentAdapter
    return object : ScrollbarAdapter {
        private fun offsetOf(index: Int): Double {
            with(density) {
                val base = itemsOffsets.getOrNull(index)?.toPx() ?: 0f
                val diff = scrollState.layoutInfo.visibleItemsInfo.sumOf { item ->
                    if (item.index !in itemsHeight.indices) {
                        item.size.toDouble()
                    } else if (item.index < index) {
                        val diff = item.size.toDouble() - itemsHeight[item.index].toPx()
                        if (diff.absoluteValue > 1) {
                            logger.trace { "Element at index ${item.index} has unexpected height! Expecting ${itemsHeight[item.index]}, found ${item.size.toDp()}" }
                        }
                        diff
                    } else 0.0
                }
                return base + diff
            }
        }

        override val scrollOffset: Double
            get() {
                val firstItem = scrollState.layoutInfo.visibleItemsInfo.minByOrNull { it.index }
                return if (firstItem == null) {
                    0.0
                } else {
                    offsetOf(firstItem.index) - firstItem.offset
                }
            }
        override val contentSize: Double
            get() {
                return offsetOf(itemsOffsets.lastIndex) +
                        scrollState.layoutInfo.beforeContentPadding +
                        scrollState.layoutInfo.afterContentPadding
            }
        override val viewportSize: Double
            get() = scrollState.layoutInfo.viewportSize.height.toDouble()

        override suspend fun scrollTo(scrollOffset: Double) {
            val distance = scrollOffset - this.scrollOffset

            // if we scroll less than viewport we need to use scrollBy function to avoid
            // undesirable scroll jumps (when an item size is different)
            //
            // if we scroll more than viewport we should immediately jump to this position
            // without recreating all items between the current and the new position
            if (abs(distance) <= viewportSize) {
                scrollState.scrollBy(distance.toFloat())
            } else {
                snapTo(scrollOffset)
            }
        }

        private suspend fun snapTo(scrollOffset: Double) {
            val scrollOffsetCoerced = scrollOffset.coerceIn(0.0, maxScrollOffset)
            val index = itemsOffsets.indices
                .indexOfLast { offsetOf(it) < scrollOffsetCoerced }
                .coerceIn(items.indices)
            val offset = (scrollOffsetCoerced - offsetOf(index)).roundToInt()
            scrollState.scrollToItem(index, offset)
        }
    }
}

@Composable
fun rememberLazySongListState(
    height: Dp,
    items: List<SongListItem>,
    tryNotToScroll: Boolean,
): LazyListState {
    val ret = remember { mutableStateOf<LazyListState?>(null) }
    rememberInjectedLazySongListState(
        height = height,
        items = items,
        tryNotToScroll = tryNotToScroll,
        state = ret
    )
    return checkNotNull(ret.value)
}

/**
 * A very complicated machine to keep the currently playing item on the screen.
 * @param tryNotToScroll if true, will scroll only if focused item is near the edges of the screen
 * @return this weird interface is because of performances
 */
@Composable
fun rememberInjectedLazySongListState(
    height: Dp,
    items: List<SongListItem>,
    tryNotToScroll: Boolean,
    state: MutableState<in LazyListState>,
) {
    val density = LocalDensity.current
    val songHeight = songRowEstimateHeight()
    val songHeightWithInfo = songRowEstimateHeight(showInfo = true)
    val songHeightWithAll = songRowEstimateHeight(showInfo = true, showAlbum = true)

    val queue = playerController.current.queue
    val currentlyPlayingPosition = queue?.position
    val currentlyPlaying = queue?.currentSongKey

    val playingItem = items.withIndex().firstOrNull { (_, item) ->
        when (item) {
            is SongListItem.AlbumListItem -> item.songs.any { it.uniqueKey == currentlyPlaying }
            is SongListItem.ArtistListItem -> item.songs.any { it.uniqueKey == currentlyPlaying }
            is SongListItem.QueuedSongListItem -> item.indexInQueue == currentlyPlayingPosition
            is SongListItem.SongListItem -> item.song.uniqueKey == currentlyPlaying
        }
    }

    val pos = playingItem?.index ?: 0
    val positionInItem = when (val item = playingItem?.value) {
        is SongListItem.AlbumListItem -> {
            val indexInAlbum = item.songs.indexOfFirst { it.uniqueKey == currentlyPlaying }
            when (item.songs.size) {
                in 0..ALBUM_ROW_SHOW_ALBUM_INFO_THRESHOLD -> songHeightWithInfo * indexInAlbum
                else -> songHeight * indexInAlbum
            }
        }

        is SongListItem.ArtistListItem -> {
            val indexInArtist = item.songs.indexOfFirst { it.uniqueKey == currentlyPlaying }
            songHeightWithAll * indexInArtist
        }

        is SongListItem.QueuedSongListItem -> songHeightWithAll / 2f
        is SongListItem.SongListItem -> songHeightWithAll / 2f
        null -> 0.dp
    }

    val offset = (positionInItem - height * TARGET_POSITION_ON_SCREEN).toPxApprox().roundToInt()
    val listState = rememberLazyListState(pos, offset)
    state.value = listState
    var shouldBeFast by remember { mutableStateOf(false) }

    LaunchedEffect(pos, positionInItem, height) {
        val nearPositionRange = pos - 1..pos + 1
        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index in nearPositionRange }
        val isMovingToFirst =
            pos == 0 && listState.layoutInfo.visibleItemsInfo.any { it.index == listState.layoutInfo.totalItemsCount - 1 }
        val isMovingToLast =
            pos == listState.layoutInfo.totalItemsCount - 1 && listState.layoutInfo.visibleItemsInfo.any { it.index == 0 }
        // Whether the focused element is already "pretty centered" in the screen
        val insideTheScreen = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == pos }?.let { itemInfo ->
            val pos = with(density) { itemInfo.offset.toDp() } + positionInItem
            pos in (height * TARGET_POSITION_ON_SCREEN..height * (1 - TARGET_POSITION_ON_SCREEN))
        } ?: false
        try {
            if (shouldBeFast) {
                shouldBeFast = false
                listState.scrollToItem(pos, offset)
            } else if (!(insideTheScreen && tryNotToScroll) && (isVisible || isMovingToFirst || isMovingToLast)) {
                if (isVisible) {
                    listState.animateScrollToItem(pos, offset)
                } else {
                    listState.scrollToItem(pos, offset)
                }
            }
        } catch (e: CancellationException) {
            shouldBeFast = true
            throw e
        }
    }
}
