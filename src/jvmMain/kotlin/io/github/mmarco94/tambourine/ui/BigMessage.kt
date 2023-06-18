package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BigMessage(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    message: String? = null,
) {
    BigMessage(
        modifier = modifier,
        icon = { Icon(icon, null, Modifier.matchParentSize()) },
        title = { Text(title, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge) },
        message = message?.let {
            { Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium) }
        }
    )
}

@Composable
fun BigMessage(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    message: @Composable () -> Unit,
) {
    BigMessage(
        modifier = modifier,
        icon = { Icon(icon, null, Modifier.matchParentSize()) },
        title = { Text(title, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge) },
        message = message
    )
}

@Composable
fun BigMessage(
    modifier: Modifier,
    icon: @Composable BoxScope.() -> Unit,
    title: @Composable () -> Unit,
    message: (@Composable () -> Unit)? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(96.dp)) {
            icon()
        }
        Spacer(Modifier.height(16.dp))
        title()
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            message()
        }
        Spacer(Modifier.weight(1f))
    }
}