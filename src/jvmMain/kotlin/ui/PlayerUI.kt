package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.Song

@Composable
fun PlayerUI(song: Song?) {
    if (song == null) {
        Text("Select a song")
    } else {
        Box {
            //BlurredAlbumCover(song.cover)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))

                AlbumCover(song.cover, Modifier.size(256.dp), MaterialTheme.shapes.large)
                Spacer(Modifier.height(16.dp))

                Text(song.title, style = MaterialTheme.typography.h2)
                Spacer(Modifier.height(8.dp))
                Text(song.album.title, style = MaterialTheme.typography.h3)
                Text(song.album.artist.name, style = MaterialTheme.typography.h3)

                Spacer(Modifier.weight(1f))
            }
        }
    }
}