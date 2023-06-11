package io.github.musicplayer.audio

import io.github.musicplayer.utils.AppendOnlyList
import io.github.musicplayer.utils.toIntOrMax
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.yield
import javax.sound.sampled.AudioInputStream

class Chunk(
    val readData: ByteArray,
    val offset: Int,
    val length: Int,
)

class AsyncAudioInputStream(
    private val input: AudioInputStream,
    readerCount: Int,
) {

    data class BufferState(
        val chunks: List<Chunk>,
        val allBuffered: Boolean,
    )

    // Buffer, rounded so chunks contain a whole number of frames
    private val bufferChunks = 1.shl(14) / input.format.frameSize * input.format.frameSize
    val readers = List(readerCount) { Reader() }

    private val chunks = AppendOnlyList<Chunk>()

    suspend fun bufferAll() {
        while (!buffer()) {
            yield()
        }
    }

    /**
     * Returns whether the finish has been reached
     */
    suspend fun buffer(): Boolean {
        val buffer = ByteArray(bufferChunks)
        val read = input.read(buffer)
        return when {
            read < 0 -> {
                readers.forEach { it.channel.send(BufferState(chunks, true)) }
                true
            }

            read == 0 -> false
            else -> {
                chunks.add(Chunk(buffer, 0, read))
                val immutableView = chunks.subList(0, chunks.size)
                readers.forEach { it.channel.send(BufferState(immutableView, false)) }
                false
            }
        }
    }

    class Reader {
        val channel = Channel<BufferState>(Channel.CONFLATED)
        private var bufferState = BufferState(emptyList(), false)
        private var position = 0
        private var offset = 0

        fun reset() {
            position = 0
            offset = 0
        }

        /**
         * Returns the amount skipped
         */
        suspend fun skip(bytes: Long): Long {
            var ret = 0L
            while (ret < bytes) {
                internalRead(
                    minOf(Int.MAX_VALUE, (bytes - ret).toIntOrMax()),
                    { return ret },
                    { _, _, size: Int -> ret += size }
                )
            }
            return ret
        }

        /**
         * Returns the read amount
         */
        suspend fun read(max: Int): Chunk? {
            internalRead(
                max,
                { return null },
                { chunk: Chunk, offset: Int, size: Int ->
                    return Chunk(chunk.readData, offset, size)
                }
            )
            throw IllegalStateException()
        }


        private suspend inline fun internalRead(
            length: Int,
            onFinished: () -> Unit,
            onRead: (chunk: Chunk, offset: Int, size: Int) -> Unit,
        ) {
            require(length >= 0)

            if (!bufferState.allBuffered && position !in bufferState.chunks.indices) {
                bufferState = channel.receive()
            }

            val info = bufferState.chunks.getOrNull(position) ?: return onFinished()
            val off = offset
            val toRead = minOf(length, info.length - off)
            if (off + toRead >= info.length) {
                require(off + toRead == info.length)
                position++
                offset = 0
            } else {
                offset += toRead
            }
            return onRead(info, off, toRead)
        }
    }
}