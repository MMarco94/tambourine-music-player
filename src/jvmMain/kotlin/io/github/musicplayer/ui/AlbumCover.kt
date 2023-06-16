package io.github.musicplayer.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.musicplayer.data.AlbumCover

@Composable
fun AlbumCover(
    cover: AlbumCover?,
    modifier: Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = 0.dp,
    overlay: @Composable () -> Unit = {},
) {
    Surface(modifier.aspectRatio(1f), shape = shape, tonalElevation = elevation, shadowElevation = elevation) {
        if (cover != null) {
            key(cover) {
                Image(
                    cover.image,
                    null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                )
            }
        }
        overlay()
    }
}

@Composable
fun BlurredAlbumCover(cover: AlbumCover?, modifier: Modifier) {
    if (cover != null) {
        Image(
            cover.image,
            null,
            alpha = .3f,
            modifier = modifier.blur(64.dp),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
        )
    }
}

@Composable
fun BlurredFadeAlbumCover(cover: AlbumCover?, modifier: Modifier) {
    Crossfade(cover) { c ->
        BlurredAlbumCover(c, modifier)
    }
}