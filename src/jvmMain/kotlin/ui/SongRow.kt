package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import data.Song
import onSurfaceSecondary

@Composable
fun SongRow(
    song: Song,
    inAlbumContext: Boolean = false,
    onSongSelected: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable {
                onSongSelected()
                //TODO song.play()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        if (!inAlbumContext) {
            // Album cover
            key(song) {
                AlbumCover(song.cover, 40.dp, MaterialTheme.shapes.small)
                Spacer(Modifier.width(8.dp))
            }
        }
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (inAlbumContext) { // Track number
                Text(
                    song.track?.toString().orEmpty().padStart(3, ' '),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurfaceSecondary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(song.title)
        }
    }
}