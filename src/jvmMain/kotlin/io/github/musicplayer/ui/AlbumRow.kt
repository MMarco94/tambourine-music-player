package io.github.musicplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.Album
import io.github.musicplayer.data.Song

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlbumRow(
    maxTrackNumber: Int?,
    album: Album, songs: List<Song>, sideOffset: Int, onSongSelected: (Song) -> Unit,
) {
    BigSongRow(maxTrackNumber, true, songs, sideOffset, onSongSelected) {
        AlbumCover(album.cover, Modifier.size(128.dp), MaterialTheme.shapes.medium, elevation = 8.dp) {
            var mouseOver by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxSize()
                    .onPointerEvent(PointerEventType.Enter, PointerEventPass.Initial) { mouseOver = true }
                    .onPointerEvent(PointerEventType.Exit, PointerEventPass.Initial) { mouseOver = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                val alpha by animateFloatAsState(if (mouseOver) 1f else .5f)
                PlayShuffleButtons(Modifier.alpha(alpha), songs)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.subtitle1)
        Text(album.artist.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(8.dp))
        SongCollectionStatsComposable(album.stats)
    }
}