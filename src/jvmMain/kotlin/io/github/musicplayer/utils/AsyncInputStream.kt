package io.github.musicplayer.utils

import kotlinx.coroutines.channels.Channel
import java.io.InputStream

class AsyncInputStream(
    private val input: InputStream
) {

    private class ReadInfo(
        val readData: ByteArray,
        val readBytes: Int,
        val isFinished: Boolean,
    )

    private var position = 0
    private var offset = 0
    private val allInfo = mutableListOf<ReadInfo>()
    private val allBuffered get() = allInfo.lastOrNull()?.isFinished == true
    private val channel = Channel<ReadInfo>(Int.MAX_VALUE)

    /**
     * Returns whether the finish has been reached
     */
    suspend fun buffer(chunkSize: Int): Boolean {
        if (allBuffered) return true

        val buffer = ByteArray(chunkSize)
        val read = input.read(buffer)
        val info = ReadInfo(buffer, read.coerceAtLeast(0), read < 0)
        channel.send(info)
        return info.isFinished
    }

    fun reset() {
        position = 0
        offset = 0
    }

    /**
     * The amount skipped
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

    private suspend fun internalRead(buf: ByteArray?, length: Int): Int {
        require(length >= 0)
        if (length == 0) return 0
        if (allBuffered && position >= allInfo.size) return -1

        if (position !in allInfo.indices) {
            val newInfo = channel.receive()
            allInfo.add(newInfo)
        }

        val info = allInfo[position]
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