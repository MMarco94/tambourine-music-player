package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.utils.digits
import io.github.mmarco94.tambourine.utils.format

@Composable
fun SongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    item: SongListItem.SongListItem,
    showTrackNumber: Boolean = false,
    showAlbumInfo: Boolean = true,
    showArtistInfo: Boolean = true,
    showAlbumCover: Boolean = showAlbumInfo,
    controller: SongQueueController,
) {
    BaseSongRow(
        modifier = modifier,
        maxTrackNumber = maxTrackNumber,
        song = item.song,
        isCurrentSong = playerController.current.queue?.currentSong == item.song,
        showTrackNumber = showTrackNumber,
        showAlbumInfo = showAlbumInfo,
        showArtistInfo = showArtistInfo,
        showAlbumCover = showAlbumCover,
        controller = controller,
        play = { shuffle ->
            controller.play(item.song, shuffle)
        },
    )
}

@Composable
fun SongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    item: SongListItem.QueuedSongListItem,
    showTrackNumber: Boolean = false,
    showAlbumInfo: Boolean = true,
    showArtistInfo: Boolean = true,
    showAlbumCover: Boolean = showAlbumInfo,
    controller: SongQueueController,
) {
    BaseSongRow(
        modifier = modifier,
        maxTrackNumber = maxTrackNumber,
        song = item.song,
        isCurrentSong = playerController.current.queue?.position == item.indexInQueue,
        showTrackNumber = showTrackNumber,
        showAlbumInfo = showAlbumInfo,
        showArtistInfo = showArtistInfo,
        showAlbumCover = showAlbumCover,
        controller = controller,
        play = { shuffle ->
            controller.playQueued(item.indexInQueue, shuffle)
        },
    )
}

@Composable
private fun BaseSongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    song: Song,
    isCurrentSong: Boolean,
    showTrackNumber: Boolean,
    showAlbumInfo: Boolean,
    showArtistInfo: Boolean,
    showAlbumCover: Boolean,
    controller: SongQueueController,
    play: (shuffle: Boolean?) -> Unit,
) {
    val player = playerController.current
    ContextMenuArea(items = {
        listOf(
            ContextMenuItemWithIcon(Icons.Default.PlayCircle, "Play") {
                play(false)
            },
            ContextMenuItemWithIcon(Icons.Default.Shuffle, "Shuffle from here") {
                play(true)
            },
            ContextMenuItemWithIcon(Icons.AutoMirrored.Default.ReadMore, "Play next") {
                controller.playNext(song)
            },
            ContextMenuItemWithIcon(Icons.AutoMirrored.Default.PlaylistAdd, "Add to queue") {
                controller.addToQueue(song)
            },
            ContextMenuItemWithIcon(Icons.Default.Album, "Play album") {
                controller.playAlbum(song)
            },
            ContextMenuItemWithIcon(Icons.Default.Groups, "Play artist") {
                controller.playArtist(song)
            },
        )
    }) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shadowElevation = if (isCurrentSong) 2.dp else 0.dp,
        ) {
            Row(
                Modifier.clickable { play(null) },
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
}