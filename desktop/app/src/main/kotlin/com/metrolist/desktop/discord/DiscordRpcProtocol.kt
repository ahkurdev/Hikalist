package com.metrolist.desktop.discord

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class DiscordRpcActivity(
    val details: String,
    val state: String,
    val startTimestampMs: Long? = null,
    val endTimestampMs: Long? = null,
    val largeImage: String? = null,
    val largeText: String? = null,
    val buttonLabel: String? = null,
    val buttonUrl: String? = null,
)

object DiscordRpcProtocol {
    private const val HANDSHAKE_OPCODE = 0
    private const val FRAME_OPCODE = 1

    fun handshake(applicationId: String): ByteArray = encode(
        HANDSHAKE_OPCODE,
        JSONObject().apply {
            put("v", 1)
            put("client_id", applicationId)
        },
    )

    fun setActivity(
        pid: Int,
        activity: DiscordRpcActivity?,
        nonce: String = UUID.randomUUID().toString(),
    ): ByteArray = encode(
        FRAME_OPCODE,
        JSONObject().apply {
            put("cmd", "SET_ACTIVITY")
            put("nonce", nonce)
            put(
                "args",
                JSONObject().apply {
                    put("pid", pid)
                    put("activity", activity?.toJson() ?: JSONObject.NULL)
                },
            )
        },
    )

    private fun DiscordRpcActivity.toJson() = JSONObject().apply {
        put("type", 2)
        put("details", details.take(MAX_TEXT_LENGTH))
        put("state", state.take(MAX_TEXT_LENGTH))
        put("instance", false)
        if (startTimestampMs != null || endTimestampMs != null) {
            put(
                "timestamps",
                JSONObject().apply {
                    startTimestampMs?.let { put("start", it) }
                    endTimestampMs?.let { put("end", it) }
                },
            )
        }
        if (!largeImage.isNullOrBlank() || !largeText.isNullOrBlank()) {
            put(
                "assets",
                JSONObject().apply {
                    largeImage?.takeIf(String::isNotBlank)?.let { put("large_image", it) }
                    largeText?.takeIf(String::isNotBlank)?.let { put("large_text", it.take(MAX_TEXT_LENGTH)) }
                },
            )
        }
        if (!buttonLabel.isNullOrBlank() && !buttonUrl.isNullOrBlank()) {
            put(
                "buttons",
                JSONArray().put(
                    JSONObject().apply {
                        put("label", buttonLabel.take(MAX_BUTTON_LABEL_LENGTH))
                        put("url", buttonUrl)
                    },
                ),
            )
        }
    }

    private fun encode(opcode: Int, payload: JSONObject): ByteArray {
        val body = payload.toString().toByteArray(Charsets.UTF_8)
        return ByteBuffer.allocate(HEADER_SIZE + body.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(opcode)
            .putInt(body.size)
            .put(body)
            .array()
    }

    private const val HEADER_SIZE = 8
    private const val MAX_TEXT_LENGTH = 128
    private const val MAX_BUTTON_LABEL_LENGTH = 32
}

internal fun frameOpcode(frame: ByteArray): Int =
    ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN).int

internal fun decodeFramePayload(frame: ByteArray): JSONObject {
    require(frame.size >= 8) { "Discord RPC frame is too short" }
    val buffer = ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN)
    buffer.int
    val length = buffer.int
    require(length in 0..buffer.remaining()) { "Invalid Discord RPC payload length" }
    val payload = ByteArray(length).also(buffer::get)
    return JSONObject(payload.toString(Charsets.UTF_8))
}
