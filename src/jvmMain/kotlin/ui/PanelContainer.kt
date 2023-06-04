@file:OptIn(ExperimentalAnimationApi::class)

package ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout

@Composable
fun <T> SingleOrDualPanelContainer(
    modifier: Modifier,
    dualPane: Boolean,
    mainPanel: T,
    secondPanel: T,
    render: @Composable (T) -> Unit,
) {
    if (dualPane) {
        DualPanelContainer(
            modifier,
            if (mainPanel != secondPanel) mainPanel else null,
            secondPanel,
            render,
        )
    } else {
        SinglePanelContainer(
            modifier,
            mainPanel,
            render,
        )
    }

}

@Composable
fun <T> DualPanelContainer(
    modifier: Modifier,
    firstPanel: T?,
    secondPanel: T,
    render: @Composable (T) -> Unit,
) {
    Row(modifier) {
        AnimatedContent(firstPanel, transitionSpec = {
            fadeIn() with fadeOut() using SizeTransform()
        }) { p ->
            if (p != null) {
                halfSize {
                    Box(Modifier.fillMaxHeight().background(Color.Black.copy(alpha = 0.1f))) {
                        render(p)
                    }
                }
            } else {
                Spacer(Modifier.fillMaxHeight())
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Crossfade(secondPanel) { p ->
                render(p)
            }
        }
    }
}

@Composable
fun <T> SinglePanelContainer(
    modifier: Modifier,
    panel: T,
    render: @Composable (T) -> Unit,
) {
    Crossfade(panel, modifier) { p ->
        render(p)
    }
}

@Composable
private fun halfSize(f: @Composable () -> Unit) {
    Layout(f) { m, constraints ->
        val p = m.single().measure(constraints.copy(maxWidth = constraints.maxWidth / 2))
        layout(p.width, p.height) {
            p.place(0, 0)
        }
    }
}