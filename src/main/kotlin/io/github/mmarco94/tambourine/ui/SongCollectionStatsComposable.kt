package io.github.mmarco94.tambourine.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import io.github.mmarco94.tambourine.data.SongCollectionStats
import io.github.mmarco94.tambourine.generated.resources.Res
import io.github.mmarco94.tambourine.generated.resources.n_songs
import io.github.mmarco94.tambourine.utils.format
import org.jetbrains.compose.resources.pluralStringResource

@Composable
fun SongCollectionStatsComposable(stats: SongCollectionStats, yearOnly: Boolean = false) {
    val style = MaterialTheme.typography.labelMedium.merge(
        TextStyle(
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
    )
    if (!yearOnly) {
        SingleLineText(pluralStringResource(Res.plurals.n_songs, stats.songsCount, stats.songsCount), style = style)
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