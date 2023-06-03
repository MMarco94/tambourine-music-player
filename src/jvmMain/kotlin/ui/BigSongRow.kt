package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import data.Song

@Composable
fun BigSongRow(
    inAlbumContext: Boolean,
    songs: List<Song>,
    sideOffset: Int,
    onSongSelected: (Song) -> Unit,
    sideContent: @Composable () -> Unit
) {
    Column {
        SidePanel(
            sideOffset,
            sideContent = {
                Column(
                    Modifier
                        .width(128.dp + 32.dp)
                        .padding(
                            top = 16.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    sideContent()
                }
            },
            mainContent = {
                Column {
                    songs.forEach { song ->
                        SongRow(song, inAlbumContext = inAlbumContext, onSongSelected = { onSongSelected(song) })
                    }
                }
            }
        )
        Divider()
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

        val sideMeasurable = measurables[0]
        val sidePlaceable = sideMeasurable.measure(constraints)

        val mainMeasurable = measurables[1]
        val mainPlaceable = mainMeasurable.measure(
            if (constraints.hasBoundedWidth) {
                constraints.copy(
                    minWidth = (constraints.minWidth - sidePlaceable.width).coerceAtLeast(0),
                    maxWidth = (constraints.maxWidth - sidePlaceable.width).coerceAtLeast(0),
                )
            } else constraints
        )

        val offset = sideOffset.coerceAtMost(mainPlaceable.height - sidePlaceable.height)
        layout(
            sidePlaceable.width + mainPlaceable.width,
            maxOf(sidePlaceable.height, mainPlaceable.height)
        ) {
            sidePlaceable.place(0, offset)
            mainPlaceable.place(sidePlaceable.width, 0)
        }
    }
}