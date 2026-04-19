package io.github.mmarco94.tambourine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mmarco94.tambourine.data.AlbumCover
import io.github.mmarco94.tambourine.data.Song
import io.github.mmarco94.tambourine.utils.FadeIn
import kotlin.time.Duration.Companion.milliseconds

private val DEFAULT_BACKGROUND_FADE_IN_DURATION = 300.milliseconds
private val DEFAULT_FULL_RESOLUTION_FADE_IN_DURATION = 500.milliseconds


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
    /** pass this if you want a full-resolution image. Null to get just the preview */
    fullResolutionSource: Song? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        // Rounded resolution targets
        val targetW = (constraints.maxWidth / 128f + 1).toInt() * 128
        val targetH = (constraints.maxHeight / 128f + 1).toInt() * 128
        if (cover != null) {
            key(cover) {
                var fullRes: ImageBitmap? by remember { mutableStateOf(null) }
                LaunchedEffect(cover, fullResolutionSource, targetW, targetH) {
                    val currentFullRes = fullRes
                    if (fullResolutionSource != null) {
                        val fullImage = cover.decodeFullImage(fullResolutionSource, targetW, targetH, currentFullRes)
                        if (fullImage != null) {
                            fullRes = fullImage
                        }
                    }
                }
                FadeIn(
                    targetState = fullRes ?: cover.previewImage,
                    duration = DEFAULT_FULL_RESOLUTION_FADE_IN_DURATION,
                ) { image ->
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
    FadeIn(
        targetState = cover,
        duration = DEFAULT_BACKGROUND_FADE_IN_DURATION,
    ) { cover ->
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
