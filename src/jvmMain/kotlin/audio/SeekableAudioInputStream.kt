package audio

import mu.KotlinLogging
import utils.AsyncInputStream
import utils.countZeros
import utils.framesToDuration
import javax.sound.sampled.AudioFormat

private val logger = KotlinLogging.logger {}
private val skipBuffer = ByteArray(1.shl(18))

class SeekableAudioInputStream(
    val format: AudioFormat,
    private val buffered: AsyncInputStream
) {
    private var readFrames = 0L
    val readTime get() = format.framesToDuration(readFrames)

    private var zeroFrames: Long = -1L

    suspend fun seekToStart() {
        buffered.reset()
        readFrames = 0

        if (zeroFrames == -1L) {
            skipZeros()
            zeroFrames = readFrames
        }
        seekTo(zeroFrames)
    }

    suspend fun seekTo(frames: Long) {
        buffered.reset()
        readFrames = buffered.skip(frames * format.frameSize) / format.frameSize
    }

    suspend fun read(buf: ByteArray, length: Int): Int {
        val read = buffered.read(buf, length)
        readFrames += read.coerceAtLeast(0) / format.frameSize
        return read
    }

    private suspend fun skipZeros() {
        val currentPosition = readFrames
        var totalZerosFrames = 0L
        do {
            val readBytes = read(skipBuffer, skipBuffer.size / format.frameSize * format.frameSize)
            val zeroBytes = skipBuffer.countZeros(0, readBytes)
            totalZerosFrames += zeroBytes / format.frameSize
        } while (readBytes > 0 && zeroBytes == readBytes)
        if (totalZerosFrames > 0) {
            logger.debug {
                "Skipping ${format.framesToDuration(totalZerosFrames)} of silent frames"
            }
        }
        seekTo(currentPosition + totalZerosFrames)
    }
}