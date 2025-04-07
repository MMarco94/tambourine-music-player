package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.toIntOrMax
import javax.sound.sampled.AudioFormat

class SeekableAudioInputStream(
    val format: AudioFormat,
    private val input: AsyncAudioInputStream.Reader
) {
    var readBytes = 0L
        private set
    var readFrames = 0L
        private set

    suspend fun seekTo(frames: Long) {
        input.reset()
        readBytes = input.skip(frames * format.frameSize)
        readFrames = readBytes / format.frameSize
    }

    suspend fun read(maxBytes: Int, limitBytes: Long): Chunk? {
        require(maxBytes > 0)
        val toRead = maxBytes.coerceAtMost((limitBytes - readBytes).toIntOrMax())
        if (toRead <= 0) {
            return null
        }
        val read = input.read(toRead)
        if (read != null) {
            readBytes += read.length
            readFrames = readBytes / format.frameSize
        }
        return read
    }
}