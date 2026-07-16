package com.metrolist.desktop.player

internal class PcmFrameBuffer(
    private val frameSize: Int,
) {
    init {
        require(frameSize > 0) { "frameSize must be positive" }
    }

    private var pending = ByteArray(0)

    val pendingByteCount: Int
        get() = pending.size

    fun consume(
        source: ByteArray,
        length: Int,
    ): ByteArray {
        require(length in 0..source.size) { "length must be within the source array" }
        if (length == 0) return ByteArray(0)

        val combined = ByteArray(pending.size + length)
        pending.copyInto(combined)
        source.copyInto(combined, destinationOffset = pending.size, endIndex = length)

        val alignedLength = combined.size - (combined.size % frameSize)
        pending = combined.copyOfRange(alignedLength, combined.size)
        return combined.copyOf(alignedLength)
    }
}
