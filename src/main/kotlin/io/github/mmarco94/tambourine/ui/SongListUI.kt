package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

@Composable
fun SongListUI(
    maxTrackNumber: Int?,
    items: List<SongListItem>,
    state: LazyListState,
    controller: SongQueueController,
    contentPadding: PaddingValues = PaddingValues(bottom = 128.dp),
) {
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
private fun rememberSongListScrollbarAdapter(
    items: List<SongListItem>,
    scrollState: LazyListState,
): ScrollbarAdapter {
    val songHeight = songRowEstimateHeight()
    val songHeightWithInfo = songRowEstimateHeight(showInfo = true)
    val songHeightWithAll = songRowEstimateHeight(showInfo = true, showAlbum = true)

    val itemsHeight = remember(items, songHeight, songHeightWithInfo, songHeightWithAll) {
        items.map { item ->
            when (item) {
                is SongListItem.AlbumListItem -> when (item.songs.size) {
                    // Same as the item height
                    in 0..3 -> songHeightWithInfo * item.songs.size
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
