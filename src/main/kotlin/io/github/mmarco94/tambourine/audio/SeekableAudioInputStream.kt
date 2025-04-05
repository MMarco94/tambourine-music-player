package io.github.mmarco94.tambourine.audio

import javax.sound.sampled.AudioFormat

class SeekableAudioInputStream(
    val format: AudioFormat,
    private val input: AsyncAudioInputStream.Reader
) {
    var readFrames = 0L
        private set

    suspend fun seekTo(frames: Long) {
        input.reset()
        readFrames = input.skip(frames * format.frameSize) / format.frameSize
    }

    suspend fun read(maxBytes: Int): Chunk? {
        val read = input.read(maxBytes)
        if (read != null) {
            readFrames += read.length / format.frameSize
        }
        return read
    }
}