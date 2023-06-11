package io.github.musicplayer.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.Song
import io.github.musicplayer.data.SongListItem

@Composable
fun SongListUI(
    maxTrackNumber: Int?,
    items: List<SongListItem>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(bottom = 128.dp),
    onSongSelected: (Song) -> Unit,
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
                        ArtistRow(maxTrackNumber, item.artist, item.songs, offset, onSongSelected)
                    }

                    is SongListItem.AlbumListItem -> {
                        AlbumRow(maxTrackNumber, item.album, item.songs, offset, onSongSelected)
                    }

                    is SongListItem.SingleSongListItem -> {
                        SongRow(
                            Modifier.padding(horizontal = 16.dp),
                            maxTrackNumber,
                            item.song,
                            showAlbumInfo = true,
                            showArtistInfo = true
                        ) { onSongSelected(item.song) }
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