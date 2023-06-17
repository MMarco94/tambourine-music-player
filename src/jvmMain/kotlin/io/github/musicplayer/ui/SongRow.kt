package io.github.musicplayer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    showAlbumCover: Boolean = showAlbumInfo,
    onSongSelected: () -> Unit,
) {
    val player = playerController.current
    val isCurrentSong = player.queue?.currentSong == song
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
    ) {
        Row(
            Modifier
                .clickable { onSongSelected() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            if (showTrackNumber) {
                val maxDigits = maxTrackNumber?.digits() ?: 0
                SingleLineText(
                    song.track?.toString().orEmpty().padStart(maxDigits, ' '),
                    Modifier.padding(vertical = 8.dp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(8.dp))
            }
            if (showAlbumCover) {
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
                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SmallSpectrometers(
                                        Modifier.fillMaxSize().padding(10.dp),
                                        player.frequencyAnalyzer.lastFrequency,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    )
                }
                Spacer(Modifier.width(16.dp))
            }
            Box(Modifier.height(24.dp).animateContentSize()) {
                if (isCurrentSong && !showAlbumCover) {
                    Row {
                        SmallSpectrometers(
                            Modifier.size(24.dp),
                            player.frequencyAnalyzer.lastFrequency
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall)
                if (showAlbumInfo || showArtistInfo) {
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val style = MaterialTheme.typography.titleSmall
                    if (showAlbumInfo) {
                        SingleLineText(song.album.title, style = style)
                    }
                    if (showAlbumInfo && showArtistInfo) {
                        SingleLineText("•", Modifier.padding(horizontal = 8.dp), style = style)
                    }
                    if (showArtistInfo) {
                        SingleLineText(song.artist.name, style = style)
                    }
                }
            }
            SingleLineText(
                song.length.format(),
                Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.onSurfaceSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}