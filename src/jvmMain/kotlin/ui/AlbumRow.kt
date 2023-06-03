package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import audio.Album
import audio.Song

@Composable
fun AlbumRow(album: Album, songs: List<Song>, onSongSelected: (Song) -> Unit) {
    val cover = songs.firstOrNull { it.cover != null }?.cover

    Box {
//        if (cover != null) {
//            Image(
//                cover,
//                null,
//                alpha = .3f,
//                modifier = Modifier.matchParentSize().blur(64.dp),
//                contentScale = ContentScale.Crop,
//                filterQuality = FilterQuality.High,
//            )
//        }
        Row(Modifier.padding(vertical = 24.dp, horizontal = 16.dp)) {
            key(album) {
                AlbumCover(cover, 128.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(album.title, style = MaterialTheme.typography.h4)
                songs.forEach { song ->
                    SongRow(song, inAlbumContext = true, onSongSelected = { onSongSelected(song) })
                }
            }
        }
    }
}