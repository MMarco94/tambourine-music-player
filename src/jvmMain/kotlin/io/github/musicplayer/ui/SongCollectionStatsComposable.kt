package io.github.musicplayer.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import io.github.musicplayer.data.SongCollectionStats
import io.github.musicplayer.utils.format

@Composable
fun SongCollectionStatsComposable(stats: SongCollectionStats, yearOnly: Boolean = false) {
    val style = MaterialTheme.typography.subtitle2.merge(
        TextStyle(
            color = MaterialTheme.colors.onSurfaceSecondary,
            textAlign = TextAlign.Center,
        )
    )
    // TODO: plurals
    if (!yearOnly) {
        Text("${stats.songsCount} songs", style = style)
        Text(stats.totalLength.format(), style = style)
    }
    if (stats.year != null) {
        if (stats.year.first == stats.year.last) {
            Text(stats.year.first.toString(), style = style)
        } else {
            Text("${stats.year.first}-${stats.year.last}", style = style)
        }
    }
}