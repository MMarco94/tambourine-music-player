package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.data.SongListItem
import io.github.mmarco94.tambourine.data.SongQueueController

val BIG_SONG_ROW_DESIRED_WIDTH = 480.dp

@Composable
fun BigSongRow(
    maxTrackNumber: Int?,
    showTrackNumber: Boolean,
    showAlbumInfo: Boolean,
    showArtistInfo: Boolean,
    showAlbumCover: Boolean = showAlbumInfo,
    sidePanelPadding: Dp = 16.dp,
    songs: List<Song>,
    sideOffset: Int,
    controller: SongQueueController,
    sideContent: @Composable () -> Unit
) {
    val sidePanelW = 128.dp + 32.dp
    Column {
        SidePanel(
            sideOffset,
            sideContent = {
                Column(
                    Modifier
                        .width(sidePanelW)
                        .padding(
                            top = sidePanelPadding,
                            bottom = sidePanelPadding,
                            start = sidePanelPadding,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    sideContent()
                }
            },
            mainContent = {
                Column {
                    songs.forEach { song ->
                        SongRow(
                            Modifier.padding(end = 8.dp), // Space for scrollbar
                            maxTrackNumber,
                            SongListItem.SongListItem(song),
                            showTrackNumber = showTrackNumber,
                            showAlbumInfo = showAlbumInfo,
                            showArtistInfo = showArtistInfo,
                            showAlbumCover = showAlbumCover,
                            controller = controller,
                        )
                    }
                }
            }
        )
        HorizontalDivider()
    }
}

@Composable
private fun SidePanel(
    sideOffset: Int,
    sideContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
) {
    Layout(
        content = {
            sideContent()
            mainContent()
        }) { measurables, constraints ->
        require(measurables.size == 2)

        val sidePlaceable = if (showSidePanel(constraints)) {
            val sideMeasurable = measurables[0]
            sideMeasurable.measure(constraints)
        } else null

        val mainMeasurable = measurables[1]
        val mainPlaceable = mainMeasurable.measure(
            if (constraints.hasBoundedWidth && sidePlaceable != null) {
                constraints.copy(
                    minWidth = (constraints.minWidth - sidePlaceable.width).coerceAtLeast(0),
                    maxWidth = (constraints.maxWidth - sidePlaceable.width).coerceAtLeast(0),
                )
            } else constraints
        )

        layout(
            (sidePlaceable?.width ?: 0) + mainPlaceable.width,
            maxOf((sidePlaceable?.height ?: 0), mainPlaceable.height)
        ) {
            if (sidePlaceable != null) {
                val offset = sideOffset
                    .coerceAtMost(mainPlaceable.height - sidePlaceable.height)
                    .coerceAtLeast(0)
                sidePlaceable.place(0, offset)
            }
            mainPlaceable.place(sidePlaceable?.width ?: 0, 0)
        }
    }
}

private fun showSidePanel(constraints: Constraints): Boolean {
    return !constraints.hasBoundedWidth || constraints.maxWidth.dp >= BIG_SONG_ROW_DESIRED_WIDTH
}