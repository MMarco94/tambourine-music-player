package audio

import mu.KotlinLogging
import utils.chunked
import utils.countZeros
import utils.framesToDuration
import javax.sound.sampled.AudioInputStream

private val logger = KotlinLogging.logger {}
private val skipBuffer = ByteArray(1.shl(18))

class CachedAudioInputStream(
    input: AudioInputStream
) {
    private val format = input.format
    private val buffered = input.buffered()
    private var readFrames = 0L
    val readTime get() = format.framesToDuration(readFrames)

    private val skippedFrames: Long

    init {
        buffered.mark(Int.MAX_VALUE)
        skipZeros()
        skippedFrames = readFrames
    }

    fun seekToStart() {
        seekTo(skippedFrames)
    }

    fun seekTo(frames: Long) {
        buffered.reset()
        readFrames = 0
        skipFrames(frames)
    }

    fun read(buf: ByteArray, offset: Int, length: Int): Int {
        val read = buffered.read(buf, offset, length)
        readFrames += read / format.frameSize
        return read
    }

    private fun skipFrames(frames: Long) {
        require(frames >= 0L)
        var bytesToSkip = frames * format.frameSize

        if (bytesToSkip == 0L) return
        val skipped = buffered.skip(bytesToSkip)
        readFrames += skipped / format.frameSize
        bytesToSkip -= skipped

        // Unfortunately `skip` is not reliable. Reading instead
        if (bytesToSkip == 0L) return
        logger.debug("Skip didn't read all frames, skipping by reading the stream")
        bytesToSkip.chunked(skipBuffer.size) { s ->
            read(skipBuffer, 0, s)
        }
    }

    private fun skipZeros() {
        val currentPosition = readFrames
        var totalZerosFrames = 0L
        do {
            val readBytes = read(skipBuffer, 0, skipBuffer.size / format.frameSize * format.frameSize)
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