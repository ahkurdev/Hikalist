package com.metrolist.desktop.discord

import java.io.Closeable
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

internal fun interface DiscordRpcConnector {
    fun connect(): DiscordRpcConnection?
}

internal interface DiscordRpcConnection : Closeable {
    fun write(frame: ByteArray)
}

internal class DiscordRpcClient(
    private val applicationId: String,
    private val connector: DiscordRpcConnector = WindowsDiscordRpcConnector(),
    private val pid: Int = ProcessHandle.current().pid().toInt(),
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val reconnectDelayMs: Long = DEFAULT_RECONNECT_DELAY_MS,
) : DiscordPresencePublisher {
    private val logger = LoggerFactory.getLogger(DiscordRpcClient::class.java)
    private val closed = AtomicBoolean(false)
    @Volatile private var connection: DiscordRpcConnection? = null
    private var nextConnectAtMs = 0L
    private var lastLoggedActivity: Pair<String, String>? = null

    override fun publish(activity: DiscordRpcActivity): Boolean {
        if (closed.get()) return false
        val activeConnection = connection ?: connect() ?: return false
        return try {
            activeConnection.write(DiscordRpcProtocol.setActivity(pid, activity))
            val activityKey = activity.details to activity.state
            if (activityKey != lastLoggedActivity) {
                logger.info("Updated Discord Rich Presence: {} - {}", activity.details, activity.state)
                lastLoggedActivity = activityKey
            }
            true
        } catch (error: IOException) {
            disconnect(error)
            false
        }
    }

    override fun clear() {
        if (closed.get()) return
        val activeConnection = connection ?: return
        try {
            activeConnection.write(DiscordRpcProtocol.setActivity(pid, null))
            lastLoggedActivity = null
        } catch (error: IOException) {
            disconnect(error)
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        val activeConnection = connection
        connection = null
        activeConnection?.closeQuietly()
        lastLoggedActivity = null
    }

    private fun connect(): DiscordRpcConnection? {
        if (closed.get()) return null
        val currentTime = nowMs()
        if (currentTime < nextConnectAtMs) return null
        val candidate = try {
            connector.connect()
        } catch (error: IOException) {
            logger.debug("Discord Desktop RPC is unavailable: {}", error.message)
            null
        }
        if (candidate == null) {
            nextConnectAtMs = currentTime + reconnectDelayMs
            return null
        }
        return try {
            candidate.write(DiscordRpcProtocol.handshake(applicationId))
            if (closed.get()) {
                candidate.closeQuietly()
                return null
            }
            connection = candidate
            nextConnectAtMs = 0L
            logger.info("Connected to Discord Desktop Rich Presence")
            candidate
        } catch (error: IOException) {
            candidate.closeQuietly()
            nextConnectAtMs = currentTime + reconnectDelayMs
            logger.debug("Discord RPC handshake failed: {}", error.message)
            null
        }
    }

    private fun disconnect(error: IOException) {
        logger.debug("Discord RPC connection closed: {}", error.message)
        connection?.closeQuietly()
        connection = null
        nextConnectAtMs = nowMs() + reconnectDelayMs
    }

    private fun DiscordRpcConnection.closeQuietly() {
        runCatching { close() }
    }

    private companion object {
        const val DEFAULT_RECONNECT_DELAY_MS = 10_000L
    }
}

private class WindowsDiscordRpcConnector : DiscordRpcConnector {
    override fun connect(): DiscordRpcConnection? {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return null
        for (index in 0 until PIPE_COUNT) {
            try {
                return WindowsDiscordRpcConnection(RandomAccessFile("\\\\.\\pipe\\discord-ipc-$index", "rw"))
            } catch (_: IOException) {
                // Discord may expose any one of its numbered local IPC pipes.
            }
        }
        return null
    }

    private companion object {
        const val PIPE_COUNT = 10
    }
}

private class WindowsDiscordRpcConnection(
    private val pipe: RandomAccessFile,
) : DiscordRpcConnection {
    private val logger = LoggerFactory.getLogger(WindowsDiscordRpcConnection::class.java)
    private val open = AtomicBoolean(true)

    @Synchronized
    override fun write(frame: ByteArray) {
        if (!open.get()) throw IOException("Discord RPC pipe is closed")
        pipe.write(frame)
        awaitCommandResponse()
    }

    override fun close() {
        if (!open.compareAndSet(true, false)) return
        runCatching { pipe.close() }
    }

    private fun awaitCommandResponse() {
        val header = ByteArray(HEADER_SIZE)
        try {
            while (open.get()) {
                readFullyWaiting(header)
                if (!open.get()) break
                val headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                val opcode = headerBuffer.int
                val payloadSize = headerBuffer.int
                if (payloadSize !in 0..MAX_PAYLOAD_SIZE) throw IOException("Invalid Discord RPC response size")
                val payload = ByteArray(payloadSize)
                if (payloadSize > 0) readFullyWaiting(payload)
                when (opcode) {
                    FRAME_OPCODE -> {
                        val response = payload.toString(Charsets.UTF_8)
                        if (response.contains("\"evt\":\"ERROR\"")) {
                            throw IOException("Discord rejected Rich Presence: $response")
                        }
                        return
                    }
                    CLOSE_OPCODE -> {
                        val reason = payload.toString(Charsets.UTF_8)
                        logger.warn("Discord closed the Rich Presence pipe: {}", reason)
                        open.set(false)
                        throw IOException("Discord closed Rich Presence: $reason")
                    }
                    PING_OPCODE -> writeControlFrame(PONG_OPCODE, payload)
                }
            }
        } catch (error: IOException) {
            open.set(false)
            throw error
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while waiting for Discord", interrupted)
        }
        throw IOException("Discord RPC pipe is closed")
    }

    private fun readFullyWaiting(target: ByteArray) {
        var offset = 0
        while (open.get() && offset < target.size) {
            val read = pipe.read(target, offset, target.size - offset)
            if (read < 0) {
                Thread.sleep(EMPTY_PIPE_RETRY_MS)
            } else {
                offset += read
            }
        }
    }

    private fun writeControlFrame(opcode: Int, payload: ByteArray) {
        val frame = ByteBuffer.allocate(HEADER_SIZE + payload.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(opcode)
            .putInt(payload.size)
            .put(payload)
            .array()
        pipe.write(frame)
    }

    private companion object {
        const val HEADER_SIZE = 8
        const val FRAME_OPCODE = 1
        const val CLOSE_OPCODE = 2
        const val PING_OPCODE = 3
        const val PONG_OPCODE = 4
        const val MAX_PAYLOAD_SIZE = 1_048_576
        const val EMPTY_PIPE_RETRY_MS = 25L
    }
}
