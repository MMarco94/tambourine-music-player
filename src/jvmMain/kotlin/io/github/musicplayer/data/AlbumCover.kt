package io.github.musicplayer.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO

data class AlbumCover(
    val image: ImageBitmap,
) {
    private val lazyFile = lazy {
        val f = File.createTempFile("album-cover-", ".png")
        val img = image.toAwtImage()
        require(ImageIO.write(img, "png", f))
        f
    }
    val file: File by lazyFile

    val fileOrNull: File? get() = if (lazyFile.isInitialized()) file else null
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
                        null
                    }
                }
            }
        }
    }
}