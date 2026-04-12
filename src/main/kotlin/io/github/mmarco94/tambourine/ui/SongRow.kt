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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.playerController
import io.github.mmarco94.tambourine.utils.digits

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
        isCurrentSong = playerController.current.queue?.currentSongKey == item.song.uniqueKey,
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
            controller.playQueued(item.song, item.indexInQueue, shuffle)
        },
    )
}

@Composable
fun songRowEstimateHeight(showInfo: Boolean = false, showAlbum: Boolean = false): Dp {
    val minHeight = 40.dp
    val padding = 16.dp
    val albumHeight = if (showAlbum) 48.dp + padding else 0.dp
    val textHeight = with(LocalDensity.current) {
        MaterialTheme.typography.titleSmall.lineHeight.toDp()
    }
    val contentHeight = padding + if (showInfo) textHeight * 2 + 4.dp else textHeight
    return maxOf(
        minHeight,
        albumHeight,
        contentHeight,
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
            ContextMenuItemWithIcon(Icons.Default.PlayCircle, Res.string.action_play) {
                play(false)
            },
            ContextMenuItemWithIcon(Icons.Default.Shuffle, Res.string.action_shuffle_from_here) {
                play(true)
            },
            ContextMenuItemWithIcon(Icons.AutoMirrored.Default.ReadMore, Res.string.action_play_next) {
                controller.playNext(song)
            },
            ContextMenuItemWithIcon(Icons.AutoMirrored.Default.PlaylistAdd, Res.string.action_add_to_queue) {
                controller.addToQueue(song)
            },
            ContextMenuItemWithIcon(Icons.Default.Album, Res.string.action_play_album) {
                controller.playAlbum(song)
            },
            ContextMenuItemWithIcon(Icons.Default.Groups, Res.string.action_play_artist) {
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
                Modifier.clickable { play(null) }.heightIn(min = 40.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showTrackNumber) {
                    val maxDigits = maxTrackNumber?.digits() ?: 0
                    SingleLineText(
                        song.track?.toString().orEmpty().padStart(maxDigits, ' '),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (showAlbumCover) {
                    Box {
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
                                        SmallFakeSpectrometers(
                                            Modifier.fillMaxSize().padding(10.dp),
                                            player,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                Box(Modifier.height(24.dp).animateContentSize()) {
                    if (isCurrentSong && !showAlbumCover) {
                        Row {
                            SmallFakeSpectrometers(
                                Modifier.size(24.dp),
                                player,
                                color = LocalContentColor.current,
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
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
                    song.formattedLength,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}