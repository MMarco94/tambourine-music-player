package ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

@Composable
fun <T> PanelContainer(
    modifier: Modifier,
    panels: List<T>,
    render: @Composable (T) -> Unit,
) {
    Row(modifier) {
        panels.forEachIndexed { index, panel ->
            key(panel) {
                Box(
                    Modifier
                        .animateContentSize()
                        .background(Color.Black.copy(alpha = 1 - 0.9f.pow(panels.size - 1 - index)))
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    render(panel)
                }
            }
        }
    }
}