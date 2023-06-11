package io.github.musicplayer.audio

import io.github.musicplayer.utils.countZeros
import io.github.musicplayer.utils.framesToDuration
import mu.KotlinLogging
import javax.sound.sampled.AudioFormat

private val logger = KotlinLogging.logger {}

class SeekableAudioInputStream(
    val format: AudioFormat,
    private val input: AsyncAudioInputStream.Reader
) {
    private var readFrames = 0L
    val readTime get() = format.framesToDuration(readFrames)

    private var zeroFrames: Long = -1L

    suspend fun seekToStart() {
        input.reset()
        readFrames = 0

        if (zeroFrames == -1L) {
            skipZeros()
            zeroFrames = readFrames
        } else {
            seekTo(zeroFrames)
        }
    }

    suspend fun seekTo(frames: Long) {
        input.reset()
        readFrames = input.skip(frames * format.frameSize) / format.frameSize
    }

    suspend fun read(max: Int): Chunk? {
        val read = input.read(max)
        if (read != null) {
            readFrames += read.length / format.frameSize
        }
        return read
    }

    private suspend fun skipZeros() {
        val currentPosition = readFrames
        var totalZerosFrames = 0L
        do {
            val chunk = read(Int.MAX_VALUE)
            val zeroBytes: Int
            if (chunk != null) {
                zeroBytes = chunk.readData.countZeros(0, chunk.length)
                totalZerosFrames += zeroBytes / format.frameSize
            } else {
                zeroBytes = 0
            }
        } while (chunk != null && zeroBytes == chunk.length)
        if (totalZerosFrames > 0) {
            logger.debug {
                "Skipping ${format.framesToDuration(totalZerosFrames)} of silent frames"
            }
        }
        seekTo(currentPosition + totalZerosFrames)
    }
}