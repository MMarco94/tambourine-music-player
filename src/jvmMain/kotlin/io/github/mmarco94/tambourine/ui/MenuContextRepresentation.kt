package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ContextMenuState.Status.Open
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round

object MenuContextRepresentation : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        var offset by remember { mutableStateOf(IntOffset.Zero) }
        if (status is Open) offset = status.rect.center.round()

        Box(Modifier.offset { offset }) {
            DropdownMenu(
                expanded = status is Open,
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
            ) {
                items().forEach { item ->
                    DropdownMenuItem(
                        text = { SingleLineText(item.label, style = LocalTextStyle.current) },
                        leadingIcon = if (item is ContextMenuItemWithIcon) {
                            { Icon(item.icon, null) }
                        } else null,
                        onClick = {
                            item.onClick()
                            state.status = ContextMenuState.Status.Closed
                        },
                    )
                }
            }
        }
    }
}

class ContextMenuItemWithIcon(
    val icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) : ContextMenuItem(label, onClick)