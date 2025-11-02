package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BaseTag(
    active: Boolean,
    enabled: Boolean,
    content: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
        }
    )
    val contentColor by animateColorAsState(
        if (enabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
    val alpha by animateFloatAsState(if (active) 1f else INACTIVE_ALPHA)
    Card(
        Modifier.alpha(alpha),
        colors = CardDefaults.cardColors(bg, contentColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(Modifier.clickable { onClick() }) {
            content()
        }
    }
}

@Composable
fun Tag(
    active: Boolean,
    enabled: Boolean,
    showAsSubtitle: Boolean,
    icon: ImageVector,
    description: String,
    selectedLabel: String,
    selectedIcon: ImageVector?,
    reset: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Box(Modifier.padding(2.dp)) {
        BaseTag(
            active = active,
            enabled = enabled,
            onClick = onClick,
            content = {
                Row(
                    Modifier.height(IntrinsicSize.Max).heightIn(min = 40.dp).animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, description)
                        Spacer(Modifier.width(8.dp))
                        if (showAsSubtitle) {
                            Column {
                                SingleLineText(description, style = MaterialTheme.typography.labelLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SingleLineText(selectedLabel, style = MaterialTheme.typography.labelMedium)
                                    if (selectedIcon != null) {
                                        Spacer(Modifier.width(2.dp))
                                        Icon(selectedIcon, null, Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            SingleLineText(selectedLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (reset != null) {
                        IconButton({
                            reset()
                        }, Modifier.width(40.dp).fillMaxHeight()) {
                            Icon(Icons.Filled.Close, "Reset", Modifier.padding(8.dp))
                        }
                    }
                }
            },
        )
    }
}