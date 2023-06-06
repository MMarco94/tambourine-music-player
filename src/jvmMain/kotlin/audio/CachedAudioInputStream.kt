package audio

import framesToDuration
import javax.sound.sampled.AudioInputStream
import kotlin.time.Duration


class CachedAudioInputStream(
    input: AudioInputStream
) {
    private val format = input.format
    private val buffered = input.buffered()
    private var readFrames = 0L
    val readTime get() = format.framesToDuration(readFrames)

    private val skippedFrames: Long
    val startDelay: Duration

    init {
        val buffer = ByteArray(format.frameSize)
        buffered.mark(Int.MAX_VALUE)
        var zeros = 0L
        while (buffered.read(buffer) == format.frameSize && buffer.all { it == 0.toByte() }) {
            zeros++
        }
        skippedFrames = zeros
        if (skippedFrames > 0) {
            println("Skipping $skippedFrames silent frames")
        }
        readFrames = skippedFrames
        startDelay = format.framesToDuration(zeros)
        seekToStart()
    }

    fun seekToStart() {
        resetTo(skippedFrames)
    }

    fun resetTo(frames: Long) {
        buffered.reset()
        readFrames = 0
        skipFrames(frames)
    }

    fun read(buf: ByteArray, offset: Int, length: Int): Int {
        val read = buffered.read(buf, offset, length)
        readFrames += read / format.frameSize
        return read
    }

    private fun skipFrames(frames: Long): Long {
        val skipped = buffered.skip(frames * format.frameSize)
        readFrames += skipped / format.frameSize
        return skipped
    }
}