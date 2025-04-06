package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController

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
            itemsIndexed(items) { index, item ->
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
            adapter = rememberScrollbarAdapter(
                scrollState = state
            ),
            style = LocalScrollbarStyle.current.copy(
                minimalHeight = 64.dp,
            )
        )
    }
}