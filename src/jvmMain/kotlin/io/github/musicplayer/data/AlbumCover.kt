package io.github.musicplayer.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.musicplayer.utils.palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

data class AlbumCover(
    val image: ImageBitmap,
) {
    val palette = image.palette()
    val file: File? by lazy {
        try {
            val f = File.createTempFile("album-cover-", ".png")
            val img = image.toAwtImage()
            require(ImageIO.write(img, "png", f))
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

    private class RawImage(val bytes: ByteArray) {
        private val hc = bytes.contentHashCode()

        override fun equals(other: Any?): Boolean {
            return other is RawImage && other.hc == hc && other.bytes.contentEquals(bytes)
        }

        override fun hashCode(): Int {
            return hc
        }
    }

    private val mutex = Mutex()
    private val jobs = mutableMapOf<RawImage, Deferred<AlbumCover?>>()

    suspend fun decode(img: ByteArray): Deferred<AlbumCover?> {
        val rawImg = RawImage(img)
        return mutex.withLock {
            jobs.getOrPut(rawImg) {
                coroutineScope.async {
                    try {
                        val decoded = Image.makeFromEncoded(img).toComposeImageBitmap()
                        AlbumCover(decoded)
                    } catch (e: Exception) {
                        logger.error { e.message }
                        null
                    }
                }
            }
        }
    }
}