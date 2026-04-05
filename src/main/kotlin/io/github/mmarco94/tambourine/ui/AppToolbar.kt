package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.utils.Preferences

@Composable
fun AppToolbar(
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    autoSize: Boolean = true,
) {
    val useSystemDecorations by Preferences.useSystemDecorations
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
                Icon(Icons.Default.Close, "Close app")
            }
        }
    }
}