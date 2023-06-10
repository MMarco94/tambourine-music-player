package io.github.musicplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.Song
import io.github.musicplayer.utils.digits
import io.github.musicplayer.utils.format

@Composable
fun SongRow(
    maxTrackNumber: Int?,
    song: Song,
    inAlbumContext: Boolean = false,
    showAlbum: Boolean = !inAlbumContext,
    onSongSelected: () -> Unit,
) {
    Surface(
        modifier = Modifier.padding(end = 4.dp), // Space for scrollbar
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
    ) {
        Row(
            Modifier.clickable { onSongSelected() }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inAlbumContext) { // Track number
                val maxDigits = maxTrackNumber?.digits() ?: 0
                SingleLineText(
                    song.track?.toString().orEmpty().padStart(maxDigits, ' '),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurfaceSecondary,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (showAlbum) {
                AlbumCover(song.cover, Modifier.size(40.dp), MaterialTheme.shapes.small)
                Spacer(Modifier.width(8.dp))
            }
            Text(song.title,Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            SingleLineText(
                song.length.format(),
                color = MaterialTheme.colors.onSurfaceSecondary,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }
}