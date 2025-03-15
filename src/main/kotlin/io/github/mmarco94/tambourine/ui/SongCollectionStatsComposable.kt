package io.github.mmarco94.tambourine.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import io.github.mmarco94.tambourine.data.SongCollectionStats
import io.github.mmarco94.tambourine.utils.format

@Composable
fun SongCollectionStatsComposable(stats: SongCollectionStats, yearOnly: Boolean = false) {
    val style = MaterialTheme.typography.labelMedium.merge(
        TextStyle(
            color = MaterialTheme.colorScheme.onSurfaceSecondary,
            textAlign = TextAlign.Center,
        )
    )
    // TODO: plurals
    if (!yearOnly) {
        SingleLineText("${stats.songsCount} songs", style = style)
        SingleLineText(stats.totalLength.format(), style = style)
    }
    if (stats.year != null) {
        if (stats.year.first == stats.year.last) {
            SingleLineText(stats.year.first.toString(), style = style)
        } else {
            SingleLineText("${stats.year.first}-${stats.year.last}", style = style)
        }
    }
}