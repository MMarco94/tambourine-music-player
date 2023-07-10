package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.Lyrics
import kotlin.math.roundToInt
import kotlin.time.Duration

private val verticalPadding = 48.dp

@Composable
fun LyricsComposable(
    lyrics: Lyrics,
    getPosition: () -> Duration,
    setPosition: (Duration) -> Unit
) {
    when (lyrics) {
        is Lyrics.Plain -> PlainLyricsComposable(lyrics)
        is Lyrics.Synchronized -> SynchronizedLyricsComposable(lyrics, getPosition, setPosition)
    }
}

@Composable
fun PlainLyricsComposable(lyrics: Lyrics.Plain) {
    LyricsContainer {
        item {
            Text(lyrics.lyrics, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SynchronizedLyricsComposable(
    lyrics: Lyrics.Synchronized,
    getPosition: () -> Duration,
    setPosition: (Duration) -> Unit
) {
    BoxWithConstraints {
        val height = maxHeight.toPxApprox()
        val offset = (verticalPadding + 32.dp).toPxApprox()

        val ss = rememberLazyListState()
        val position = getPosition()
        val activeIndex = lyrics.lines.indexOfLast { it.start < position }
        if (activeIndex >= 0) {
            LaunchedEffect(activeIndex) {
                ss.animateScrollToItem(activeIndex, (offset - height / 2).roundToInt().coerceAtMost(0))
            }
        }
        LyricsContainer(ss) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isActive = index == activeIndex
                val alpha by animateFloatAsState(if (isActive) 1f else .5f)
                val weight by animateFloatAsState((if (isActive) FontWeight.Bold else FontWeight.Normal).weight.toFloat())

                Surface(shape = MaterialTheme.shapes.small, color = Color.Transparent) {
                    // This text is identical, but "as big as it can be"
                    Text(
                        line.content,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.alpha(0f).padding(8.dp),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        line.content,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(weight.roundToInt())),
                        modifier = Modifier.alpha(alpha).clickable { setPosition(line.start) }.padding(8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}


@Composable
private fun LyricsContainer(
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = state,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = verticalPadding)
    ) {
        content()
    }
}