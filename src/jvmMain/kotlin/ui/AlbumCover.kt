package ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AlbumCover(cover: ImageBitmap?, size: Dp, shape: Shape = RectangleShape) {
    Surface(Modifier.size(size).aspectRatio(1f), shape = shape) {
        if (cover != null) {
            Image(
                cover,
                null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
            )
        }
    }
}

@Composable
fun BlurredAlbumCover(cover: ImageBitmap?, modifier: Modifier) {
    if (cover != null) {
        Image(
            cover,
            null,
            alpha = .3f,
            modifier = modifier.blur(64.dp),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
        )
    }
}

@Composable
fun BlurredFadeAlbumCover(cover: ImageBitmap?, modifier: Modifier) {
    Crossfade(cover) { c ->
        BlurredAlbumCover(c, modifier)
    }
}