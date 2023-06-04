package ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import data.Song
import data.SongListItem

@Composable
fun SongListUI(
    maxTrackNumber: Int?,
    items: List<SongListItem>,
    onSongSelected: (Song) -> Unit,
) {
    Box {
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items) { index, item ->
                val offset = if (index == state.firstVisibleItemIndex) state.firstVisibleItemScrollOffset else 0
                when (item) {
                    is SongListItem.ArtistListItem -> {
                        ArtistRow(maxTrackNumber, item.artist, item.songs, offset, onSongSelected)
                    }

                    is SongListItem.AlbumListItem -> {
                        AlbumRow(maxTrackNumber, item.album, item.songs, offset, onSongSelected)
                    }

                    is SongListItem.SingleSongListItem -> {
                        SongRow(maxTrackNumber, item.song) { onSongSelected(item.song) }
                    }
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = state
            )
        )
    }
}