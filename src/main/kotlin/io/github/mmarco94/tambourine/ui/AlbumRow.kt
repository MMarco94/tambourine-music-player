package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.github.mmarco94.tambourine.data.Album
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongQueueController

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlbumRow(
    maxTrackNumber: Int?,
    album: Album, songs: List<Song>,
    sideOffset: Int,
    controller: SongQueueController,
) {
    val showAlbumInfo = songs.size > 3
    val showAlbumStats = songs.size > 6
    val estimatedHeightWithInfo = 60.dp * songs.size
    val albumSize = when (songs.size) {
        1 -> 48.dp
        2 -> 96.dp
        else -> 128.dp
    }
    val padding = if (showAlbumInfo) {
        16.dp
    } else {
        (estimatedHeightWithInfo - albumSize) / 2
    }
    BigSongRow(
        maxTrackNumber,
        songs = songs,
        sideOffset = sideOffset,
        controller = controller,
        showTrackNumber = true,
        showAlbumInfo = !showAlbumInfo,
        showArtistInfo = !showAlbumInfo,
        sidePanelPadding = padding,
        showAlbumCover = false,
    ) {
        Box(Modifier.width(128.dp), contentAlignment = Alignment.CenterEnd) {
            AlbumCover(album.cover, Modifier.size(albumSize), MaterialTheme.shapes.medium, elevation = 8.dp) {
                if (songs.size > 1) {
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
            }
        }
        if (showAlbumInfo) {
            Spacer(Modifier.height(8.dp))
            Text(album.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            Text(album.artist.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall)
        }
        if (showAlbumStats) {
            Spacer(Modifier.height(8.dp))
            SongCollectionStatsComposable(album.stats)
        } else if (showAlbumInfo) {
            SongCollectionStatsComposable(album.stats, yearOnly = true)
        }
    }
}