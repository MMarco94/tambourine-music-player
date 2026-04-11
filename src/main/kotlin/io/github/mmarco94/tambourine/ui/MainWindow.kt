package io.github.mmarco94.tambourine.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import io.github.mmarco94.tambourine.utils.Preferences
import io.github.mmarco94.tambourine.utils.Preferences.save
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainWindow(
    onCloseRequest: () -> Unit,
    visible: Boolean = true,
    title: String = "Untitled",
    icon: Painter? = null,
    transparent: Boolean = false,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    alwaysOnTop: Boolean = false,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit

) {
    val useSystemDecorations by Preferences.useSystemDecorations.state
    val state = Preferences.mainWindowState()
    key(useSystemDecorations) {
        Window(
            onCloseRequest = onCloseRequest,
            state = state,
            visible = visible,
            title = title,
            icon = icon,
            undecorated = !useSystemDecorations,
            transparent = transparent,
            resizable = resizable,
            enabled = enabled,
            focusable = focusable,
            alwaysOnTop = alwaysOnTop,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent,
        ) {
            val density = LocalDensity.current
            LaunchedEffect(state.size, state.position, state.placement, density) {
                delay(100.milliseconds)
                save(state, window.insets, density)
            }
            Scaled {
                content()
            }
        }
    }
}

@Composable
fun Scaled(content: @Composable () -> Unit) {
    val fontScale by Preferences.fontScale.state
    CompositionLocalProvider(
        LocalDensity provides Density(
            LocalDensity.current.density,
            LocalDensity.current.fontScale * fontScale
        )
    ) {
        content()
    }
}
