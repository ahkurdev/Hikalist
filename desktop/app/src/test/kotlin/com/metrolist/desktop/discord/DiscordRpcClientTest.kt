package com.metrolist.desktop.discord

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordRpcClientTest {
    @Test
    fun `first publish connects handshakes and sends activity`() {
        val connection = RecordingConnection()
        val client = DiscordRpcClient(
            applicationId = APPLICATION_ID,
            connector = DiscordRpcConnector { connection },
            pid = 42,
            nowMs = { 1_000L },
        )

        assertTrue(client.publish(activity()))
        assertEquals(listOf(0, 1), connection.frames.map(::frameOpcode))
    }

    @Test
    fun `clear only updates an established connection`() {
        val connection = RecordingConnection()
        var connections = 0
        val client = DiscordRpcClient(
            applicationId = APPLICATION_ID,
            connector = DiscordRpcConnector { connections++; connection },
            pid = 42,
        )

        client.clear()
        assertEquals(0, connections)
        client.publish(activity())
        client.clear()

        assertEquals(listOf(0, 1, 1), connection.frames.map(::frameOpcode))
        assertTrue(decodeFramePayload(connection.frames.last()).getJSONObject("args").isNull("activity"))
    }

    @Test
    fun `failed write disconnects and reconnects after retry delay`() {
        val broken = RecordingConnection(failOnWrite = 2)
        val healthy = RecordingConnection()
        val candidates = ArrayDeque<DiscordRpcConnection>().apply {
            add(broken)
            add(healthy)
        }
        var now = 1_000L
        val client = DiscordRpcClient(
            applicationId = APPLICATION_ID,
            connector = DiscordRpcConnector { candidates.removeFirstOrNull() },
            pid = 42,
            nowMs = { now },
            reconnectDelayMs = 5_000L,
        )

        assertFalse(client.publish(activity()))
        assertTrue(broken.closed)
        assertFalse(client.publish(activity()))
        now += 5_000L
        assertTrue(client.publish(activity()))
        assertEquals(listOf(0, 1), healthy.frames.map(::frameOpcode))
    }

    @Test
    fun `missing Discord is harmless and throttled`() {
        var attempts = 0
        var now = 1_000L
        val client = DiscordRpcClient(
            applicationId = APPLICATION_ID,
            connector = DiscordRpcConnector { attempts++; null },
            nowMs = { now },
            reconnectDelayMs = 5_000L,
        )

        assertFalse(client.publish(activity()))
        assertFalse(client.publish(activity()))
        assertEquals(1, attempts)
        now += 5_000L
        assertFalse(client.publish(activity()))
        assertEquals(2, attempts)
    }

    @Test
    fun `second song update is sent on the existing connection`() {
        val connection = SequentialConnection()
        val client = DiscordRpcClient(
            applicationId = APPLICATION_ID,
            connector = DiscordRpcConnector { connection },
            pid = 42,
        )

        assertTrue(client.publish(activity()))
        assertTrue(client.publish(DiscordRpcActivity(details = "golden hour", state = "JVKE")))
        assertEquals(listOf(0, 1, 1), connection.frames.map(::frameOpcode))
    }

    @Test
    fun `close does not wait for a blocked Discord update`() {
        val connection = BlockingWriteConnection()
        val client = DiscordRpcClient(
            applicationId = APPLICATION_ID,
            connector = DiscordRpcConnector { connection },
            pid = 42,
        )
        val executor = Executors.newFixedThreadPool(2)
        val publish = executor.submit<Boolean> { client.publish(activity()) }
        assertTrue(connection.activityWriteStarted.await(1, TimeUnit.SECONDS))
        val closing = executor.submit { client.close() }

        val closedQuickly = try {
            closing.get(300, TimeUnit.MILLISECONDS)
            true
        } catch (_: TimeoutException) {
            false
        } finally {
            connection.releaseWrite.countDown()
        }
        publish.get(1, TimeUnit.SECONDS)
        closing.get(1, TimeUnit.SECONDS)
        executor.shutdownNow()

        assertTrue("Closing Hikalist must not wait on Discord IPC", closedQuickly)
        assertTrue(connection.closed)
    }

    private fun activity() = DiscordRpcActivity(details = "Ada", state = "Lyodra, Afgan")

    private class RecordingConnection(private val failOnWrite: Int? = null) : DiscordRpcConnection {
        val frames = mutableListOf<ByteArray>()
        var closed = false
        private var writes = 0

        override fun write(frame: ByteArray) {
            writes++
            if (writes == failOnWrite) throw IOException("pipe closed")
            frames += frame
        }

        override fun close() {
            closed = true
        }
    }

    private class SequentialConnection : DiscordRpcConnection {
        val frames = mutableListOf<ByteArray>()

        override fun write(frame: ByteArray) {
            frames += frame
        }

        override fun close() = Unit
    }

    private class BlockingWriteConnection : DiscordRpcConnection {
        val activityWriteStarted = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        var closed = false

        override fun write(frame: ByteArray) {
            if (frameOpcode(frame) != 1) return
            activityWriteStarted.countDown()
            releaseWrite.await(2, TimeUnit.SECONDS)
        }

        override fun close() {
            closed = true
            releaseWrite.countDown()
        }
    }

    private companion object {
        const val APPLICATION_ID = "1527193618769510430"
    }
}
