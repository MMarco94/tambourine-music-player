package io.github.musicplayer.utils

import kotlinx.coroutines.channels.Channel
import java.io.InputStream

class AsyncInputStream(
    private val input: InputStream
) {

    private class Chunk(
        val readData: ByteArray,
        val readBytes: Int,
    )

    private var position = 0
    private var offset = 0
    private val chunks = mutableListOf<Chunk>()
    var allBuffered = false
        private set
    private val channel = Channel<Unit>(Channel.CONFLATED)

    /**
     * Returns whether the finish has been reached
     */
    suspend fun buffer(chunkSize: Int): Boolean {
        val buffer = ByteArray(chunkSize)
        val read = input.read(buffer)
        return when {
            read < 0 -> {
                allBuffered = true
                channel.send(Unit)
                true
            }

            read == 0 -> false
            else -> {
                chunks.add(Chunk(buffer, read))
                channel.send(Unit)
                false
            }
        }
    }

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
            val read = internalRead(null, minOf(Int.MAX_VALUE, (bytes - ret).toIntOrMax()))
            if (read < 0) return ret
            ret += read
        }
        return ret
    }

    /**
     * Returns the read amount
     */
    suspend fun read(buf: ByteArray, length: Int): Int {
        require(buf.size >= length)
        return internalRead(buf, length)
    }

    fun readAll(): ByteArray {
        require(allBuffered)
        val length = chunks.sumOf { it.readBytes }
        var chunk = 0
        var offset = 0
        return ByteArray(length) { i ->
            if (offset >= chunks[chunk].readBytes) {
                chunk++
                offset = 0
            }
            chunks[chunk].readData[offset++]
        }
    }

    private suspend fun internalRead(buf: ByteArray?, length: Int): Int {
        require(length >= 0)
        if (length == 0) return 0

        if (!allBuffered && position !in chunks.indices) {
            channel.receive()
        }

        val info = chunks.getOrNull(position) ?: return -1
        val ret = minOf(length, info.readBytes - offset)
        if (buf != null) {
            info.readData.copyInto(buf, 0, offset, offset + ret)
        }

        if (offset + ret >= info.readBytes) {
            require(offset + ret == info.readBytes)
            position++
            offset = 0
        } else {
            offset += ret
        }

        return ret
    }
}