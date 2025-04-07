package io.github.mmarco94.tambourine.audio

import io.github.mmarco94.tambourine.utils.AppendOnlyList
import io.github.mmarco94.tambourine.utils.toIntOrMax
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
    private val firstBufferSize: Int,
    private val bufferSize: Int,
) {

    data class BufferState(
        val chunks: List<Chunk>,
        val allBuffered: Boolean,
    )

    val readers = List(readerCount) { Reader() }

    suspend fun bufferAll() {
        val chunks = AppendOnlyList<Chunk>()
        do {
            val buffer = ByteArray(if (chunks.isEmpty()) firstBufferSize else bufferSize)
            val read = input.read(buffer)
            if (read > 0) {
                chunks.add(Chunk(buffer, 0, read))
                val state = BufferState(chunks.subList(0, chunks.size), false)
                readers.forEach {
                    // Conflated channels should never block nor fail
                    require(it.channel.trySend(state).isSuccess)
                }
            }
            // Giving opportunities for this coroutine to be cancelled
            yield()
        } while (read >= 0)
        val state = BufferState(chunks, true)
        readers.forEach {
            it.channel.send(state)
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