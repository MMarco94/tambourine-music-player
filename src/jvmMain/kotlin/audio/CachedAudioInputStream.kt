package audio

import javax.sound.sampled.AudioInputStream

private val chunkSize = 1024

class CachedAudioInputStream(
    val input: AudioInputStream
) {
    val buffered = input.buffered()

    init {
        buffered.mark(Int.MAX_VALUE)
    }

    fun reset() = buffered.reset()

    fun read(buf: ByteArray, offset: Int, length: Int): Int {
        return buffered.read(buf, offset, length)
    }

    fun skip(n: Long): Long {
        return buffered.skip(n)
    }
}