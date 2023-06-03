package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
    val sidePanelW = 128.dp + 32.dp
    Column {
        SidePanel(
            sideOffset,
            sideContent = {
                Column(
                    Modifier
                        .width(sidePanelW)
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
                        SongRow(
                            song,
                            inAlbumContext = inAlbumContext,
                            onSongSelected = { onSongSelected(song) })
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
    return !constraints.hasBoundedWidth || constraints.maxWidth.dp >= 480.dp
}