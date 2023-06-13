package io.github.musicplayer.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Image

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
    private val jobs = mutableMapOf<RawImage, Deferred<ImageBitmap?>>()

    suspend fun decode(img: ByteArray): Deferred<ImageBitmap?> {
        val rawImg = RawImage(img)
        return mutex.withLock {
            jobs.getOrPut(rawImg) {
                coroutineScope.async {
                    Image.makeFromEncoded(img).toComposeImageBitmap()
                }
            }
        }
    }
}