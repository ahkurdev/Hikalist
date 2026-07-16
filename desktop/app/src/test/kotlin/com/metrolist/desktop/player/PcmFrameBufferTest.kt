package com.metrolist.desktop.player

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmFrameBufferTest {
    @Test
    fun `consume preserves partial frames for the next audio chunk`() {
        val buffer = PcmFrameBuffer(frameSize = 4)

        assertArrayEquals(byteArrayOf(), buffer.consume(byteArrayOf(1, 2, 3), 3))
        assertEquals(3, buffer.pendingByteCount)

        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
            buffer.consume(byteArrayOf(4, 5, 6, 7, 8), 5),
        )
        assertEquals(0, buffer.pendingByteCount)
    }

    @Test
    fun `consume ignores unused bytes in the source array`() {
        val buffer = PcmFrameBuffer(frameSize = 4)

        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4),
            buffer.consume(byteArrayOf(1, 2, 3, 4, 99, 100), 4),
        )
        assertEquals(0, buffer.pendingByteCount)
    }
}
