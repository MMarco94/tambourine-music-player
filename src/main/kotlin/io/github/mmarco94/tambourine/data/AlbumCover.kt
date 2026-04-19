package io.github.mmarco94.tambourine.data

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import io.github.mmarco94.tambourine.ui.TambourineTheme
import io.github.mmarco94.tambourine.utils.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jaudiotagger.audio.AudioFileIO
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

private val logger = KotlinLogging.logger {}

private const val PREVIEW_SIZE = 256

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

sealed interface AlbumCover {
    val fullWidth: Int
    val fullHeight: Int
    val previewImage: ImageBitmap
    val colorPalette: List<Color>
    val colorScheme: TambourineTheme.ColorSchemeContainer
    fun getMprisFile(song: Song): Path?

    suspend fun decodeFullImage(
        song: Song,
        targetW: Int,
        targetH: Int,
        currentDecodedImage: ImageBitmap? = null
    ): ImageBitmap?

    @Stable
    @Immutable
    data class File(
        override val fullWidth: Int,
        override val fullHeight: Int,
        val file: Path,
        override val previewImage: ImageBitmap,
    ) : AlbumCover {
        override val colorPalette: List<Color> = previewImage.palette(4)
        override val colorScheme: TambourineTheme.ColorSchemeContainer =
            TambourineTheme.colorScheme(colorPalette.map { it.hsb() })

        override suspend fun decodeFullImage(
            song: Song,
            targetW: Int,
            targetH: Int,
            currentDecodedImage: ImageBitmap?
        ): ImageBitmap? {
            return decode(
                logTag = file,
                targetW = targetW,
                targetH = targetH,
                currentDecodedImage = currentDecodedImage,
            ) {
                Image.makeFromEncoded(file.readBytes())
            }
        }

        override fun getMprisFile(song: Song): Path = file
    }

    @Stable
    @Immutable
    class SongBacked(
        override val fullWidth: Int,
        override val fullHeight: Int,
        override val previewImage: ImageBitmap,
    ) : AlbumCover {
        override val colorPalette: List<Color> = previewImage.palette(4)
        override val colorScheme: TambourineTheme.ColorSchemeContainer =
            TambourineTheme.colorScheme(colorPalette.map { it.hsb() })

        fun getRawBytes(song: Song): ByteArray? {
            val audioFileTag = AudioFileIO.read(song.file.toFile()).tag
            return audioFileTag.firstArtwork?.binaryData
        }

        override suspend fun decodeFullImage(
            song: Song,
            targetW: Int,
            targetH: Int,
            currentDecodedImage: ImageBitmap?
        ): ImageBitmap? {
            return decode(
                logTag = song.file,
                targetW = targetW,
                targetH = targetH,
                currentDecodedImage = currentDecodedImage,
            ) {
                getRawBytes(song)?.let {
                    Image.makeFromEncoded(it)
                }
            }
        }

        private var mprisFileCache: Wrapper<Path?>? = null

        override fun getMprisFile(song: Song): Path? {
            val cache = mprisFileCache
            if (cache != null) {
                return cache.value
            } else {
                val path = try {
                    val f = Files.createTempFile("album-cover-", ".png")
                    getRawBytes(song)?.let {
                        f.writeBytes(it)
                        f
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error saving image ${song.file}" }
                    null
                }
                mprisFileCache = Wrapper(path)
                return path
            }
        }
    }
}

private suspend fun AlbumCover.decode(
    logTag: Any,
    targetW: Int,
    targetH: Int,
    currentDecodedImage: ImageBitmap?,
    decoder: suspend () -> Image?,
): ImageBitmap? {
    val fallback =
        if (currentDecodedImage != null && currentDecodedImage.width > previewImage.width && currentDecodedImage.height > previewImage.height) {
            currentDecodedImage
        } else {
            previewImage
        }
    return if (fallback.width >= targetW && fallback.height >= targetH) {
        // Fallback is already good enough
        fallback
    } else if (fallback.width == fullWidth && fallback.height == fullHeight) {
        // Fallback is already as good as it gets
        fallback
    } else {
        // Decode full image
        withContext(Dispatchers.Default) {
            try {
                logger.debugElapsed("Decoding Full Image $logTag at $targetW:$targetH") {
                    val image = decoder()
                    image?.toBitmap(targetW, targetH)?.asComposeImageBitmap()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error decoding image $logTag" }
                null
            }
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
                        val previewImage = image.toBitmap(PREVIEW_SIZE, PREVIEW_SIZE).asComposeImageBitmap()
                        AlbumCover.SongBacked(
                            fullWidth = image.width,
                            fullHeight = image.height,
                            previewImage = previewImage,
                        )
                    } catch (e: Exception) {
                        logger.error { e.message }
                        null
                    }
                }
            }
        }
    }

    fun decode(file: Path): Deferred<AlbumCover?> {
        // Files' decoding isn't shared, since presumably I'll be called just once per file
        return coroutineScope.async {
            try {
                val image = Image.makeFromEncoded(file.readBytes())
                val previewImage = image.toBitmap(PREVIEW_SIZE, PREVIEW_SIZE).asComposeImageBitmap()
                AlbumCover.File(
                    fullWidth = image.width,
                    fullHeight = image.height,
                    file = file,
                    previewImage = previewImage,
                )
            } catch (e: Exception) {
                logger.error { e.message }
                null
            }
        }
    }
}