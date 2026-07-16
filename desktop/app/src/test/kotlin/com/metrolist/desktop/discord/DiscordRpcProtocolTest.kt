package com.metrolist.desktop.discord

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordRpcProtocolTest {
    @Test
    fun `handshake frame uses little endian header and public application id`() {
        val bytes = DiscordRpcProtocol.handshake("1527193618769510430")
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val opcode = buffer.int
        val length = buffer.int
        val payloadBytes = ByteArray(length).also(buffer::get)
        val payload = JSONObject(payloadBytes.toString(Charsets.UTF_8))

        assertEquals(0, opcode)
        assertEquals(1, payload.getInt("v"))
        assertEquals("1527193618769510430", payload.getString("client_id"))
        assertEquals(bytes.size - 8, length)
    }

    @Test
    fun `set activity frame contains listening metadata timer artwork and link`() {
        val activity = DiscordRpcActivity(
            details = "Ada",
            state = "Lyodra, Afgan",
            startTimestampMs = 1_000,
            endTimestampMs = 241_000,
            largeImage = "https://example.com/cover.jpg",
            largeText = "Ada",
            buttonLabel = "Listen on YouTube Music",
            buttonUrl = "https://music.youtube.com/watch?v=video-id",
        )

        val frame = DiscordRpcProtocol.setActivity(pid = 42, activity = activity, nonce = "fixed")
        val payload = decodePayload(frame)
        val args = payload.getJSONObject("args")
        val encodedActivity = args.getJSONObject("activity")

        assertEquals("SET_ACTIVITY", payload.getString("cmd"))
        assertEquals("fixed", payload.getString("nonce"))
        assertEquals(42, args.getInt("pid"))
        assertEquals(2, encodedActivity.getInt("type"))
        assertEquals("Ada", encodedActivity.getString("details"))
        assertEquals("Lyodra, Afgan", encodedActivity.getString("state"))
        assertEquals(1_000, encodedActivity.getJSONObject("timestamps").getLong("start"))
        assertEquals("https://example.com/cover.jpg", encodedActivity.getJSONObject("assets").getString("large_image"))
        assertEquals("Listen on YouTube Music", encodedActivity.getJSONArray("buttons").getJSONObject(0).getString("label"))
    }

    @Test
    fun `clear frame explicitly sends null activity`() {
        val payload = decodePayload(DiscordRpcProtocol.setActivity(pid = 7, activity = null, nonce = "clear"))

        assertTrue(payload.getJSONObject("args").isNull("activity"))
    }

    private fun decodePayload(frame: ByteArray): JSONObject {
        val buffer = ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1, buffer.int)
        val length = buffer.int
        val payload = ByteArray(length).also(buffer::get)
        return JSONObject(payload.toString(Charsets.UTF_8))
    }
}
