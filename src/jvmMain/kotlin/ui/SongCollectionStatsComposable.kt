package ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import data.SongCollectionStats
import onSurfaceSecondary
import rounded

@Composable
fun SongCollectionStatsComposable(stats: SongCollectionStats) {
    val style = MaterialTheme.typography.subtitle2.merge(
        TextStyle(
            color = MaterialTheme.colors.onSurfaceSecondary,
            textAlign = TextAlign.Center,
        )
    )
    Text("${stats.songsCount} songs", style = style)
    Text(stats.totalLength.rounded().toString(), style = style)
    if (stats.year != null) {
        if (stats.year.first == stats.year.last) {
            Text(stats.year.first.toString(), style = style)
        } else {
            Text("${stats.year.first}-${stats.year.last}", style = style)
        }
    }
}