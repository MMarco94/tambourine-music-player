package io.github.musicplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import io.github.musicplayer.utils.animateOrSnapFloatAsState
import kotlin.math.pow

private class PanelInfo(
    var startWeight: Int,
    var endWeight: Int,
    var visible: Boolean,
    var bgAlpha: Float,
)

@Composable
fun <T> PanelContainer(
    modifier: Modifier,
    allPanels: Set<T>,
    panels: List<T>,
    render: @Composable (T) -> Unit,
) {

    val panelsData: MutableMap<T, PanelInfo> = remember(allPanels) {
        allPanels
            .associateWith { PanelInfo(0, 0, false, 0f) }
            .toMutableMap()
    }
    panelsData.values.forEach { it.visible = false }
    panels.forEachIndexed { index, t ->
        panelsData.getValue(t).apply {
            startWeight = index
            endWeight = panels.size - index - 1
            visible = true
            bgAlpha = 1 - 0.9f.pow(endWeight)
        }
    }
    Box(modifier) {
        allPanels.forEach { panel ->
            key(panel) {
                val info = panelsData.getValue(panel)
                val alpha by animateFloatAsState(if (info.visible) 1f else 0f)
                val bgAlpha by animateFloatAsState(info.bgAlpha)

                val sw = animateOrSnapFloatAsState((alpha == 0f) to info.startWeight.toFloat())
                val ew = animateOrSnapFloatAsState((alpha == 0f) to info.endWeight.toFloat())
                if (alpha > 0) {
                    Row(Modifier.matchParentSize().alpha(alpha)) {
                        if (sw.value > 0) Spacer(Modifier.weight(sw.value))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = bgAlpha))) {
                            render(panel)
                        }
                        if (ew.value > 0) Spacer(Modifier.weight(ew.value))
                    }
                }
            }
        }
    }
}