package io.github.mmarco94.tambourine.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.mmarco94.tambourine.ui.MusicPlayerTheme
import io.github.mmarco94.tambourine.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes

private val logger = KotlinLogging.logger {}

private const val PREVIEW_WIDTH = 256

@Stable
@Immutable
class RawImage(val bytes: ByteArray) {
    private val hc: Int = bytes.fastHashCode()

    override fun equals(other: Any?): Boolean {
        return other is RawImage && other.hc == hc && other.bytes.contentEquals(bytes)
    }

    override fun hashCode(): Int {
        return hc
    }
}

@Stable
@Immutable
class AlbumCover(
    val rawImage: RawImage,
    val previewImage: ImageBitmap,
) {
    val colorPalette = previewImage.palette(4)
    val colorScheme = MusicPlayerTheme.colorScheme(colorPalette.map { it.hsb() })

    suspend fun decodeFullImage(): ImageBitmap {
        return withContext(Dispatchers.Default) {
            logger.debugElapsed("Decoding Full Image") {
                Image.makeFromEncoded(rawImage.bytes).toComposeImageBitmap()
            }
        }
    }

    val file: Path? by lazy {
        try {
            val f = Files.createTempFile("album-cover-", ".png")
            f.writeBytes(rawImage.bytes)
            f
        } catch (e: Exception) {
            logger.error { "Error saving image: $e" }
            null
        }
    }
}

class CoversDecoder(
    private val coroutineScope: CoroutineScope,
) {

    private val mutex = Mutex()
    private val jobs = mutableMapOf<RawImage, Deferred<AlbumCover?>>()

    suspend fun decode(img: ByteArray): Deferred<AlbumCover?> {
        val rawImg = RawImage(img)
        return mutex.withLock {
            jobs.getOrPut(rawImg) {
                coroutineScope.async {
                    try {
                        val image = Image.makeFromEncoded(img)
                        val width = minOf(PREVIEW_WIDTH, image.width)
                        AlbumCover(rawImg, image.toBitmap(width).asComposeImageBitmap())
                    } catch (e: Exception) {
                        logger.error { e.message }
                        null
                    }
                }
            }
        }
    }
}