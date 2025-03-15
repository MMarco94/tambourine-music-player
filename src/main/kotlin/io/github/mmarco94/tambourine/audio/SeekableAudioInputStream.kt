package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.framesToDuration
import javax.sound.sampled.AudioFormat

class SeekableAudioInputStream(
    val format: AudioFormat,
    private val input: AsyncAudioInputStream.Reader
) {
    private var readFrames = 0L
    val readTime get() = format.framesToDuration(readFrames)

    suspend fun seekToStart() {
        input.reset()
        readFrames = 0
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
}