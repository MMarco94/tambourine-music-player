package io.github.mmarco94.tambourine.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.AlbumCover

@Composable
fun AlbumContainer(
    modifier: Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier.aspectRatio(1f),
        shape = shape,
        tonalElevation = elevation,
        shadowElevation = elevation,
        content = content,
    )
}

@Composable
fun AlbumCoverContent(
    cover: AlbumCover?,
    colorFilter: ColorFilter? = null,
    fullResolution: Boolean = false,
) {
    if (cover != null) {
        key(cover) {
            var image by remember { mutableStateOf(cover.previewImage) }
            LaunchedEffect(cover, fullResolution) {
                image = cover.previewImage
                if (fullResolution) {
                    image = cover.decodeFullImage()
                }
            }
            Image(
                image,
                null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                colorFilter = colorFilter,
            )
        }
    }
}

@Composable
fun AlbumCover(
    cover: AlbumCover?,
    modifier: Modifier,
    shape: Shape = RectangleShape,
    elevation: Dp = 0.dp,
    overlay: @Composable () -> Unit = {},
) {
    AlbumContainer(modifier, shape, elevation) {
        AlbumCoverContent(cover)
        overlay()
    }
}

@Composable
fun AlbumCoverBackground(cover: AlbumCover?, modifier: Modifier) {
    Crossfade(cover) { cover ->
        val bgColor = cover?.colorScheme?.auto()?.background ?: MaterialTheme.colorScheme.background
        Box(modifier.background(bgColor).paperNoise().blur(64.dp)) {
            if (cover != null) {
                Image(
                    cover.previewImage,
                    null,
                    alpha = .3f,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                )
            }
        }
    }
}
