package ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Icon(icon, null, Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, textAlign = TextAlign.Center, style = MaterialTheme.typography.h3)
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.subtitle1)
        }
        Spacer(Modifier.weight(1f))
    }
}