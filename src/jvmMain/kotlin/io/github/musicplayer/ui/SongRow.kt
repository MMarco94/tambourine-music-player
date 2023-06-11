package io.github.musicplayer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import io.github.musicplayer.playerController
import io.github.musicplayer.utils.digits
import io.github.musicplayer.utils.format

@Composable
fun SongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    song: Song,
    showTrackNumber: Boolean = false,
    showAlbumInfo: Boolean = true,
    showArtistInfo: Boolean = true,
    onSongSelected: () -> Unit,
) {
    val player = playerController.current
    val isCurrentSong = player.queue?.currentSong == song
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
    ) {
        Row(
            Modifier.clickable { onSongSelected() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            if (showTrackNumber) {
                val maxDigits = maxTrackNumber?.digits() ?: 0
                SingleLineText(
                    song.track?.toString().orEmpty().padStart(maxDigits, ' '),
                    Modifier.padding(vertical = 8.dp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colors.onSurfaceSecondary,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (showAlbumInfo) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.padding(vertical = 8.dp)) {
                    AlbumCover(
                        song.cover,
                        Modifier.size(48.dp),
                        MaterialTheme.shapes.small,
                        elevation = 4.dp,
                        overlay = {
                            if (isCurrentSong) {
                                Box(
                                    Modifier.background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SmallSpectrometers(
                                        Modifier.fillMaxSize().padding(10.dp),
                                        player.frequencyAnalyzer.fadedALittleFrequency
                                    )
                                }
                            }
                        }
                    )
                }
                Spacer(Modifier.width(16.dp))
            }
            Box(Modifier.animateContentSize()) {
                if (isCurrentSong && !showAlbumInfo) {
                    Row {
                        SmallSpectrometers(
                            Modifier.size(24.dp),
                            player.frequencyAnalyzer.fadedALittleFrequency
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(song.title)
                if (showAlbumInfo || showArtistInfo) {
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val style = MaterialTheme.typography.subtitle2
                    if (showAlbumInfo) {
                        SingleLineText(song.album.title, style = style)
                    }
                    if (showAlbumInfo && showArtistInfo) {
                        Text("•", Modifier.padding(horizontal = 8.dp))
                    }
                    if (showArtistInfo) {
                        SingleLineText(song.artist.name, style = style)
                    }
                }
            }
            SingleLineText(
                song.length.format(),
                Modifier.padding(8.dp),
                color = MaterialTheme.colors.onSurfaceSecondary,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }
}