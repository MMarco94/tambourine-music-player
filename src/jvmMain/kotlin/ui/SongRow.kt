package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import audio.Song

@Composable
fun SongRow(
    song: Song,
    inAlbumContext: Boolean = false,
    onSongSelected: () -> Unit,
) {
    Row(Modifier.fillMaxWidth()
        .clickable {
            onSongSelected()
            //TODO song.play()
        }
        .padding(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        if (inAlbumContext) {
            // Track number
            Text(
                song.track?.toString().orEmpty().padStart(3, ' '),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
            )
        } else {
            // Album cover
            key(song) {
                AlbumCover(song.cover, 48.dp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(song.title)
    }
}