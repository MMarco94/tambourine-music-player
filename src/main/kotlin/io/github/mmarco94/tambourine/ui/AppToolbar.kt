package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.generated.resources.*
import io.github.mmarco94.tambourine.utils.Preferences
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppToolbar(
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    autoSize: Boolean = true,
) {
    val useSystemDecorations by Preferences.useSystemDecorations.state
    Row(
        modifier = if (autoSize) {
            modifier.heightIn(min = 64.dp).padding(8.dp)
        } else {
            modifier
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        AppSettingsButton(Modifier, openSettings)
        if (!useSystemDecorations) {
            IconButton(closeApp) {
                Icon(Icons.Default.Close, stringResource(Res.string.action_close_app))
            }
        }
    }
}

@Composable
fun AppSettingsButton(
    modifier: Modifier,
    openSettings: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton({ showMenu = !showMenu }) {
            Icon(Icons.Default.MoreVert, stringResource(Res.string.action_open_menu))
        }
        DropdownMenu(
            showMenu,
            { showMenu = false },
            offset = DpOffset(8.dp, 0.dp),
        ) {
            DropdownMenuItem(
                text = { SingleLineText(stringResource(Res.string.settings), style = LocalTextStyle.current) },
                leadingIcon = { Icon(Icons.Default.Settings, null) },
                onClick = {
                    openSettings()
                    showMenu = false
                },
            )
            DropdownMenuItem(
                text = { SingleLineText(stringResource(Res.string.reload_library), style = LocalTextStyle.current) },
                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                onClick = {
                    Preferences.libraryFolder.reload()
                    showMenu = false
                },
            )
        }
    }
}
